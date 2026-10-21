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
const COLUMN_COUNT = 6;

const state = {
  page: 0,
  size: 20,
  email: '',
};

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: 'iOS 출시 알림 신청',
    description: '랜딩 페이지에서 받은 출시 알림 신청을 확인하고, 안내 메일을 보낸 뒤 파기합니다.',
  });
  bindElements();
  bindEvents();
  loadSubscriptions();
});

function bindElements() {
  els.form = document.querySelector('#launch-alert-filter-form');
  els.email = document.querySelector('#launch-alert-email');
  els.refreshButton = document.querySelector('#launch-alert-refresh-button');
  els.exportButton = document.querySelector('#launch-alert-export-button');
  els.purgeButton = document.querySelector('#launch-alert-purge-button');
  els.exportStatus = document.querySelector('#launch-alert-export-status');
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
  els.refreshButton.addEventListener('click', loadSubscriptions);
  els.exportButton.addEventListener('click', exportRecipients);
  els.purgeButton.addEventListener('click', purgeExported);
  els.tbody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="delete"]');
    if (button) deleteSubscription(button.dataset.id, button.dataset.email);
  });
}

async function loadSubscriptions() {
  els.tableWrap.setAttribute('aria-busy', 'true');
  try {
    const page = await request('/api/admin/launch-alerts', {
      query: { platform: PLATFORM, email: state.email, page: state.page, size: state.size },
    });
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
      <td>${subscription.exportedAt ? escapeHtml(formatDateTime(subscription.exportedAt)) : '<span class="muted">-</span>'}</td>
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

async function exportRecipients() {
  els.exportButton.disabled = true;
  try {
    const recipients = await request('/api/admin/launch-alerts/recipients', { query: { platform: PLATFORM } });
    if (!recipients.count) {
      els.exportStatus.textContent = '발송할 신청이 없습니다.';
      return;
    }
    downloadCsv(recipients.emails);
    els.exportStatus.textContent = `${recipients.count}건을 내려받았습니다. 메일을 모두 보낸 뒤 "발송 완료 · 파기"를 누르세요.`;
    loadSubscriptions();
  } catch (error) {
    showToast(error.message || '발송 대상을 내려받지 못했습니다.', 'error');
  } finally {
    els.exportButton.disabled = false;
  }
}

async function purgeExported() {
  const confirmed = await confirmAction({
    title: '내려받은 신청을 파기할까요?',
    message: '"내려받음" 시각이 있는 신청을 모두 지웁니다. 그 뒤에 들어온 신청은 남습니다. 메일을 보내기 전이라면 취소하세요.',
    confirmLabel: '파기',
    danger: true,
  });
  if (!confirmed) return;

  els.purgeButton.disabled = true;
  try {
    const result = await request('/api/admin/launch-alerts', {
      method: 'DELETE',
      query: { platform: PLATFORM },
    });
    els.exportStatus.textContent = `${result.deletedCount}건을 파기했습니다.`;
    showToast(`${result.deletedCount}건을 파기했습니다.`);
    state.page = 0;
    loadSubscriptions();
  } catch (error) {
    showToast(error.message || '파기하지 못했습니다.', 'error');
  } finally {
    els.purgeButton.disabled = false;
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

function downloadCsv(emails) {
  // Excel 이 UTF-8 로 열도록 BOM 을 붙인다.
  const csv = `\uFEFFemail\n${emails.map(toCsvCell).join('\n')}\n`;
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `launch-alert-${PLATFORM.toLowerCase()}-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** 서버가 형식을 막지만, 스프레드시트가 수식으로 읽지 않도록 한 번 더 감싼다. */
function toCsvCell(value) {
  const text = /^[=+\-@\t\r]/.test(value) ? `'${value}` : value;
  return `"${text.replaceAll('"', '""')}"`;
}
