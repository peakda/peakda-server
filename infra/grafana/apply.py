#!/usr/bin/env python3
"""peakda prod 대시보드·알림을 Grafana Cloud 에 반영한다. 정의는 이 파일이 원본이다.

    PEAKDA_ALERT_EMAILS=<받을 메일>[,<메일>] infra/grafana/apply.py            # 반영
    infra/grafana/apply.py --dry-run [--out <디렉터리>]                        # JSON 만 만든다

토큰: ~/.config/peakda/grafana-token (서비스 계정, Admin). 다른 경로는 GRAFANA_TOKEN_FILE.
Grafana UI 에서 고친 내용은 다음 반영 때 덮어쓴다. 알림 규칙·경로는 UI 에서 읽기 전용이다.

무료 한도를 지킨다
- 지표: 여기서 쓰는 지표는 infra/oci/alloy-config.alloy 의 허용 목록에 있어야 수집된다
- 알림 규칙은 10개 안팎으로 둔다. 로그 쿼리 알림은 5분 범위의 집계 1개만 쓴다
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

GRAFANA_URL = os.environ.get("GRAFANA_URL", "https://tealbalcony3113.grafana.net")
TOKEN_FILE = os.environ.get("GRAFANA_TOKEN_FILE", os.path.expanduser("~/.config/peakda/grafana-token"))

FOLDER_UID = "peakda"
FOLDER_TITLE = "peakda"
PROM = {"type": "prometheus", "uid": "grafanacloud-prom"}
LOKI = {"type": "loki", "uid": "grafanacloud-logs"}
USAGE = {"type": "prometheus", "uid": "grafanacloud-usage"}

E = 'env="prod"'
APP = f'{E}, job="peakda-server"'
APP_LOGS = f'{{{E}, container="peakda-app"}}'
CADDY_LOGS = f'{{{E}, container="peakda-caddy"}}'

# 무료 한도 (Grafana Cloud Free)
SERIES_LIMIT = 10_000
LOGS_GB_LIMIT = 50

# 이전 대시보드 중 더 이상 쓰지 않는 것. prod 는 호스트 지표를 수집하지 않는다.
REMOVED_DASHBOARDS = ["peakda-host"]


# ---------------------------------------------------------------------------
# 링크
# ---------------------------------------------------------------------------

def explore_url(expr, datasource=LOKI):
    """현재 대시보드 시간 범위로 Explore 를 연다. ${__from}·${__to} 는 Grafana 가 채운다."""
    pane = {
        "p": {
            "datasource": datasource["uid"],
            "queries": [{"refId": "A", "expr": expr, "editorMode": "code", "datasource": datasource}],
            "range": {"from": "${__from}", "to": "${__to}"},
        }
    }
    return "/explore?schemaVersion=1&orgId=1&panes=" + urllib.parse.quote(
        json.dumps(pane, separators=(",", ":"), ensure_ascii=False), safe="${}_"
    )


def dashboard_url(uid):
    return f"/d/{uid}?${{__url_time_range}}"


def link(title, url):
    return {"title": title, "url": url, "targetBlank": False}


# ---------------------------------------------------------------------------
# 패널
# ---------------------------------------------------------------------------

def prom(expr, legend="", instant=False, ds=PROM):
    return {"datasource": ds, "expr": expr, "legendFormat": legend, "instant": instant, "range": not instant}


def loki(expr, instant=False):
    return {"datasource": LOKI, "expr": expr, "queryType": "instant" if instant else "range", "editorMode": "code"}


def steps(*pairs, base="green"):
    """임계값. pairs 는 (값, 색) 순서."""
    return {"mode": "absolute", "steps": [{"color": base, "value": None}] + [{"color": c, "value": v} for v, c in pairs]}


def stat(title, target, unit="short", thresholds=None, links=(), description="", mappings=None, no_value="-", decimals=None):
    defaults = {
        "unit": unit,
        "thresholds": thresholds or steps(),
        "color": {"mode": "thresholds"},
        "noValue": no_value,
        "links": list(links),
    }
    if mappings:
        defaults["mappings"] = mappings
    if decimals is not None:
        defaults["decimals"] = decimals
    return {
        "type": "stat",
        "title": title,
        "description": description,
        "datasource": target["datasource"],
        "targets": [dict(target, refId="A")],
        "fieldConfig": {"defaults": defaults, "overrides": []},
        "options": {
            "colorMode": "background",
            "graphMode": "none",
            "justifyMode": "center",
            "textMode": "value",
            "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
        },
    }


def timeseries(title, targets, unit="short", description="", links=(), stack=False):
    return {
        "type": "timeseries",
        "title": title,
        "description": description,
        "datasource": targets[0]["datasource"],
        "targets": [dict(t, refId=chr(ord("A") + i)) for i, t in enumerate(targets)],
        "fieldConfig": {
            "defaults": {
                "unit": unit,
                "links": list(links),
                "custom": {"fillOpacity": 10, "lineWidth": 1, "showPoints": "never",
                           "stacking": {"mode": "normal" if stack else "none"}},
            },
            "overrides": [],
        },
        "options": {"legend": {"displayMode": "list", "placement": "bottom"}, "tooltip": {"mode": "multi"}},
    }


def logs(title, expr, description=""):
    return {
        "type": "logs",
        "title": title,
        "description": description,
        "datasource": LOKI,
        "targets": [dict(loki(expr), refId="A")],
        "options": {"showTime": True, "wrapLogMessage": True, "sortOrder": "Descending",
                    "enableLogDetails": True, "dedupStrategy": "none"},
    }


def layout(rows, collapsed_rows=()):
    """rows: [(높이, [(너비, 패널), ...]), ...] 를 24칸 격자에 배치한다.
    collapsed_rows: [(제목, 높이, 패널)] 는 맨 아래 접힌 행으로 둔다. 접힌 행의 패널은 펼칠 때만 쿼리한다."""
    panels, y, pid = [], 0, 1
    for height, cells in rows:
        x = 0
        for width, panel in cells:
            panel = dict(panel, id=pid, gridPos={"x": x, "y": y, "w": width, "h": height})
            panels.append(panel)
            x += width
            pid += 1
        y += height
    for title, height, panel in collapsed_rows:
        inner = dict(panel, id=pid + 1, gridPos={"x": 0, "y": y + 1, "w": 24, "h": height})
        panels.append({"type": "row", "title": title, "collapsed": True, "id": pid,
                       "gridPos": {"x": 0, "y": y, "w": 24, "h": 1}, "panels": [inner]})
        pid += 2
        y += 1
    return panels


def dashboard(uid, title, description, rows, variables=(), refresh="1m", time_from="now-6h", collapsed_rows=()):
    return {
        "uid": uid,
        "title": title,
        "description": description,
        "tags": ["peakda", "prod"],
        "timezone": "Asia/Seoul",
        "refresh": refresh,
        "time": {"from": time_from, "to": "now"},
        "schemaVersion": 39,
        "editable": False,
        "templating": {"list": list(variables)},
        "links": [
            {"title": "운영 개요", "type": "link", "url": "/d/peakda-overview", "keepTime": True},
            {"title": "배치 잡", "type": "link", "url": "/d/peakda-batch", "keepTime": True},
            {"title": "외부 API", "type": "link", "url": "/d/peakda-external", "keepTime": True},
        ],
        "panels": layout(rows, collapsed_rows),
    }


# ---------------------------------------------------------------------------
# 대시보드
# ---------------------------------------------------------------------------

HTTP_RATE = f'sum(rate(http_server_requests_seconds_count{{{APP}, uri!~"/actuator.*"}}[5m]))'
HTTP_5XX = f'sum(rate(http_server_requests_seconds_count{{{APP}, uri!~"/actuator.*", status=~"5.."}}[5m]))'
HEAP = (f'sum(jvm_memory_used_bytes{{{APP}, area="heap"}}) / '
        f'sum(jvm_memory_max_bytes{{{APP}, area="heap"}})')

# 마지막 성공 기준에서 뺀다. 할 일이 있을 때만 30초마다 돌아 "스케줄러가 살아 있다" 의 근거가 못 된다.
POLLING_JOB = 'job_name!="noticeDispatch"'


def increase_or_new(metric, window, by=""):
    """카운터는 처음 증가할 때 생겨 첫 값이 1 이다. increase() 는 그 첫 값을 세지 않으므로
    범위 안에서 새로 생긴 시계열의 값도 더한다. (잡 카운터는 기동 시 0 으로 미리 만든다)"""
    selector = f'{metric}{{{APP}}}'
    group = f" by ({by})" if by else ""
    return f'sum{group} (increase({selector}[{window}]) or ({selector} unless {selector} offset {window}))'


def overview():
    error_logs = f'{{{E}, container="peakda-app", level="ERROR"}}'
    caddy_5xx = f'{CADDY_LOGS} | status=~"5.."'
    request_var = {
        "name": "request_id",
        "label": "요청 ID (X-Request-Id)",
        "type": "textbox",
        "query": "",
        "current": {"text": "", "value": ""},
        "hide": 0,
    }
    up = stat(
        "앱 상태", prom(f'max(up{{{APP}}})', instant=True),
        thresholds=steps((1, "green"), base="red"),
        mappings=[{"type": "value", "options": {"0": {"text": "다운", "color": "red"},
                                                 "1": {"text": "정상", "color": "green"}}}],
        no_value="수집 끊김",
        description="수집기가 30초마다 앱을 긁는다. '수집 끊김' 은 앱이나 서버, 수집기가 멈춘 것이다.",
        links=[link("앱 로그", explore_url(APP_LOGS))],
    )
    error_ratio = stat(
        "5xx 비율 (15분)",
        prom(f'{HTTP_5XX.replace("[5m]", "[15m]")} / clamp_min({HTTP_RATE.replace("[5m]", "[15m]")}, 1e-9)', instant=True),
        unit="percentunit", thresholds=steps((0.01, "yellow"), (0.05, "red")), no_value="0%", decimals=1,
        description="actuator 제외. 요청이 없으면 0%.",
        links=[link("5xx 접근 로그", explore_url(caddy_5xx)), link("앱 에러 로그", explore_url(error_logs))],
    )
    p95 = stat(
        "가장 느린 API p95",
        prom(f'max(http_server_requests_seconds{{{APP}, uri!~"/actuator.*", quantile="0.95"}})', instant=True),
        unit="s", thresholds=steps((1, "yellow"), (3, "red")), decimals=2,
        description="엔드포인트별 p95 중 가장 큰 값(최근 약 2분). 아래 '엔드포인트별 p95' 에서 어느 API 인지 본다.",
    )
    errors = stat(
        "앱 에러 로그 (1시간)",
        {"datasource": LOKI, "expr": f'sum(count_over_time({error_logs}[1h]))', "queryType": "instant"},
        thresholds=steps((1, "yellow"), (20, "red")), no_value="0",
        links=[link("에러 로그", explore_url(error_logs))],
    )
    job_failures = stat(
        "잡 실패 (24시간)", prom(f'sum(increase(scheduler_job_failure_total{{{APP}}}[24h]))', instant=True),
        thresholds=steps((1, "red")), no_value="0", decimals=0,
        links=[link("배치 잡", dashboard_url("peakda-batch"))],
    )
    scheduler = stat(
        "스케줄러 마지막 성공", prom(f'time() - max(scheduler_job_last_success_timestamp{{{APP}, {POLLING_JOB}}})', instant=True),
        unit="s", thresholds=steps((4 * 3600, "yellow"), (6 * 3600, "red")),
        description="noticeDispatch 를 뺀 모든 잡을 통틀어 마지막으로 성공한 뒤 지난 시간. 3시간마다 도는 잡(vilageFcstSync)이 있어 정상이면 4시간을 넘지 않는다.",
        links=[link("배치 잡", dashboard_url("peakda-batch"))],
    )
    quota = stat(
        "쿼터 소진 (24시간)", prom(increase_or_new("external_api_quota_exhausted_total", "24h"), instant=True),
        thresholds=steps((1, "orange")), no_value="0", decimals=0,
        links=[link("외부 API", dashboard_url("peakda-external"))],
    )
    heap = stat("힙 사용률", prom(HEAP, instant=True), unit="percentunit",
                thresholds=steps((0.8, "yellow"), (0.9, "red")), decimals=0)
    db_pending = stat(
        "DB 대기 커넥션", prom(f'max(hikaricp_connections_pending{{{APP}}})', instant=True),
        thresholds=steps((1, "yellow"), (3, "red")), no_value="0",
        description="커넥션을 기다리는 요청 수. 0 이 아니면 풀(8개)이 모자라거나 느린 쿼리가 있다.",
    )
    series = stat(
        "지표 한도 사용", prom(f'max(grafanacloud_instance_active_series) / {SERIES_LIMIT}', instant=True, ds=USAGE),
        unit="percentunit", thresholds=steps((0.7, "yellow"), (0.9, "red")), decimals=0,
        description=f"활성 시계열 / 무료 한도 {SERIES_LIMIT:,}. 늘었다면 alloy-config.alloy 허용 목록을 본다.",
        links=[link("Cardinality management", "/d/cardinality-management")],
    )
    logs_usage = stat(
        "로그 월 한도 사용", prom('max(grafanacloud_org_logs_usage) / max(grafanacloud_org_logs_included_usage)',
                             instant=True, ds=USAGE),
        unit="percentunit", thresholds=steps((0.7, "yellow"), (0.9, "red")), decimals=1,
        description=f"이번 달 수집량 / 무료 {LOGS_GB_LIMIT}GB.",
    )

    trace = logs(
        "요청 추적",
        # 칸이 비어 있으면 request_id="" 가 ID 없는 모든 줄과 맞으므로 빈 값을 따로 막는다.
        f'{{{E}}} | request_id!="" | request_id="$request_id"',
        description="같은 요청 ID 의 Caddy 접근 로그와 앱 로그를 시간순으로 보여 준다. 응답 헤더 X-Request-Id 값을 쓴다.",
    )
    return dashboard(
        "peakda-overview", "peakda / 운영 개요",
        "prod 상태를 한 화면에서 본다. 타일을 누르면 관련 로그나 상세 대시보드로 간다.",
        [
            (4, [(4, up), (4, error_ratio), (4, p95), (4, errors), (4, job_failures), (4, scheduler)]),
            (4, [(4, quota), (4, heap), (4, db_pending), (4, series), (4, logs_usage),
                 (4, stat("앱 가동 시간", prom(f'max(process_uptime_seconds{{{APP}}})', instant=True), unit="s",
                          description="마지막 배포·재시작 이후 시간."))]),
            (8, [(12, timeseries("요청량과 5xx", [prom(HTTP_RATE, "전체 req/s"), prom(HTTP_5XX, "5xx req/s")],
                                 unit="reqps", links=[link("5xx 접근 로그", explore_url(caddy_5xx))])),
                 (12, timeseries("엔드포인트별 p95 (상위 8개)",
                                 [prom(f'topk(8, max by (method, uri) (http_server_requests_seconds{{{APP}, uri!~"/actuator.*", quantile="0.95"}}))',
                                       "{{method}} {{uri}}")], unit="s"))]),
            (7, [(8, timeseries("JVM 힙", [prom(f'sum(jvm_memory_used_bytes{{{APP}, area="heap"}})', "사용"),
                                          prom(f'sum(jvm_memory_max_bytes{{{APP}, area="heap"}})', "최대")], unit="bytes")),
                 (8, timeseries("DB 커넥션 풀", [prom(f'max(hikaricp_connections_active{{{APP}}})', "사용 중"),
                                              prom(f'max(hikaricp_connections_idle{{{APP}}})', "유휴"),
                                              prom(f'max(hikaricp_connections_pending{{{APP}}})', "대기")])),
                 (8, timeseries("로그 레벨별 (분당)", [prom(f'sum by (level) (rate(logback_events_total{{{APP}, level=~"error|warn"}}[5m])) * 60',
                                                         "{{level}}")], links=[link("에러 로그", explore_url(error_logs))]))]),
            (10, [(24, logs("최근 에러 로그", error_logs,
                            description="스택트레이스가 본문에 붙어 있다. 줄을 펼쳐 request_id 를 위 칸에 넣으면 그 요청 전체를 본다."))]),
        ],
        variables=[request_var],
        collapsed_rows=[("요청 추적 — 펼친 뒤 위 '요청 ID' 칸에 X-Request-Id 를 넣는다", 10, trace)],
    )


def batch():
    job_logs = f'{APP_LOGS} |= "[scheduler]"'
    table = {
        "type": "table",
        "title": "잡별 현황 — 이름을 누르면 그 잡의 로그",
        "datasource": PROM,
        "targets": [
            dict(prom(f'time() - max by (job_name) (scheduler_job_last_success_timestamp{{{APP}}})', instant=True), refId="A", format="table"),
            dict(prom(f'sum by (job_name) (increase(scheduler_job_success_total{{{APP}}}[24h]))', instant=True), refId="B", format="table"),
            dict(prom(f'sum by (job_name) (increase(scheduler_job_failure_total{{{APP}}}[24h]))', instant=True), refId="C", format="table"),
            dict(prom(f'sum by (job_name) (increase(scheduler_job_skip_total{{{APP}}}[24h]))', instant=True), refId="D", format="table"),
            dict(prom(f'max by (job_name) (scheduler_job_duration_seconds{{{APP}, quantile="0.95"}})', instant=True), refId="E", format="table"),
        ],
        "transformations": [
            {"id": "merge"},
            {"id": "organize", "options": {
                "excludeByName": {"Time": True},
                "renameByName": {"job_name": "잡", "Value #A": "마지막 성공 후", "Value #B": "성공 24h",
                                 "Value #C": "실패 24h", "Value #D": "스킵 24h", "Value #E": "소요 p95"},
                "indexByName": {"job_name": 0, "Value #A": 1, "Value #C": 2, "Value #B": 3, "Value #D": 4, "Value #E": 5},
            }},
            {"id": "sortBy", "options": {"sort": [{"field": "실패 24h", "desc": True}]}},
        ],
        "fieldConfig": {
            "defaults": {"noValue": "0", "custom": {"align": "auto"}},
            "overrides": [
                {"matcher": {"id": "byName", "options": "잡"},
                 "properties": [{"id": "links", "value": [link("이 잡의 로그", explore_url(
                     f'{APP_LOGS} |= "[scheduler] job=${{__value.raw}} "'))]}]},
                {"matcher": {"id": "byName", "options": "마지막 성공 후"},
                 "properties": [{"id": "unit", "value": "s"},
                                {"id": "custom.cellOptions", "value": {"type": "color-text"}},
                                {"id": "thresholds", "value": steps((26 * 3600, "yellow"), (8 * 86400, "red"))}]},
                {"matcher": {"id": "byName", "options": "실패 24h"},
                 "properties": [{"id": "decimals", "value": 0},
                                {"id": "custom.cellOptions", "value": {"type": "color-background"}},
                                {"id": "thresholds", "value": steps((1, "red"), base="transparent")}]},
                {"matcher": {"id": "byName", "options": "성공 24h"}, "properties": [{"id": "decimals", "value": 0}]},
                {"matcher": {"id": "byName", "options": "스킵 24h"}, "properties": [{"id": "decimals", "value": 0}]},
                {"matcher": {"id": "byName", "options": "소요 p95"}, "properties": [{"id": "unit", "value": "s"}]},
            ],
        },
        "options": {"showHeader": True, "cellHeight": "sm"},
        "description": "마지막 성공 후: 26시간이 넘으면 노랑(일 1회 잡 기준), 8일이 넘으면 빨강(주 1회 잡 기준). "
                       "3시간·12시간 주기 잡은 그보다 짧게 봐야 한다. 잡별 기준은 Spring Batch 이관(PEAK-111) 때 정한다.",
    }
    return dashboard(
        "peakda-batch", "peakda / 배치 잡",
        "스케줄러 잡의 성공·실패·소요시간. 실패 원인은 표의 잡 이름이나 아래 로그에서 본다.",
        [
            (11, [(24, table)]),
            (8, [(12, timeseries("실패·스킵 (1시간 증가분)",
                                 [prom(f'sum by (job_name) (increase(scheduler_job_failure_total{{{APP}}}[1h]))', "실패 {{job_name}}"),
                                  prom(f'sum by (job_name, reason) (increase(scheduler_job_skip_total{{{APP}}}[1h]))', "스킵 {{job_name}} {{reason}}")],
                                 links=[link("실패 로그", explore_url(f'{{{E}, container="peakda-app", level="ERROR"}} |= "[scheduler]"'))])),
                 (12, timeseries("소요시간 p95", [prom(f'max by (job_name) (scheduler_job_duration_seconds{{{APP}, quantile="0.95"}})', "{{job_name}}")],
                                 unit="s"))]),
            (10, [(12, logs("실패한 잡 로그", f'{{{E}, container="peakda-app", level="ERROR"}} |= "[scheduler]"')),
                  (12, logs("스케줄러 로그 (noticeDispatch 제외)",
                            f'{job_logs} != "job=noticeDispatch " |~ "status=(COMPLETED|FAILED|SKIPPED)"'))]),
        ],
        time_from="now-24h",
    )


def external():
    ext_logs = f'{APP_LOGS} |= "[external]"'
    return dashboard(
        "peakda-external", "peakda / 외부 API",
        "공공데이터 API 쿼터 사용량과 차단. 쿼터가 소진되면 그 날 해당 잡은 스킵된다.",
        [
            (4, [(6, stat("쿼터 소비 (24시간)", prom(f'sum(increase(external_api_quota_consumed_total{{{APP}}}[24h]))', instant=True),
                          no_value="0", decimals=0)),
                 (6, stat("쿼터 소진 (24시간)", prom(increase_or_new("external_api_quota_exhausted_total", "24h"), instant=True),
                          thresholds=steps((1, "orange")), no_value="0", decimals=0,
                          links=[link("쿼터 로그", explore_url(f'{ext_logs} |~ "(?i)quota"'))])),
                 (6, stat("레이트리밋 차단 (24시간)", prom(f'sum(increase(external_api_rate_limit_rejected_total{{{APP}}}[24h]))', instant=True),
                          thresholds=steps((1, "yellow")), no_value="0", decimals=0)),
                 (6, stat("외부 API 에러 로그 (24시간)",
                          {"datasource": LOKI, "expr": f'sum(count_over_time({{{E}, container="peakda-app", level=~"ERROR|WARN"}} |= "[external]" [24h]))',
                           "queryType": "instant"},
                          thresholds=steps((1, "yellow"), (50, "red")), no_value="0",
                          links=[link("외부 API 로그", explore_url(ext_logs))]))]),
            (9, [(12, timeseries("서비스별 쿼터 소비 (1시간)",
                                 [prom(f'sum by (provider, service) (increase(external_api_quota_consumed_total{{{APP}}}[1h]))', "{{provider}} {{service}}")],
                                 stack=True)),
                 (12, timeseries("쿼터 소진·레이트리밋 차단 (1시간)",
                                 [prom(increase_or_new("external_api_quota_exhausted_total", "1h", "provider, service"), "소진 {{provider}} {{service}}"),
                                  prom(f'sum by (provider) (increase(external_api_rate_limit_rejected_total{{{APP}}}[1h]))', "차단 {{provider}}")]))]),
            (10, [(24, logs("외부 API 로그", ext_logs))]),
        ],
        time_from="now-24h",
    )


# ---------------------------------------------------------------------------
# 알림
# ---------------------------------------------------------------------------

def rule(uid, title, severity, expr, summary, ds=PROM, threshold=("gt", 0), for_="0s",
         no_data="OK", window=600, link_url=None):
    """expr 가 낸 값이 threshold 를 만족하면 발화한다. 쿼리는 최근 window 초 범위를 본다."""
    query = {"refId": "A", "expr": expr, "instant": True, "range": False}
    if ds is LOKI:
        query = {"refId": "A", "expr": expr, "queryType": "instant"}
    annotations = {"summary": summary}
    if link_url:
        annotations["runbook_url"] = GRAFANA_URL + link_url
    return {
        "uid": uid,
        "title": title,
        "condition": "B",
        "data": [
            {"refId": "A", "datasourceUid": ds["uid"], "relativeTimeRange": {"from": window, "to": 0}, "model": query},
            {"refId": "B", "datasourceUid": "__expr__", "relativeTimeRange": {"from": 0, "to": 0},
             "model": {"refId": "B", "type": "threshold", "expression": "A",
                       "conditions": [{"evaluator": {"type": threshold[0], "params": [threshold[1]]}}]}},
        ],
        "noDataState": no_data,
        "execErrState": "KeepLast",
        "for": for_,
        "labels": {"severity": severity, "service": "peakda", "env": "prod"},
        "annotations": annotations,
        "isPaused": False,
    }


def alert_rules():
    error_logs = f'{{{E}, container="peakda-app", level="ERROR"}}'
    return [
        rule("peakda-app-down", "앱 다운 또는 지표 수집 끊김", "긴급", f'max(up{{{APP}}})',
             "앱이 응답하지 않거나 서버·수집기가 멈췄다. 서버에서 docker compose ps 와 alloy 로그를 본다.",
             threshold=("lt", 1), for_="3m", no_data="Alerting", link_url="/d/peakda-overview"),
        rule("peakda-5xx", "5xx 비율 5% 초과", "경고",
             f'({HTTP_5XX.replace("[5m]", "[10m]")} / clamp_min({HTTP_RATE.replace("[5m]", "[10m]")}, 1e-9)) '
             f'and on() ({HTTP_RATE.replace("[5m]", "[10m]")} > 0.05)',
             "최근 10분 5xx 비율이 5% 를 넘었다(분당 3건 이상 요청이 있을 때만). 운영 개요의 5xx 타일에서 로그로 간다.",
             threshold=("gt", 0.05), for_="5m", link_url="/d/peakda-overview"),
        rule("peakda-error-burst", "앱 에러 로그 급증", "경고", f'sum(count_over_time({error_logs}[5m]))',
             "최근 5분 앱 ERROR 로그가 30건을 넘었다.", ds=LOKI, threshold=("gt", 30), window=300,
             link_url="/d/peakda-overview"),
        rule("peakda-job-failed", "배치 잡 실패", "경고",
             f'sum by (job_name) (increase(scheduler_job_failure_total{{{APP}}}[15m]))',
             "{{ $labels.job_name }} 잡이 실패했다. 배치 잡 대시보드에서 잡 이름을 눌러 로그를 본다.",
             threshold=("gt", 0), window=900, link_url="/d/peakda-batch"),
        rule("peakda-scheduler-stalled", "스케줄러 정지 의심", "경고",
             f'time() - max(scheduler_job_last_success_timestamp{{{APP}, {POLLING_JOB}}})',
             "6시간 동안 성공한 잡이 하나도 없다(noticeDispatch 제외). 3시간 주기 잡이 있으므로 스케줄러가 멈췄거나 모두 실패하고 있다.",
             threshold=("gt", 6 * 3600), for_="10m", link_url="/d/peakda-batch"),
        rule("peakda-quota-exhausted", "외부 API 쿼터 소진", "정보",
             increase_or_new("external_api_quota_exhausted_total", "1h", "provider, service"),
             "{{ $labels.provider }} {{ $labels.service }} 쿼터가 소진됐다. 오늘 해당 잡은 스킵된다.",
             threshold=("gt", 0), window=3600, link_url="/d/peakda-external"),
        rule("peakda-db-pending", "DB 커넥션 대기", "경고", f'max(hikaricp_connections_pending{{{APP}}})',
             "커넥션을 기다리는 요청이 5분째 있다. 느린 쿼리나 커넥션 누수를 의심한다.",
             threshold=("gt", 0), for_="5m", link_url="/d/peakda-overview"),
        rule("peakda-heap", "JVM 힙 90% 초과", "경고", HEAP,
             "힙 사용률이 10분째 90% 를 넘었다. OOM 직전일 수 있다.",
             threshold=("gt", 0.9), for_="10m", link_url="/d/peakda-overview"),
        rule("peakda-usage-series", "Grafana 지표 한도 70% 초과", "정보", 'max(grafanacloud_instance_active_series)',
             f"활성 시계열이 무료 한도({SERIES_LIMIT:,})의 70% 를 넘었다. alloy-config.alloy 허용 목록과 Cardinality management 를 본다.",
             ds=USAGE, threshold=("gt", SERIES_LIMIT * 0.7), for_="30m", link_url="/d/cardinality-management"),
        rule("peakda-usage-logs", "Grafana 로그 월 한도 70% 초과", "정보",
             'max(grafanacloud_org_logs_usage) / max(grafanacloud_org_logs_included_usage)',
             f"이번 달 로그 수집량이 무료 {LOGS_GB_LIMIT}GB 의 70% 를 넘었다. 로그가 폭증한 컨테이너를 찾는다.",
             ds=USAGE, threshold=("gt", 0.7), for_="30m", link_url="/d/peakda-overview"),
    ]


SUBJECT_TEMPLATE = (
    '{{ define "peakda.subject" }}[{{ .CommonLabels.severity }}]'
    '{{ if eq .Status "resolved" }}[해소]{{ end }} peakda {{ .CommonLabels.alertname }}'
    '{{ if gt (len .Alerts.Firing) 1 }} ({{ len .Alerts.Firing }}건){{ end }}{{ end }}'
)


def contact_point(emails):
    return {
        "uid": "peakda-email",
        "name": "peakda-email",
        "type": "email",
        "settings": {"addresses": ";".join(emails), "singleEmail": True,
                     "subject": '{{ template "peakda.subject" . }}'},
        "disableResolveMessage": False,
    }


def policies():
    """긴급은 바로, 나머지는 묶어서 보낸다. 해소 알림도 받는다(배치 잡은 다음 날 정상화되는 일이 많다)."""
    return {
        "receiver": "peakda-email",
        "group_by": ["alertname"],
        "group_wait": "1m",
        "group_interval": "30m",
        "repeat_interval": "12h",
        "routes": [
            {"receiver": "peakda-email", "object_matchers": [["severity", "=", "긴급"]],
             "group_wait": "30s", "group_interval": "5m", "repeat_interval": "1h"},
        ],
    }


# ---------------------------------------------------------------------------
# 반영
# ---------------------------------------------------------------------------

class Grafana:
    def __init__(self, token):
        self.token = token

    def call(self, method, path, body=None, ok_missing=False):
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(GRAFANA_URL + path, data=data, method=method)
        request.add_header("Authorization", f"Bearer {self.token}")
        request.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            if ok_missing and e.code == 404:
                return None
            raise SystemExit(f"{method} {path} → {e.code} {e.read().decode(errors='replace')[:500]}")


def build(emails):
    return {
        "dashboards": [overview(), batch(), external()],
        "rules": alert_rules(),
        "template": SUBJECT_TEMPLATE,
        "contact_point": contact_point(emails),
        "policies": policies(),
    }


def apply(g, spec):
    if g.call("GET", f"/api/folders/{FOLDER_UID}", ok_missing=True) is None:
        g.call("POST", "/api/folders", {"uid": FOLDER_UID, "title": FOLDER_TITLE})

    for d in spec["dashboards"]:
        g.call("POST", "/api/dashboards/db", {"dashboard": d, "folderUid": FOLDER_UID, "overwrite": True,
                                              "message": "infra/grafana/apply.py"})
        print(f"대시보드 {d['uid']}")
    for uid in REMOVED_DASHBOARDS:
        if g.call("DELETE", f"/api/dashboards/uid/{uid}", ok_missing=True) is not None:
            print(f"대시보드 삭제 {uid}")

    g.call("PUT", "/api/v1/provisioning/templates/peakda.subject", {"template": spec["template"]})
    cp = spec["contact_point"]
    existing = {c["uid"] for c in g.call("GET", "/api/v1/provisioning/contact-points")}
    if cp["uid"] in existing:
        g.call("PUT", f"/api/v1/provisioning/contact-points/{cp['uid']}", cp)
    else:
        g.call("POST", "/api/v1/provisioning/contact-points", cp)
    g.call("PUT", "/api/v1/provisioning/policies", spec["policies"])
    print("알림 경로 peakda-email")

    g.call("PUT", f"/api/v1/provisioning/folder/{FOLDER_UID}/rule-groups/prod",
           {"title": "prod", "folderUid": FOLDER_UID, "interval": 60,
            "rules": [dict(r, folderUID=FOLDER_UID, ruleGroup="prod") for r in spec["rules"]]})
    print(f"알림 규칙 {len(spec['rules'])}개")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="반영하지 않고 JSON 만 만든다")
    parser.add_argument("--out", default="build/grafana", help="--dry-run 출력 디렉터리")
    args = parser.parse_args()

    emails = [e.strip() for e in os.environ.get("PEAKDA_ALERT_EMAILS", "").split(",") if e.strip()]
    if args.dry_run:
        spec = build(emails or ["alerts@example.com"])
        os.makedirs(args.out, exist_ok=True)
        for name, value in spec.items():
            with open(os.path.join(args.out, f"{name}.json"), "w") as f:
                json.dump(value, f, ensure_ascii=False, indent=2)
        print(f"{args.out} 에 썼다")
        return
    if not emails:
        sys.exit("PEAKDA_ALERT_EMAILS 에 알림 받을 메일을 넣는다")
    with open(TOKEN_FILE) as f:
        token = f.read().strip()
    apply(Grafana(token), build(emails))


if __name__ == "__main__":
    main()
