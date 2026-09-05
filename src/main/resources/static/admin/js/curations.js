import { request } from './api.js';
import { initLayout } from './layout.js';
import { escapeHtml, showToast, confirmAction, renderPagination, emptyTableRow } from './ui.js';

const state = {
  page: 0,
  size: 20,
  status: '',
};

const rows = document.querySelector('#curation-rows');
const pagination = document.querySelector('#pagination');
const pageSize = document.querySelector('#page-size');
const refreshButton = document.querySelector('#refresh-button');
const statusTabs = document.querySelector('#status-tabs');

await initLayout({
  title: '큐레이션 관리',
  description: '주차 큐레이션 전체 상태 목록',
});

statusTabs.addEventListener('click', (event) => {
  const button = event.target.closest('[data-status]');
  if (!button) return;
  state.status = button.dataset.status || '';
  state.page = 0;
  statusTabs.querySelectorAll('[data-status]').forEach((tab) => {
    tab.classList.toggle('is-active', tab === button);
  });
  loadCurations();
});

pageSize.addEventListener('change', () => {
  state.page = 0;
  state.size = Number(pageSize.value);
  loadCurations();
});

refreshButton.addEventListener('click', () => loadCurations());

rows.addEventListener('click', async (event) => {
  const deleteButton = event.target.closest('[data-action="delete"]');
  if (!deleteButton) return;

  const id = deleteButton.dataset.id;
  const title = deleteButton.dataset.title || '큐레이션';
  const confirmed = await confirmAction({
    title: '큐레이션 삭제',
    message: `'${title}' 큐레이션을 삭제할까요?`,
    confirmLabel: '삭제',
    danger: true,
  });
  if (!confirmed) return;

  try {
    safeSetLoading(deleteButton, true);
    await request(`/api/admin/curations/${encodeURIComponent(id)}`, { method: 'DELETE' });
    toast('큐레이션을 삭제했습니다.');
    await loadCurations();
  } catch (error) {
    toast(error.message || '큐레이션 삭제에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(deleteButton, false);
  }
});

loadCurations();

async function loadCurations() {
  try {
    safeSetLoading(refreshButton, true);
    rows.innerHTML = tableLoadingRow(7, '큐레이션을 불러오는 중입니다.');
    const response = await request('/api/admin/curations', {
      query: {
        page: state.page,
        size: state.size,
        status: state.status || undefined,
      },
    });
    const pageData = unwrap(response);
    renderRows(pageData.content || []);
    renderPageControls(pageData);
  } catch (error) {
    rows.innerHTML = tableEmptyRow(7, error.message || '큐레이션 목록을 불러오지 못했습니다.');
    pagination.replaceChildren();
    toast(error.message || '큐레이션 목록을 불러오지 못했습니다.', 'error');
  } finally {
    safeSetLoading(refreshButton, false);
  }
}

function renderRows(items) {
  if (!items.length) {
    rows.innerHTML = tableEmptyRow(7, '큐레이션이 없습니다.');
    return;
  }

  rows.innerHTML = items.map((item) => {
    const title = item.title || '-';

    return `
      <tr>
        <td>${statusBadge(item.status)}</td>
        <td>${escapeHtml(item.weekLabel || '-')}</td>
        <td>
          <strong>${escapeHtml(title)}</strong>
        </td>
        <td>${escapeHtml(formatDateRange(item.weekStartDate, item.weekEndDate))}</td>
        <td>${escapeHtml(formatDateTime(item.publishedAt))}</td>
        <td>${escapeHtml(formatCounts(item))}</td>
        <td>
          <div class="inline-actions">
            <a class="button button-small button-secondary" href="/admin/curation-edit.html?id=${encodeURIComponent(item.id)}">편집</a>
            <button
              class="button button-small button-danger"
              type="button"
              data-action="delete"
              data-id="${escapeHtml(String(item.id))}"
              data-title="${escapeHtml(title)}"
            >삭제</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderPageControls(pageData) {
  if (!pageData || Number(pageData.totalPages || 0) <= 1) {
    pagination.replaceChildren();
    return;
  }

  try {
    renderPagination(pagination, pageData, (page) => {
      state.page = page;
      loadCurations();
    });
  } catch (_error) {
    pagination.innerHTML = `
      <button class="button button-small button-secondary" type="button" data-page="${Math.max(0, state.page - 1)}" ${state.page <= 0 ? 'disabled' : ''}>이전</button>
      <span class="page-summary">${state.page + 1} / ${pageData.totalPages}</span>
      <button class="button button-small button-secondary" type="button" data-page="${state.page + 1}" ${pageData.hasNext ? '' : 'disabled'}>다음</button>
    `;
    pagination.querySelectorAll('[data-page]').forEach((button) => {
      button.addEventListener('click', () => {
        state.page = Number(button.dataset.page);
        loadCurations();
      });
    });
  }
}

function statusBadge(status) {
  const normalized = String(status || 'DRAFT').toLowerCase();
  const label = status === 'PUBLISHED' ? '발행' : '임시저장';
  return `<span class="badge badge-${escapeHtml(normalized)}">${escapeHtml(label)}</span>`;
}

function formatCounts(item) {
  return `챕터 ${Number(item.chapterCount || 0)} · 추천 ${Number(item.recommendationCount || 0)}`;
}

function formatDateRange(start, end) {
  if (!start && !end) return '-';
  if (start && end) return `${start} ~ ${end}`;
  return start || end;
}

function formatDateTime(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function unwrap(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
}

function tableEmptyRow(colspan, message) {
  try {
    return emptyTableRow(colspan, message);
  } catch (_error) {
    return `<tr><td class="empty-cell" colspan="${colspan}">${escapeHtml(message)}</td></tr>`;
  }
}

function tableLoadingRow(colspan, message) {
  return `<tr><td class="empty-cell" colspan="${colspan}">${escapeHtml(message)}</td></tr>`;
}

function safeSetLoading(element, loading) {
  element.disabled = loading;
  element.setAttribute('aria-busy', String(loading));
}

function toast(message, type = 'success') {
  try {
    showToast(message, type);
  } catch (_error) {
    window.alert(message);
  }
}
