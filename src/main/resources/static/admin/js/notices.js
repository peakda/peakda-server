import { request } from './api.js';
import { initLayout } from './layout.js';
import {
  escapeHtml,
  formatDateTime,
  showToast,
  confirmAction,
  renderPagination,
  emptyTableRow,
} from './ui.js';

const NOTICE_STATUSES = {
  DRAFT: { label: '작성중', tone: 'neutral' },
  DISPATCHING: { label: '발송중', tone: 'warning' },
  DISPATCHED: { label: '발송완료', tone: 'success' },
  CANCELED: { label: '취소됨', tone: 'danger' },
};

const LINK_TYPES = {
  EXTERNAL: '외부 링크',
  INTERNAL: '앱 내부 대상',
};

const state = {
  page: 0,
  size: 20,
  status: '',
};

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: '공지 관리',
    description: '운영 공지를 작성하고 발송 상태를 관리합니다.',
  });
  bindElements();
  bindEvents();
  resetForm();
  loadNotices();
});

function bindElements() {
  els.filterForm = document.querySelector('#notice-filter-form');
  els.statusFilter = document.querySelector('#notice-status-filter');
  els.sizeInput = document.querySelector('#notice-size');
  els.tableWrap = document.querySelector('#notice-table-wrap');
  els.tbody = document.querySelector('#notice-table-body');
  els.pagination = document.querySelector('#notice-pagination');
  els.newButton = document.querySelector('#notice-new-button');
  els.form = document.querySelector('#notice-form');
  els.formTitle = document.querySelector('#notice-form-title');
  els.formMode = document.querySelector('#notice-form-mode');
  els.id = document.querySelector('#notice-id');
  els.title = document.querySelector('#notice-title');
  els.body = document.querySelector('#notice-body');
  els.linkType = document.querySelector('#notice-link-type');
  els.linkUrl = document.querySelector('#notice-link-url');
  els.targetId = document.querySelector('#notice-target-id');
  els.saveButton = document.querySelector('#notice-save-button');
  els.resetButton = document.querySelector('#notice-reset-button');
}

function bindEvents() {
  els.filterForm.addEventListener('submit', (event) => {
    event.preventDefault();
    state.page = 0;
    state.status = els.statusFilter.value;
    state.size = clampPageSize(els.sizeInput.value);
    els.sizeInput.value = String(state.size);
    loadNotices();
  });

  els.newButton.addEventListener('click', resetForm);
  els.resetButton.addEventListener('click', resetForm);
  els.form.addEventListener('submit', saveNotice);

  els.tbody.addEventListener('click', async (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;

    const id = button.dataset.id;
    const action = button.dataset.action;
    if (action === 'edit') {
      await loadNoticeForEdit(id);
    }
    if (action === 'dispatch') {
      await dispatchNotice(id);
    }
    if (action === 'cancel') {
      await cancelNotice(id);
    }
  });
}

async function loadNotices() {
  setBusy(true);
  try {
    const page = unwrap(await request('/api/admin/notices', {
      query: {
        status: state.status,
        page: state.page,
        size: state.size,
      },
    }));
    renderNoticeRows(page.content || []);
    renderPageNav(page, (nextPage) => {
      state.page = nextPage;
      loadNotices();
    });
  } catch (error) {
    els.tbody.innerHTML = safeEmptyRow(8, '공지 목록을 불러오지 못했습니다.');
    showToast(error.message || '공지 목록을 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

function renderNoticeRows(notices) {
  if (!notices.length) {
    els.tbody.innerHTML = safeEmptyRow(8, '조회된 공지가 없습니다.');
    return;
  }

  els.tbody.innerHTML = notices.map((notice) => {
    const editable = notice.status === 'DRAFT';
    const linkSummary = notice.linkType === 'EXTERNAL'
      ? notice.linkUrl || '-'
      : `대상 #${notice.targetId || '-'}`;
    const actions = [
      editable ? actionButton('edit', notice.id, '수정') : '',
      editable ? actionButton('dispatch', notice.id, '발송') : '',
      editable ? actionButton('cancel', notice.id, '취소') : '',
    ].filter(Boolean).join('');

    return `
      <tr>
        <td>${escapeHtml(notice.id)}</td>
        <td>
          <strong>${escapeHtml(notice.title)}</strong>
          <p class="muted">${escapeHtml(truncate(notice.body, 80))}</p>
        </td>
        <td>${safeBadge(statusLabel(notice.status), statusTone(notice.status))}</td>
        <td>${escapeHtml(notice.sentCount ?? 0)}명</td>
        <td>${escapeHtml(LINK_TYPES[notice.linkType] || notice.linkType)}<br><span class="muted">${escapeHtml(linkSummary)}</span></td>
        <td>${escapeHtml(notice.createdBy)}</td>
        <td>${escapeHtml(displayDateTime(notice.updatedAt))}</td>
        <td class="table-actions">${actions || '<span class="muted">-</span>'}</td>
      </tr>
    `;
  }).join('');
}

async function loadNoticeForEdit(id) {
  setBusy(true);
  try {
    const notice = unwrap(await request(`/api/admin/notices/${encodeURIComponent(id)}`));
    fillForm(notice);
    document.querySelector('#notice-form-title').scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (error) {
    showToast(error.message || '공지 상세를 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

async function saveNotice(event) {
  event.preventDefault();
  const payload = readPayload();
  const id = els.id.value;
  const method = id ? 'PUT' : 'POST';
  const path = id ? `/api/admin/notices/${encodeURIComponent(id)}` : '/api/admin/notices';

  setFormDisabled(true);
  try {
    unwrap(await request(path, { method, body: payload }));
    showToast(id ? '공지를 수정했습니다.' : '공지를 생성했습니다.', 'success');
    resetForm();
    await loadNotices();
  } catch (error) {
    showToast(error.message || '공지 저장에 실패했습니다.', 'error');
  } finally {
    setFormDisabled(false);
  }
}

async function dispatchNotice(id) {
  const ok = await confirmSafely({
    title: '공지 발송',
    message: '공지 발송을 시작하면 되돌릴 수 없습니다. 발송하시겠습니까?',
    confirmLabel: '발송',
    danger: true,
  });
  if (!ok) return;

  setBusy(true);
  try {
    unwrap(await request(`/api/admin/notices/${encodeURIComponent(id)}/dispatch`, { method: 'POST' }));
    showToast('공지 발송을 시작했습니다.', 'success');
    await loadNotices();
  } catch (error) {
    showToast(error.message || '공지 발송에 실패했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

async function cancelNotice(id) {
  const ok = await confirmSafely({
    title: '공지 취소',
    message: '작성중인 공지를 취소하시겠습니까?',
    confirmLabel: '공지 취소',
    danger: true,
  });
  if (!ok) return;

  setBusy(true);
  try {
    unwrap(await request(`/api/admin/notices/${encodeURIComponent(id)}/cancel`, { method: 'POST' }));
    showToast('공지를 취소했습니다.', 'success');
    await loadNotices();
  } catch (error) {
    showToast(error.message || '공지 취소에 실패했습니다.', 'error');
  } finally {
    setBusy(false);
  }
}

function readPayload() {
  const linkUrl = els.linkUrl.value.trim();
  const targetId = els.targetId.value.trim();
  return {
    title: els.title.value.trim(),
    body: els.body.value.trim(),
    linkType: els.linkType.value,
    linkUrl: linkUrl || null,
    targetId: targetId ? Number(targetId) : null,
  };
}

function fillForm(notice) {
  els.id.value = notice.id;
  els.title.value = notice.title || '';
  els.body.value = notice.body || '';
  els.linkType.value = notice.linkType || 'EXTERNAL';
  els.linkUrl.value = notice.linkUrl || '';
  els.targetId.value = notice.targetId || '';
  els.formTitle.textContent = `공지 수정 #${notice.id}`;
  els.formMode.textContent = `현재 상태: ${statusLabel(notice.status)} · 발송 수 ${notice.sentCount ?? 0}명`;
  els.saveButton.textContent = '수정 저장';
}

function resetForm() {
  els.form.reset();
  els.id.value = '';
  els.linkType.value = 'EXTERNAL';
  els.formTitle.textContent = '공지 작성';
  els.formMode.textContent = 'DRAFT 상태의 공지만 수정할 수 있습니다.';
  els.saveButton.textContent = '저장';
}

function actionButton(action, id, label) {
  return `<button type="button" class="button button-small" data-action="${action}" data-id="${escapeHtml(id)}">${label}</button>`;
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

function setFormDisabled(disabled) {
  els.form.querySelectorAll('input, textarea, select, button').forEach((control) => {
    control.disabled = disabled;
  });
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

async function confirmSafely(options) {
  try {
    return await confirmAction(options);
  } catch {
    return window.confirm(options.message);
  }
}

function statusLabel(status) {
  return NOTICE_STATUSES[status]?.label || status || '-';
}

function statusTone(status) {
  return NOTICE_STATUSES[status]?.tone || 'neutral';
}

function truncate(value, maxLength) {
  const text = value || '';
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function displayDateTime(value) {
  return value ? formatDateTime(value) : '-';
}
