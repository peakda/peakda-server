"""Reconcile ECS deployments with RDS-managed credential changes (no secret reads)."""

import json
import os


def reconcile(secrets, ecs, secret_arn, cluster, service_name):
    metadata = secrets.describe_secret(SecretId=secret_arn)
    # Successful rotation only: LastChangedDate also advances for metadata and
    # AWSPENDING, which could restart tasks before the new password is current.
    rotated_at = metadata.get("LastRotatedDate")
    if rotated_at is None:
        return {"status": "not_yet_rotated"}

    response = ecs.describe_services(cluster=cluster, services=[service_name])
    if response.get("failures") or len(response.get("services", [])) != 1:
        raise RuntimeError("ECS service could not be described")
    service = response["services"][0]
    if service["desiredCount"] == 0:
        return {"status": "scaled_to_zero"}
    primary = next((d for d in service["deployments"] if d["status"] == "PRIMARY"), None)
    if primary is None:
        raise RuntimeError("ECS primary deployment is unavailable")
    if primary.get("rolloutState") == "FAILED":
        raise RuntimeError("ECS primary deployment has failed")
    if primary["createdAt"] > rotated_at:
        return {"status": "current", "deployment": primary["id"]}

    # After circuit-breaker rollback the old COMPLETED deployment can be PRIMARY
    # again. Never undo that rollback by blindly retrying the same rotation.
    if any(d.get("rolloutState") == "FAILED" and d["createdAt"] >= rotated_at
           for d in service["deployments"]):
        raise RuntimeError("Credential refresh deployment failed; manual recovery is required")
    for deployment in service["deployments"]:
        if deployment.get("rolloutState") == "IN_PROGRESS" and deployment["createdAt"] > rotated_at:
            return {"status": "current", "deployment": deployment["id"]}

    # The deployment timestamp is the durable watermark. Duplicate events and
    # scheduled runs reuse it, without a second state store or a pinned image.
    updated = ecs.update_service(cluster=cluster, service=service_name, forceNewDeployment=True)
    deployment = next(d for d in updated["service"]["deployments"] if d["status"] == "PRIMARY")
    return {"status": "redeployed", "deployment": deployment["id"]}


def handler(event, context):
    # boto3 is supplied by the managed Lambda Python runtime. Import here so the
    # deterministic decision logic can be tested locally without AWS packages.
    import boto3

    result = reconcile(
        boto3.client("secretsmanager"),
        boto3.client("ecs"),
        os.environ["SECRET_ARN"],
        os.environ["ECS_CLUSTER"],
        os.environ["ECS_SERVICE"],
    )
    print(json.dumps(result))
    return result
