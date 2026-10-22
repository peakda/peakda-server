import { request } from './api.js';
import { initLayout } from './layout.js';
import {
  confirmAction,
  emptyTableRow,
  escapeHtml,
  formatDateTime,
  renderPagination,
  showToast,
} from './ui.js';

const PLATFORM = 'IOS';
const COLUMN_COUNT = 5;
const DISPATCH_COLUMN_COUNT = 6;
const POLL_INTERVAL_MS = 10000;

const STATUS_LABELS = {
  DISPATCHING: '보내는 중',
  COMPLETED: '완료',
  FAILED: '중단',
  CANCELED: '멈춤',
};

const state = {
  page: 0,
  size: 20,
  email: '',
  totalElements: 0,
  pollTimer: null,
  // 저장하지 않은 문구가 있으면 테스트·전체 발송이 예전 문구로 나간다. 먼저 저장하게 막는다.
  templateDirty: false,
};

const TEMPLATE_FIELDS = ['subject', 'headline', 'body', 'buttonLabel', 'footer'];

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: 'iOS 출시 알림 신청',
    description: '랜딩 페이지에서 받은 출시 알림 신청을 확인하고, 출시 안내 메일을 보냅니다.',
  });
  bindElements();
  bindEvents();
  refresh();
  loadTemplate();
});

function bindElements() {
  els.form = document.querySelector('#launch-alert-filter-form');
  els.email = document.querySelector('#launch-alert-email');
  els.refreshButton = document.querySelector('#launch-alert-refresh-button');
  els.dispatchForm = document.querySelector('#launch-alert-dispatch-form');
  els.storeUrl = document.querySelector('#launch-alert-store-url');
  els.testEmail = document.querySelector('#launch-alert-test-email');
  els.testButton = document.querySelector('#launch-alert-test-button');
  els.dispatchButton = document.querySelector('#launch-alert-dispatch-button');
  els.dispatchBody = document.querySelector('#launch-alert-dispatch-body');
  els.templateForm = document.querySelector('#launch-alert-template-form');
  els.templateSave = document.querySelector('#launch-alert-template-save');
  els.templateUpdated = document.querySelector('#launch-alert-template-updated');
  els.templatePreview = document.querySelector('#launch-alert-template-preview');
  els.template = {
    subject: document.querySelector('#launch-alert-template-subject'),
    headline: document.querySelector('#launch-alert-template-headline'),
    body: document.querySelector('#launch-alert-template-body'),
    buttonLabel: document.querySelector('#launch-alert-template-button'),
    footer: document.querySelector('#launch-alert-template-footer'),
  };
  els.tableWrap = document.querySelector('#launch-alert-table-wrap');
  els.tbody = document.querySelector('#launch-alert-table-body');
  els.pagination = document.querySelector('#launch-alert-pagination');
}

function bindEvents() {
  els.form.addEventListener('submit', (event) => {
    event.preventDefault();
    state.page = 0;
    state.email = els.email.value.trim();
    loadSubscriptions();
  });
  els.refreshButton.addEventListener('click', refresh);
  els.testButton.addEventListener('click', sendTestMail);
  els.templateForm.addEventListener('submit', (event) => {
    event.preventDefault();
    saveTemplate();
  });
  els.templateForm.addEventListener('input', () => {
    state.templateDirty = true;
  });
  // 스토어 주소를 바꾸면 미리보기 버튼 링크도 바꿔 보여 준다.
  els.storeUrl.addEventListener('change', refreshPreview);
  // 입력란에서 Enter 를 눌러도 전체 발송이 시작되지 않게, 폼 제출은 막고 버튼으로만 보낸다.
  els.dispatchForm.addEventListener('submit', (event) => event.preventDefault());
  els.dispatchButton.addEventListener('click', requestDispatch);
  els.dispatchBody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="cancel"]');
    if (button) cancelDispatch(button.dataset.id);
  });
  els.tbody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="delete"]');
    if (button) deleteSubscription(button.dataset.id, button.dataset.email);
  });
}

function refresh() {
  loadSubscriptions();
  loadDispatches();
}

async function loadSubscriptions() {
  els.tableWrap.setAttribute('aria-busy', 'true');
  try {
    const page = await request('/api/admin/launch-alerts', {
      query: { platform: PLATFORM, email: state.email, page: state.page, size: state.size },
    });
    if (!state.email) state.totalElements = page.totalElements ?? 0;
    renderRows(page.content || []);
    renderPagination(els.pagination, page, (nextPage) => {
      state.page = nextPage;
      loadSubscriptions();
    });
  } catch (error) {
    els.tbody.innerHTML = emptyTableRow(COLUMN_COUNT, '신청 목록을 불러오지 못했습니다.');
    showToast(error.message || '신청 목록을 불러오지 못했습니다.', 'error');
  } finally {
    els.tableWrap.setAttribute('aria-busy', 'false');
  }
}

function renderRows(subscriptions) {
  if (!subscriptions.length) {
    els.tbody.innerHTML = emptyTableRow(COLUMN_COUNT, '신청이 없습니다.');
    return;
  }

  els.tbody.innerHTML = subscriptions.map((subscription) => `
    <tr>
      <td><strong>${escapeHtml(subscription.email)}</strong></td>
      <td>${escapeHtml(subscription.consentVersion)}</td>
      <td>${escapeHtml(formatDateTime(subscription.consentedAt))}</td>
      <td>${escapeHtml(formatDateTime(subscription.createdAt))}</td>
      <td>
        <button
          type="button"
          class="button button-small button-danger"
          data-action="delete"
          data-id="${escapeHtml(subscription.id)}"
          data-email="${escapeHtml(subscription.email)}"
        >삭제</button>
      </td>
    </tr>
  `).join('');
}

async function loadDispatches() {
  try {
    const page = await request('/api/admin/launch-alerts/dispatches', { query: { page: 0, size: 5 } });
    const dispatches = page.content || [];
    renderDispatches(dispatches);
    schedulePoll(dispatches.some((dispatch) => dispatch.status === 'DISPATCHING'));
  } catch (error) {
    els.dispatchBody.innerHTML = emptyTableRow(DISPATCH_COLUMN_COUNT, '발송 이력을 불러오지 못했습니다.');
  }
}

function renderDispatches(dispatches) {
  const inProgress = dispatches.some((dispatch) => dispatch.status === 'DISPATCHING');
  els.dispatchButton.disabled = inProgress;
  if (!dispatches.length) {
    els.dispatchBody.innerHTML = emptyTableRow(DISPATCH_COLUMN_COUNT, '아직 보낸 적이 없습니다.');
    return;
  }

  els.dispatchBody.innerHTML = dispatches.map((dispatch) => `
    <tr>
      <td><strong>${escapeHtml(STATUS_LABELS[dispatch.status] || dispatch.status)}</strong></td>
      <td>${escapeHtml(dispatch.sentCount)}</td>
      <td>${escapeHtml(dispatch.failedCount)}</td>
      <td>${escapeHtml(formatDateTime(dispatch.requestedAt))}</td>
      <td>${dispatch.completedAt ? escapeHtml(formatDateTime(dispatch.completedAt)) : '-'}</td>
      <td>${dispatch.status === 'DISPATCHING'
        ? `<button type="button" class="button button-small button-danger" data-action="cancel" data-id="${escapeHtml(dispatch.id)}">멈춤</button>`
        : ''}</td>
    </tr>
  `).join('');
}

/** 보내는 중이면 진행 수와 남은 신청이 바뀌므로 주기적으로 다시 읽는다. */
function schedulePoll(inProgress) {
  clearTimeout(state.pollTimer);
  state.pollTimer = inProgress ? setTimeout(refresh, POLL_INTERVAL_MS) : null;
}

async function loadTemplate() {
  try {
    renderTemplate(await request('/api/admin/launch-alerts/mail-template', { query: { platform: PLATFORM } }));
  } catch (error) {
    showToast(error.message || '메일 문구를 불러오지 못했습니다.', 'error');
  }
}

function renderTemplate(template) {
  TEMPLATE_FIELDS.forEach((field) => {
    els.template[field].value = template[field] ?? '';
  });
  els.templateUpdated.textContent = template.updatedAt ? `최종 수정 ${formatDateTime(template.updatedAt)}` : '';
  state.templateDirty = false;
  refreshPreview();
}

async function saveTemplate() {
  if (!els.templateForm.reportValidity()) return;
  const body = { platform: PLATFORM };
  TEMPLATE_FIELDS.forEach((field) => {
    body[field] = els.template[field].value;
  });

  els.templateSave.disabled = true;
  try {
    renderTemplate(await request('/api/admin/launch-alerts/mail-template', { method: 'PUT', body }));
    showToast('메일 문구를 저장했습니다.');
  } catch (error) {
    showToast(error.message || '메일 문구를 저장하지 못했습니다.', 'error');
  } finally {
    els.templateSave.disabled = false;
  }
}

function refreshPreview() {
  const query = new URLSearchParams({ platform: PLATFORM });
  const storeUrl = els.storeUrl.value.trim();
  if (storeUrl) query.set('storeUrl', storeUrl);
  els.templatePreview.src = `/api/admin/launch-alerts/mail-template/preview?${query}`;
}

function ensureTemplateSaved() {
  if (!state.templateDirty) return true;
  els.templateSave.focus();
  showToast('저장하지 않은 메일 문구가 있습니다. 먼저 저장해 주세요.', 'error');
  return false;
}

async function sendTestMail() {
  if (!ensureTemplateSaved()) return;
  if (!els.storeUrl.reportValidity()) return;
  const email = els.testEmail.value.trim();
  if (!email || !els.testEmail.checkValidity()) {
    els.testEmail.focus();
    showToast('테스트 받을 이메일을 입력해 주세요.', 'error');
    return;
  }

  els.testButton.disabled = true;
  try {
    await request('/api/admin/launch-alerts/dispatches/test', {
      method: 'POST',
      body: { platform: PLATFORM, storeUrl: els.storeUrl.value.trim(), email },
    });
    showToast('테스트 메일을 보냈습니다. 받은편지함과 스팸함을 확인해 주세요.');
  } catch (error) {
    showToast(error.message || '테스트 메일을 보내지 못했습니다.', 'error');
  } finally {
    els.testButton.disabled = false;
  }
}

async function requestDispatch() {
  if (!ensureTemplateSaved()) return;
  if (!els.storeUrl.reportValidity()) return;
  const confirmed = await confirmAction({
    title: `신청 ${state.totalElements}건에 출시 안내를 보낼까요?`,
    message: '남은 신청 전체에 메일을 보내고, 보낸 신청은 바로 지웁니다. 테스트 메일로 내용과 링크를 먼저 확인하세요.',
    confirmLabel: '전체 발송',
    danger: true,
  });
  if (!confirmed) return;

  els.dispatchButton.disabled = true;
  try {
    await request('/api/admin/launch-alerts/dispatches', {
      method: 'POST',
      body: { platform: PLATFORM, storeUrl: els.storeUrl.value.trim() },
    });
    showToast('발송을 요청했습니다. 30초 안에 시작합니다.');
    loadDispatches();
  } catch (error) {
    els.dispatchButton.disabled = false;
    showToast(error.message || '발송을 요청하지 못했습니다.', 'error');
  }
}

async function cancelDispatch(id) {
  const confirmed = await confirmAction({
    title: '발송을 멈출까요?',
    message: '지금 보내는 한 통까지만 나가고 멈춥니다. 남은 신청은 목록에 그대로 남습니다.',
    confirmLabel: '멈춤',
    danger: true,
  });
  if (!confirmed) return;

  try {
    await request(`/api/admin/launch-alerts/dispatches/${encodeURIComponent(id)}/cancel`, { method: 'POST' });
    showToast('발송을 멈췄습니다.');
    refresh();
  } catch (error) {
    showToast(error.message || '발송을 멈추지 못했습니다.', 'error');
  }
}

async function deleteSubscription(id, email) {
  const confirmed = await confirmAction({
    title: '신청을 삭제할까요?',
    message: `${email} 의 출시 알림 신청을 지웁니다. 신청자가 철회를 요청했을 때 사용합니다.`,
    confirmLabel: '삭제',
    danger: true,
  });
  if (!confirmed) return;

  try {
    await request(`/api/admin/launch-alerts/${encodeURIComponent(id)}`, { method: 'DELETE' });
    showToast('신청을 삭제했습니다.');
    loadSubscriptions();
  } catch (error) {
    showToast(error.message || '신청을 삭제하지 못했습니다.', 'error');
  }
}
