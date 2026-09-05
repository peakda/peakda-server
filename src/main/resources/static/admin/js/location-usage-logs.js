import { request } from './api.js';
import { initLayout } from './layout.js';
import {
  escapeHtml,
  formatDateTime,
  showToast,
  renderPagination,
  emptyTableRow,
} from './ui.js';

const COLUMN_COUNT = 4;

const SERVICE_LABELS = {
  BLOOM_MAP: '지도 영역 개화 현황',
  CURATION_DETAIL: '큐레이션 상세 거리 계산',
  SPOT_MATCH: '좌표 기반 스팟 매칭',
  SPOT_PREVIEW: '지도 핀 프리뷰',
};

const CHANNEL_LABELS = {
  ANDROID: 'Android',
  IOS: 'iOS',
  WEB: 'Web',
  UNKNOWN: '미상',
};

const state = {
  page: 0,
  size: 20,
  email: '',
  service: '',
  from: '',
  to: '',
};

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: '위치정보 이용·제공사실 확인자료',
    description: '개인위치정보를 이용한 요청을 대상, 취득경로, 제공서비스, 이용일시로 조회합니다.',
  });
  bindElements();
  bindEvents();
  loadUsageLogs();
});

function bindElements() {
  els.form = document.querySelector('#location-usage-filter-form');
  els.email = document.querySelector('#location-usage-email');
  els.service = document.querySelector('#location-usage-service');
  els.from = document.querySelector('#location-usage-from');
  els.to = document.querySelector('#location-usage-to');
  els.size = document.querySelector('#location-usage-size');
  els.refreshButton = document.querySelector('#location-usage-refresh-button');
  els.tableWrap = document.querySelector('#location-usage-table-wrap');
  els.tbody = document.querySelector('#location-usage-table-body');
  els.pagination = document.querySelector('#location-usage-pagination');
}

function bindEvents() {
  els.form.addEventListener('submit', (event) => {
    event.preventDefault();
    state.page = 0;
    state.email = els.email.value.trim();
    state.service = els.service.value;
    state.from = toInstant(els.from.value);
    state.to = toInstant(els.to.value);
    state.size = clampPageSize(els.size.value);
    els.size.value = String(state.size);
    loadUsageLogs();
  });

  els.refreshButton.addEventListener('click', loadUsageLogs);
}

async function loadUsageLogs() {
  setBusy(true);
  try {
    const page = unwrap(await request('/api/admin/location-usage-logs', {
      query: {
        email: state.email,
        service: state.service,
        from: state.from,
        to: state.to,
        page: state.page,
        size: state.size,
      },
    }));
    renderRows(page.content || []);
    renderPageNav(page, (nextPage) => {
      state.page = nextPage;
      loadUsageLogs();
    });
  } catch (error) {
    els.tbody.innerHTML = safeEmptyRow(COLUMN_COUNT, '확인자료를 불러오지 못했습니다.');
    showToast(error.message || '확인자료를 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

function renderRows(usageLogs) {
  if (!usageLogs.length) {
    els.tbody.innerHTML = safeEmptyRow(COLUMN_COUNT, '조회된 확인자료가 없습니다.');
    return;
  }

  els.tbody.innerHTML = usageLogs.map((usageLog) => `
    <tr>
      <td>
        <strong>${escapeHtml(subjectLabel(usageLog))}</strong>
        <p class="muted">ID ${escapeHtml(usageLog.userId)}</p>
      </td>
      <td>${escapeHtml(CHANNEL_LABELS[usageLog.channel] || usageLog.channel)}</td>
      <td>${escapeHtml(SERVICE_LABELS[usageLog.service] || usageLog.service)}</td>
      <td>${escapeHtml(displayDateTime(usageLog.usedAt))}</td>
    </tr>
  `).join('');
}

/** 이메일이 원칙이지만, 이메일이 없거나 탈퇴로 익명화된 계정은 닉네임·id 로 대체 표기한다. */
function subjectLabel(usageLog) {
  return usageLog.email || usageLog.nickname || `사용자 #${usageLog.userId}`;
}

/** datetime-local 값(로컬 타임존)을 서버가 받는 ISO-8601 순간으로 바꾼다. */
function toInstant(value) {
  if (!value) return '';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '' : parsed.toISOString();
}

function clampPageSize(value) {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) return 20;
  return Math.min(50, Math.max(1, parsed));
}

function unwrap(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
}

function setBusy(isLoading) {
  els.tableWrap.setAttribute('aria-busy', String(isLoading));
}

function safeEmptyRow(colspan, message) {
  try {
    return emptyTableRow(colspan, message);
  } catch {
    return `<tr><td colspan="${colspan}" class="empty">${escapeHtml(message)}</td></tr>`;
  }
}

function renderPageNav(page, onChange) {
  try {
    renderPagination(els.pagination, page, onChange);
  } catch {
    fallbackPagination(els.pagination, page, onChange);
  }
}

function fallbackPagination(container, page, onChange) {
  container.innerHTML = '';
  const totalPages = page.totalPages || 0;
  if (totalPages <= 1) return;
  for (let index = 0; index < totalPages; index += 1) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = index === page.page ? 'button button-primary' : 'button';
    button.textContent = String(index + 1);
    button.disabled = index === page.page;
    button.addEventListener('click', () => onChange(index));
    container.append(button);
  }
}

function displayDateTime(value) {
  return value ? formatDateTime(value) : '-';
}
