import { request } from './api.js';
import { initLayout } from './layout.js';
import {
  escapeHtml,
  formatDateTime,
  showToast,
  renderPagination,
  emptyTableRow,
} from './ui.js';

const TARGET_TYPES = {
  SPOT_RECORD: '스팟 기록',
  USER: '사용자',
  PLANT: '식물',
  CURATION: '큐레이션',
  FESTIVAL: '축제',
  NOTICE: '공지',
  SCHEDULER_JOB: '스케줄러 잡',
};

const ACTION_LABELS = {
  REPORT_RESOLVE_HIDE: '신고 숨김 처리',
  REPORT_RESOLVE_KEEP: '신고 유지 처리',
  REPORT_DISMISS: '신고 기각',
  SPOT_RECORD_HIDE: '스팟 기록 숨김',
  SPOT_RECORD_RESTORE: '스팟 기록 복구',
  PLANT_UPDATE: '식물 수정',
  PLANT_REJECT: '식물 반려',
  PLANT_RESTORE: '식물 복구',
  CURATION_UPSERT: '큐레이션 저장',
  CURATION_DELETE: '큐레이션 삭제',
  FESTIVAL_EDITORIAL_UPSERT: '축제 에디토리얼 저장',
  FESTIVAL_EDITORIAL_DELETE: '축제 에디토리얼 삭제',
  NOTICE_CREATE: '공지 생성',
  NOTICE_UPDATE: '공지 수정',
  NOTICE_DISPATCH: '공지 발송',
  NOTICE_CANCEL: '공지 취소',
  USER_SUSPEND: '사용자 정지',
  USER_UNSUSPEND: '사용자 정지 해제',
  SCHEDULER_JOB_TRIGGER: '잡 수동 실행',
};

const state = {
  page: 0,
  size: 20,
  targetType: '',
  targetId: '',
  adminId: '',
};

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: '감사 로그',
    description: '운영자 조치 기록을 대상, 관리자, ID 기준으로 조회합니다.',
  });
  bindElements();
  bindEvents();
  loadAuditLogs();
});

function bindElements() {
  els.form = document.querySelector('#audit-filter-form');
  els.targetType = document.querySelector('#audit-target-type');
  els.targetId = document.querySelector('#audit-target-id');
  els.adminId = document.querySelector('#audit-admin-id');
  els.size = document.querySelector('#audit-size');
  els.refreshButton = document.querySelector('#audit-refresh-button');
  els.tableWrap = document.querySelector('#audit-table-wrap');
  els.tbody = document.querySelector('#audit-table-body');
  els.pagination = document.querySelector('#audit-pagination');
}

function bindEvents() {
  els.form.addEventListener('submit', (event) => {
    event.preventDefault();
    state.page = 0;
    state.targetType = els.targetType.value;
    state.targetId = els.targetId.value.trim();
    state.adminId = els.adminId.value.trim();
    state.size = clampPageSize(els.size.value);
    els.size.value = String(state.size);
    loadAuditLogs();
  });

  els.refreshButton.addEventListener('click', loadAuditLogs);
}

async function loadAuditLogs() {
  setBusy(true);
  try {
    const page = unwrap(await request('/api/admin/audit-logs', {
      query: {
        targetType: state.targetType,
        targetId: state.targetId,
        adminId: state.adminId,
        page: state.page,
        size: state.size,
      },
    }));
    renderRows(page.content || []);
    renderPageNav(page, (nextPage) => {
      state.page = nextPage;
      loadAuditLogs();
    });
  } catch (error) {
    els.tbody.innerHTML = safeEmptyRow(6, '감사 로그를 불러오지 못했습니다.');
    showToast(error.message || '감사 로그를 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

function renderRows(logs) {
  if (!logs.length) {
    els.tbody.innerHTML = safeEmptyRow(6, '조회된 감사 로그가 없습니다.');
    return;
  }

  els.tbody.innerHTML = logs.map((log) => `
    <tr>
      <td>${escapeHtml(log.id)}</td>
      <td>
        <strong>${escapeHtml(log.adminNickname || `관리자 #${log.adminId}`)}</strong>
        <p class="muted">ID ${escapeHtml(log.adminId)}</p>
      </td>
      <td>${safeBadge(actionLabel(log.action), actionTone(log.action))}<br><span class="muted">${escapeHtml(log.action)}</span></td>
      <td>${escapeHtml(TARGET_TYPES[log.targetType] || log.targetType)} #${escapeHtml(log.targetId)}</td>
      <td>${escapeHtml(log.memo || '-')}</td>
      <td>${escapeHtml(displayDateTime(log.createdAt))}</td>
    </tr>
  `).join('');
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

function safeBadge(label, tone) {
  return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`;
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

function actionLabel(action) {
  return ACTION_LABELS[action] || action || '-';
}

function actionTone(action) {
  if (action?.includes('DELETE') || action?.includes('REJECT') || action?.includes('HIDE') || action?.includes('SUSPEND')) {
    return 'danger';
  }
  if (action?.includes('DISPATCH') || action?.includes('TRIGGER')) {
    return 'warning';
  }
  return 'neutral';
}

function displayDateTime(value) {
  return value ? formatDateTime(value) : '-';
}
