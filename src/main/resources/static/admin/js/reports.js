import { request } from "./api.js";
import { initLayout } from "./layout.js";
import { confirmAction, escapeHtml, formatDateTime, renderPagination, showToast } from "./ui.js";

const STATUS_LABELS = {
  PENDING: "대기",
  RESOLVED: "처리 완료",
  DISMISSED: "기각",
};

const TARGET_LABELS = {
  SPOT_RECORD: "스팟 기록",
};

const REASON_LABELS = {
  SPAM: "스팸",
  INAPPROPRIATE: "부적절한 콘텐츠",
  HARASSMENT: "괴롭힘",
  ETC: "기타",
};

const ACTION_LABELS = {
  RESOLVE_HIDE: "숨김 처리",
  RESOLVE_KEEP: "신고만 처리",
  DISMISS: "기각",
};

const state = {
  status: "PENDING",
  page: 0,
  size: 20,
  selected: null,
};

const tbody = document.querySelector("#reports-tbody");
const pagination = document.querySelector("#reports-pagination");
const tabs = document.querySelector("#reports-tabs");
const sizeSelect = document.querySelector("#reports-size");
const detail = document.querySelector("#report-detail");
const tableRegion = document.querySelector("#reports-table-region");

initLayout({
  title: "신고 심사",
  description: "상태별 신고 대상 집계와 상세 신고 내역을 검토합니다.",
});

tabs.addEventListener("click", (event) => {
  const button = event.target.closest("[data-status]");
  if (!button) return;
  state.status = button.dataset.status;
  state.page = 0;
  state.selected = null;
  updateTabs();
  renderDetail(null);
  loadReports();
});

sizeSelect.addEventListener("change", () => {
  state.size = Number(sizeSelect.value);
  state.page = 0;
  loadReports();
});

tbody.addEventListener("click", (event) => {
  const row = event.target.closest("[data-target-type][data-target-id]");
  if (!row) return;
  selectRow(row);
});

tbody.addEventListener("keydown", (event) => {
  if (event.key !== "Enter" && event.key !== " ") return;
  const row = event.target.closest("[data-target-type][data-target-id]");
  if (!row) return;
  event.preventDefault();
  selectRow(row);
});

detail.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.target.closest("[data-review-form]");
  if (!form) return;
  const action = form.elements.action.value;
  const memo = form.elements.memo.value.trim();
  if (!action) {
    showToast("처리 방식을 선택하세요.", "warning");
    return;
  }
  const confirmed = await confirmAction({
    title: "신고 처리 저장",
    message: `${ACTION_LABELS[action]}로 처리할까요?`,
    confirmLabel: "저장",
    danger: action === "RESOLVE_HIDE",
  });
  if (!confirmed) return;
  await reviewSelected(action, memo);
});

loadReports();

function updateTabs() {
  tabs.querySelectorAll("[data-status]").forEach((button) => {
    const active = button.dataset.status === state.status;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-selected", String(active));
  });
}

async function loadReports() {
  setBusy(true);
  try {
    const response = await request("/api/admin/reports", {
      query: { status: state.status, page: state.page, size: state.size },
    });
    const page = unwrap(response);
    renderRows(page.content || []);
    renderPagination(pagination, page, (nextPage) => {
      state.page = nextPage;
      loadReports();
    });
  } catch (error) {
    showToast(error.message || "신고 목록을 불러오지 못했습니다.", "error");
  } finally {
    setBusy(false);
  }
}

function renderRows(items) {
  if (!items.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">표시할 신고 대상이 없습니다.</td></tr>`;
    return;
  }
  tbody.innerHTML = items.map((item) => {
    const selected = state.selected?.targetType === item.targetType && String(state.selected.targetId) === String(item.targetId);
    return `
      <tr class="${selected ? "is-selected" : ""}" tabindex="0" data-target-type="${escapeHtml(item.targetType)}" data-target-id="${escapeHtml(String(item.targetId))}">
        <td>
          <button type="button" class="link-button">#${escapeHtml(String(item.targetId))}</button>
          <span class="muted">${escapeHtml(TARGET_LABELS[item.targetType] || item.targetType)}</span>
        </td>
        <td>${escapeHtml(item.targetSummary || "요약 없음")}</td>
        <td>${escapeHtml(item.targetAuthorNickname || "-")}</td>
        <td>${escapeHtml(String(item.reportCount))}건</td>
        <td>${escapeHtml(formatDateTime(item.lastReportedAt))}</td>
        <td>${labelBadge(item.targetExists ? STATUS_LABELS[state.status] : "대상 없음", item.targetExists ? state.status : "MISSING")}</td>
      </tr>
    `;
  }).join("");
}

function selectRow(row) {
  state.selected = {
    targetType: row.dataset.targetType,
    targetId: row.dataset.targetId,
  };
  loadDetail(state.selected.targetType, state.selected.targetId);
}

async function loadDetail(targetType, targetId) {
  detail.innerHTML = `<p class="muted">상세 정보를 불러오는 중입니다.</p>`;
  try {
    const response = await request(`/api/admin/reports/targets/${encodeURIComponent(targetType)}/${encodeURIComponent(targetId)}`);
    renderDetail(unwrap(response));
  } catch (error) {
    showToast(error.message || "신고 상세를 불러오지 못했습니다.", "error");
    renderDetail(null);
  }
}

function renderDetail(item) {
  if (!item) {
    detail.className = "detail-empty";
    detail.textContent = "목록에서 신고 대상을 선택하세요.";
    return;
  }
  detail.className = "detail-content";
  const hideDisabled = item.targetType !== "SPOT_RECORD";
  const reports = item.reports || [];
  detail.innerHTML = `
    <div class="detail-header">
      <div>
        <strong>${escapeHtml(TARGET_LABELS[item.targetType] || item.targetType)} #${escapeHtml(String(item.targetId))}</strong>
        <p>${escapeHtml(item.targetSummary || "요약 없음")}</p>
      </div>
      ${labelBadge(item.targetExists ? `${item.reportCount}건` : "대상 없음", item.targetExists ? "PENDING" : "MISSING")}
    </div>
    <dl class="meta-list">
      <div><dt>작성자</dt><dd>${escapeHtml(item.targetAuthorNickname || "-")}</dd></div>
      <div><dt>사유 분포</dt><dd>${renderReasonDistribution(item.reasonDistribution || {})}</dd></div>
    </dl>
    <form class="review-form" data-review-form>
      <fieldset>
        <legend>처리 방식</legend>
        <label><input type="radio" name="action" value="RESOLVE_HIDE" ${hideDisabled ? "disabled" : ""}> 숨김 처리</label>
        <label><input type="radio" name="action" value="RESOLVE_KEEP"> 신고만 처리</label>
        <label><input type="radio" name="action" value="DISMISS"> 기각</label>
      </fieldset>
      <label class="field">
        <span>처리 메모</span>
        <textarea name="memo" rows="4" maxlength="500" placeholder="운영 메모를 입력하세요."></textarea>
      </label>
      <button type="submit" class="button button-primary" ${state.status !== "PENDING" ? "disabled" : ""}>처리 저장</button>
      ${hideDisabled ? `<p class="help-text">숨김 처리는 스팟 기록 대상에서만 사용할 수 있습니다.</p>` : ""}
    </form>
    <h3>개별 신고</h3>
    <ul class="review-list">
      ${reports.map(renderReportItem).join("") || `<li class="empty-cell">신고 내역이 없습니다.</li>`}
    </ul>
  `;
}

function renderReasonDistribution(distribution) {
  return Object.entries(distribution)
    .map(([reason, count]) => `${escapeHtml(REASON_LABELS[reason] || reason)} ${escapeHtml(String(count))}건`)
    .join(", ") || "-";
}

function renderReportItem(report) {
  return `
    <li>
      <div class="review-item-header">
        <strong>${escapeHtml(REASON_LABELS[report.reason] || report.reason)}</strong>
        ${labelBadge(STATUS_LABELS[report.status] || report.status, report.status)}
      </div>
      <p>${escapeHtml(report.detail || "상세 사유 없음")}</p>
      <p class="muted">신고자 ${escapeHtml(report.reporterNickname || `#${report.reporterId}`)} · ${escapeHtml(formatDateTime(report.createdAt))}</p>
      ${report.reviewMemo ? `<p class="muted">메모: ${escapeHtml(report.reviewMemo)}</p>` : ""}
    </li>
  `;
}

async function reviewSelected(action, memo) {
  if (!state.selected) return;
  try {
    await request(`/api/admin/reports/targets/${encodeURIComponent(state.selected.targetType)}/${encodeURIComponent(state.selected.targetId)}`, {
      method: "PATCH",
      body: { action, memo: memo || null },
    });
    showToast("신고 처리를 저장했습니다.", "success");
    state.selected = null;
    renderDetail(null);
    await loadReports();
  } catch (error) {
    showToast(error.message || "신고 처리를 저장하지 못했습니다.", "error");
  }
}

function setBusy(isBusy) {
  tableRegion.setAttribute("aria-busy", String(isBusy));
}

function labelBadge(label, value) {
  const normalized = String(value || "neutral").toLowerCase().replaceAll("_", "-");
  return `<span class="badge badge-${escapeHtml(normalized)}">${escapeHtml(label)}</span>`;
}

function unwrap(response) {
  return response && Object.prototype.hasOwnProperty.call(response, "data") ? response.data : response;
}
