import { request } from "./api.js";
import { initLayout } from "./layout.js";
import { confirmAction, escapeHtml, formatDateTime, renderPagination, showToast } from "./ui.js";

const STATUS_LABELS = {
  ACTIVE: "승인",
  PENDING: "대기",
  REJECTED: "거절",
};

const SEASON_LABELS = {
  SPRING: "봄",
  SUMMER: "여름",
  AUTUMN_WINTER: "가을/겨울",
};

const BLOOM_CATEGORIES = [
  "PLUM",
  "FORSYTHIA",
  "AZALEA_KR",
  "CHERRY",
  "CANOLA",
  "AZALEA",
  "HYDRANGEA",
  "LOTUS",
  "COSMOS",
  "PINK_MUHLY",
  "SILVERGRASS",
  "MAPLE",
  "CAMELLIA",
];

const state = {
  status: "ACTIVE",
  page: 0,
  size: 20,
};

const tbody = document.querySelector("#plants-tbody");
const pagination = document.querySelector("#plants-pagination");
const statusSelect = document.querySelector("#plants-status");
const sizeSelect = document.querySelector("#plants-size");
const tableRegion = document.querySelector("#plants-table-region");

initLayout({
  title: "식물 제안 검수",
  description: "사용자 제안 식물을 승인하거나 거절합니다.",
});

statusSelect.addEventListener("change", () => {
  state.status = statusSelect.value;
  state.page = 0;
  loadPlants();
});

sizeSelect.addEventListener("change", () => {
  state.size = Number(sizeSelect.value);
  state.page = 0;
  loadPlants();
});

tbody.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.target.closest("[data-plant-form]");
  if (!form) return;
  await savePlant(form);
});

tbody.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-reject-id]");
  if (!button) return;
  const row = button.closest("[data-plant-id]");
  const form = row?.querySelector("[data-plant-form]");
  if (!form) return;
  const confirmed = await confirmAction({
    title: "식물 제안 거절",
    message: "이 식물 제안을 거절할까요?",
    confirmLabel: "거절",
    danger: true,
  });
  if (!confirmed) return;
  await updatePlant(button.dataset.rejectId, buildPlantBody(form, "REJECTED"), "식물 제안을 거절했습니다.");
});

loadPlants();

async function loadPlants() {
  setBusy(true);
  try {
    const query = { suggestedOnly: true, page: state.page, size: state.size };
    if (state.status) query.status = state.status;
    const response = await request("/api/admin/plants", { query });
    const page = unwrap(response);
    renderRows(page.content || []);
    renderPagination(pagination, page, (nextPage) => {
      state.page = nextPage;
      loadPlants();
    });
  } catch (error) {
    showToast(error.message || "식물 목록을 불러오지 못했습니다.", "error");
  } finally {
    setBusy(false);
  }
}

function renderRows(items) {
  if (!items.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">검수할 식물 제안이 없습니다.</td></tr>`;
    return;
  }
  tbody.innerHTML = items.map((item) => `
    <tr data-plant-id="${escapeHtml(String(item.id))}">
      <td>
        <form id="plant-form-${escapeHtml(String(item.id))}" data-plant-form data-plant-id="${escapeHtml(String(item.id))}">
          <label class="sr-only" for="plant-name-${escapeHtml(String(item.id))}">식물 이름</label>
          <input id="plant-name-${escapeHtml(String(item.id))}" name="name" type="text" maxlength="30" required value="${escapeHtml(item.name)}">
        </form>
        <p class="muted">등록 ${escapeHtml(formatDateTime(item.createdAt))}</p>
      </td>
      <td>
        <label class="sr-only" for="plant-sort-${escapeHtml(String(item.id))}">정렬 순서</label>
        <input id="plant-sort-${escapeHtml(String(item.id))}" form="plant-form-${escapeHtml(String(item.id))}" name="sortOrder" type="number" min="0" step="1" value="${escapeHtml(String(item.sortOrder))}">
      </td>
      <td>${renderSeasonChecks(item)}</td>
      <td>
        <label class="sr-only" for="plant-bloom-${escapeHtml(String(item.id))}">개화 카테고리</label>
        <select id="plant-bloom-${escapeHtml(String(item.id))}" form="plant-form-${escapeHtml(String(item.id))}" name="bloomCategory">
          <option value="">없음</option>
          ${BLOOM_CATEGORIES.map((category) => `<option value="${category}" ${item.bloomCategory === category ? "selected" : ""}>${category}</option>`).join("")}
        </select>
      </td>
      <td>${escapeHtml(item.suggestedByNickname || "-")}</td>
      <td>${labelBadge(STATUS_LABELS[item.status] || item.status, item.status)}</td>
      <td>
        <div class="button-group">
          <button type="submit" form="plant-form-${escapeHtml(String(item.id))}" class="button button-primary">저장</button>
          <button type="button" class="button button-danger" data-reject-id="${escapeHtml(String(item.id))}" ${item.status === "REJECTED" ? "disabled" : ""}>거절</button>
        </div>
      </td>
    </tr>
  `).join("");
}

function renderSeasonChecks(item) {
  const selected = new Set(item.seasons || []);
  return Object.entries(SEASON_LABELS).map(([value, label]) => `
    <label class="check-pill">
      <input form="plant-form-${escapeHtml(String(item.id))}" type="checkbox" name="seasons" value="${value}" ${selected.has(value) ? "checked" : ""}>
      ${escapeHtml(label)}
    </label>
  `).join("");
}

async function savePlant(form) {
  const id = form.dataset.plantId;
  const body = buildPlantBody(form, "ACTIVE");
  if (!body.name) {
    showToast("식물 이름을 입력하세요.", "warning");
    return;
  }
  await updatePlant(id, body, "식물 정보를 저장했습니다.");
}

function buildPlantBody(form, status) {
  const formData = new FormData(form);
  return {
    name: String(formData.get("name") || "").trim(),
    sortOrder: Number(formData.get("sortOrder")),
    status,
    bloomCategory: formData.get("bloomCategory") || null,
    seasons: formData.getAll("seasons"),
  };
}

async function updatePlant(id, body, successMessage) {
  try {
    await request(`/api/admin/plants/${encodeURIComponent(id)}`, {
      method: "PATCH",
      body,
    });
    showToast(successMessage, "success");
    await loadPlants();
  } catch (error) {
    showToast(error.message || "식물 정보를 저장하지 못했습니다.", "error");
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
