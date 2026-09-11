import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import Mock

from db_credential_refresh import reconcile


class CredentialRefreshTest(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 9, 12, tzinfo=timezone.utc)
        self.secrets = Mock()
        self.secrets.describe_secret.return_value = {"LastRotatedDate": self.now}
        self.primary = {
            "id": "old", "status": "PRIMARY", "rolloutState": "COMPLETED",
            "createdAt": self.now - timedelta(days=6),
        }
        self.service = {"desiredCount": 2, "deployments": [self.primary]}
        self.ecs = Mock()
        self.ecs.describe_services.return_value = {"services": [self.service]}
        self.fresh = dict(self.primary, id="new", createdAt=self.now + timedelta(seconds=1), rolloutState="IN_PROGRESS")
        self.ecs.update_service.return_value = {"service": {"deployments": [self.fresh]}}

    def run_reconcile(self):
        return reconcile(self.secrets, self.ecs, "secret", "cluster", "service")

    def test_rotation_redeploys_same_service_without_pinning_task_definition(self):
        self.assertEqual(self.run_reconcile(), {"status": "redeployed", "deployment": "new"})
        self.ecs.update_service.assert_called_once_with(
            cluster="cluster", service="service", forceNewDeployment=True,
        )

    def test_duplicate_event_or_schedule_does_not_restart_fresh_rollout(self):
        self.run_reconcile()
        self.service["deployments"] = [self.fresh]
        self.assertEqual(self.run_reconcile()["status"], "current")
        self.ecs.update_service.assert_called_once()

    def test_rotation_during_deployment_starts_a_new_rollout(self):
        self.primary["rolloutState"] = "IN_PROGRESS"
        self.assertEqual(self.run_reconcile()["status"], "redeployed")

    def test_later_rotation_redeploys_previously_current_service(self):
        self.primary["createdAt"] = self.now + timedelta(seconds=1)
        self.secrets.describe_secret.return_value["LastRotatedDate"] = self.now + timedelta(seconds=2)
        self.assertEqual(self.run_reconcile()["status"], "redeployed")

    def test_metadata_or_pending_secret_change_does_not_restart_current_service(self):
        self.primary["createdAt"] = self.now + timedelta(seconds=1)
        self.secrets.describe_secret.return_value["LastChangedDate"] = self.now + timedelta(hours=1)
        self.assertEqual(self.run_reconcile()["status"], "current")
        self.ecs.update_service.assert_not_called()

    def test_equal_timestamp_is_not_proof_of_fresh_credentials(self):
        self.primary["createdAt"] = self.now
        self.assertEqual(self.run_reconcile()["status"], "redeployed")

    def test_rollback_to_old_primary_does_not_restart_failed_deployment(self):
        failed = dict(self.fresh, status="ACTIVE", rolloutState="FAILED")
        self.service["deployments"].append(failed)
        with self.assertRaisesRegex(RuntimeError, "manual recovery"):
            self.run_reconcile()
        self.ecs.update_service.assert_not_called()

    def test_visible_new_rollout_prevents_duplicate_even_with_old_primary(self):
        self.service["deployments"].append(dict(self.fresh, status="ACTIVE"))
        self.assertEqual(self.run_reconcile(), {"status": "current", "deployment": "new"})
        self.ecs.update_service.assert_not_called()

    def test_manual_recovery_newer_than_rotation_supersedes_failed_deployment(self):
        self.service["deployments"] = [self.fresh, dict(self.fresh, status="ACTIVE", rolloutState="FAILED")]
        self.assertEqual(self.run_reconcile()["status"], "current")
        self.ecs.update_service.assert_not_called()

    def test_update_failure_propagates_for_retry_without_advancing_watermark(self):
        self.ecs.update_service.side_effect = RuntimeError("AWS unavailable")
        with self.assertRaises(RuntimeError):
            self.run_reconcile()
        self.ecs.update_service.side_effect = None
        self.assertEqual(self.run_reconcile()["status"], "redeployed")

    def test_missing_service_and_failed_rollout_raise_for_monitoring(self):
        for response in [{"services": [], "failures": [{"reason": "MISSING"}]},
                         {"services": [dict(self.service, deployments=[])]},
                         {"services": [dict(self.service, deployments=[dict(self.primary, rolloutState="FAILED")])]}]:
            with self.subTest(response=response):
                self.ecs.describe_services.return_value = response
                with self.assertRaises(RuntimeError):
                    self.run_reconcile()
        self.ecs.update_service.assert_not_called()

    def test_scaled_to_zero_service_is_not_started(self):
        self.service["desiredCount"] = 0
        self.assertEqual(self.run_reconcile()["status"], "scaled_to_zero")
        self.ecs.update_service.assert_not_called()

    def test_new_secret_without_successful_rotation_does_not_restart_service(self):
        self.secrets.describe_secret.return_value = {}
        self.assertEqual(self.run_reconcile()["status"], "not_yet_rotated")
        self.ecs.update_service.assert_not_called()


if __name__ == "__main__":
    unittest.main()
