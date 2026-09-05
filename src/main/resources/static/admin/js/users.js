import { request } from "./api.js";
import { initLayout } from "./layout.js";
import { confirmAction, escapeHtml, formatDateTime, renderPagination, showToast } from "./ui.js";

const STATUS_LABELS = {
  ACTIVE: "활성",
  SUSPENDED: "정지",
  DEACTIVATED: "탈퇴",
};

const ROLE_LABELS = {
  USER: "사용자",
  ADMIN: "관리자",
};

const state = {
  q: "",
  status: "",
  page: 0,
  size: 20,
  session: null,
};

const form = document.querySelector("#users-filter-form");
const qInput = document.querySelector("#users-q");
const statusSelect = document.querySelector("#users-status");
const sizeSelect = document.querySelector("#users-size");
const tbody = document.querySelector("#users-tbody");
const pagination = document.querySelector("#users-pagination");
const tableRegion = document.querySelector("#users-table-region");

state.session = await initLayout({
  title: "사용자 관리",
  description: "사용자 검색과 제재 상태 변경을 처리합니다.",
});

form.addEventListener("submit", (event) => {
  event.preventDefault();
  state.q = qInput.value.trim();
  state.status = statusSelect.value;
  state.size = Number(sizeSelect.value);
  state.page = 0;
  loadUsers();
});

statusSelect.addEventListener("change", () => {
  state.status = statusSelect.value;
  state.page = 0;
  loadUsers();
});

sizeSelect.addEventListener("change", () => {
  state.size = Number(sizeSelect.value);
  state.page = 0;
  loadUsers();
});

tbody.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-user-id][data-next-status]");
  if (!button || button.disabled) return;
  await changeStatus(button.dataset.userId, button.dataset.nextStatus);
});

loadUsers();

async function loadUsers() {
  setBusy(true);
  try {
    const query = { page: state.page, size: state.size };
    if (state.q) query.q = state.q;
    if (state.status) query.status = state.status;
    const response = await request("/api/admin/users", { query });
    const page = unwrap(response);
    renderRows(page.content || []);
    renderPagination(pagination, page, (nextPage) => {
      state.page = nextPage;
      loadUsers();
    });
  } catch (error) {
    showToast(error.message || "사용자 목록을 불러오지 못했습니다.", "error");
  } finally {
    setBusy(false);
  }
}

function renderRows(items) {
  if (!items.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">검색 결과가 없습니다.</td></tr>`;
    return;
  }
  const currentUserId = String(state.session?.userId || "");
  tbody.innerHTML = items.map((user) => {
    const isSelf = String(user.id) === currentUserId;
    const nextStatus = user.status === "SUSPENDED" ? "ACTIVE" : "SUSPENDED";
    const disabled = isSelf || user.status === "DEACTIVATED";
    const actionLabel = nextStatus === "SUSPENDED" ? "정지" : "해제";
    return `
      <tr>
        <td>
          <strong>${escapeHtml(user.nickname)}</strong>
          <span class="muted">#${escapeHtml(String(user.id))}${isSelf ? " · 본인" : ""}</span>
        </td>
        <td>${escapeHtml(user.email || "-")}</td>
        <td>${escapeHtml(user.provider)}</td>
        <td>${labelBadge(ROLE_LABELS[user.role] || user.role, user.role)}</td>
        <td>${labelBadge(STATUS_LABELS[user.status] || user.status, user.status)}</td>
        <td>${escapeHtml(formatDateTime(user.createdAt))}</td>
        <td>
          <button type="button" class="button ${nextStatus === "SUSPENDED" ? "button-danger" : "button-secondary"}" data-user-id="${escapeHtml(String(user.id))}" data-next-status="${nextStatus}" ${disabled ? "disabled" : ""}>
            ${escapeHtml(actionLabel)}
          </button>
        </td>
      </tr>
    `;
  }).join("");
}

async function changeStatus(userId, nextStatus) {
  const reason = window.prompt(`${STATUS_LABELS[nextStatus]} 사유를 입력하세요. 최대 500자까지 저장됩니다.`);
  if (reason === null) return;
  const memo = reason.trim();
  if (!memo) {
    showToast("상태 변경 사유는 필수입니다.", "warning");
    return;
  }
  if (memo.length > 500) {
    showToast("상태 변경 사유는 500자 이하로 입력하세요.", "warning");
    return;
  }
  const confirmed = await confirmAction({
    title: "사용자 상태 변경",
    message: `사용자 상태를 ${STATUS_LABELS[nextStatus]}로 변경할까요?`,
    confirmLabel: "변경",
    danger: nextStatus === "SUSPENDED",
  });
  if (!confirmed) return;
  try {
    await request(`/api/admin/users/${encodeURIComponent(userId)}/status`, {
      method: "PATCH",
      body: { status: nextStatus, memo },
    });
    showToast("사용자 상태를 변경했습니다.", "success");
    await loadUsers();
  } catch (error) {
    showToast(error.message || "사용자 상태를 변경하지 못했습니다.", "error");
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
