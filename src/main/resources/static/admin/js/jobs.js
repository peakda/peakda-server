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

const RUN_STATUSES = {
  RUNNING: { label: '실행중', tone: 'warning' },
  COMPLETED: { label: '완료', tone: 'success' },
  FAILED: { label: '실패', tone: 'danger' },
  SKIPPED: { label: '건너뜀', tone: 'neutral' },
};

const state = {
  runsPage: 0,
  runsSize: 20,
  jobName: '',
  status: '',
};

const els = {};

document.addEventListener('DOMContentLoaded', () => {
  initLayout({
    title: '스케줄러 잡',
    description: '등록된 스케줄러 잡과 실행 이력을 확인하고 필요 시 수동 실행합니다.',
  });
  bindElements();
  bindEvents();
  loadJobs();
  loadRuns();
});

function bindElements() {
  els.jobsTableWrap = document.querySelector('#jobs-table-wrap');
  els.jobsBody = document.querySelector('#jobs-table-body');
  els.refreshButton = document.querySelector('#jobs-refresh-button');
  els.runsForm = document.querySelector('#job-run-filter-form');
  els.jobNameFilter = document.querySelector('#job-name-filter');
  els.jobNameOptions = document.querySelector('#job-name-options');
  els.statusFilter = document.querySelector('#job-status-filter');
  els.sizeInput = document.querySelector('#job-run-size');
  els.runsTableWrap = document.querySelector('#job-runs-table-wrap');
  els.runsBody = document.querySelector('#job-runs-table-body');
  els.runsPagination = document.querySelector('#job-runs-pagination');
}

function bindEvents() {
  els.refreshButton.addEventListener('click', async () => {
    await loadJobs();
    await loadRuns();
  });

  els.runsForm.addEventListener('submit', (event) => {
    event.preventDefault();
    state.runsPage = 0;
    state.jobName = els.jobNameFilter.value.trim();
    state.status = els.statusFilter.value;
    state.runsSize = clampPageSize(els.sizeInput.value);
    els.sizeInput.value = String(state.runsSize);
    loadRuns();
  });

  els.jobsBody.addEventListener('click', async (event) => {
    const button = event.target.closest('button[data-action="trigger"]');
    if (button) {
      await triggerJob(button.dataset.jobName);
      return;
    }
    const row = event.target.closest('tr[data-run-id]');
    if (!row) return;
    await showErrorDetail(row.dataset.runId);
  });

  els.jobsBody.addEventListener('keydown', async (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    if (event.target.closest('button')) return;
    const row = event.target.closest('tr[data-run-id]');
    if (!row) return;
    event.preventDefault();
    await showErrorDetail(row.dataset.runId);
  });

  els.runsBody.addEventListener('click', async (event) => {
    const row = event.target.closest('tr[data-run-id]');
    if (!row) return;
    await showErrorDetail(row.dataset.runId);
  });

  els.runsBody.addEventListener('keydown', async (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    const row = event.target.closest('tr[data-run-id]');
    if (!row) return;
    event.preventDefault();
    await showErrorDetail(row.dataset.runId);
  });
}

async function loadJobs() {
  setBusy(els.jobsTableWrap, true);
  try {
    const jobs = unwrap(await request('/api/admin/jobs')) || [];
    renderJobRows(jobs);
    renderJobOptions(jobs);
  } catch (error) {
    els.jobsBody.innerHTML = safeEmptyRow(7, '스케줄러 잡 목록을 불러오지 못했습니다.');
    showToast(error.message || '스케줄러 잡 목록을 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(els.jobsTableWrap, false);
  }
}

function renderJobRows(jobs) {
  if (!jobs.length) {
    els.jobsBody.innerHTML = safeEmptyRow(7, '등록된 잡이 없습니다.');
    return;
  }

  els.jobsBody.innerHTML = jobs.map((job) => {
    const latest = job.latestRun;
    const message = latest?.errorMessage || latest?.skipReason || '-';
    const failed = latest?.status === 'FAILED';
    const rowAttrs = failed
      ? ` tabindex="0" role="button" data-run-id="${escapeHtml(latest.id)}" aria-label="${escapeHtml(job.jobName)} 최근 실패 오류 상세 보기"`
      : '';
    return `
      <tr${rowAttrs}>
        <td><strong>${escapeHtml(job.jobName)}</strong></td>
        <td>${latest ? safeBadge(statusLabel(latest.status), statusTone(latest.status)) : '<span class="muted">이력 없음</span>'}</td>
        <td>${escapeHtml(displayDateTime(latest?.startedAt))}</td>
        <td>${escapeHtml(displayDateTime(latest?.finishedAt))}</td>
        <td>${escapeHtml(progress(latest))}</td>
        <td>${escapeHtml(truncate(message, 80))}</td>
        <td>
          <button type="button" class="button button-small" data-action="trigger" data-job-name="${escapeHtml(job.jobName)}">수동 실행</button>
        </td>
      </tr>
    `;
  }).join('');
}

function renderJobOptions(jobs) {
  els.jobNameOptions.innerHTML = jobs
    .map((job) => `<option value="${escapeHtml(job.jobName)}"></option>`)
    .join('');
}

async function loadRuns() {
  setBusy(els.runsTableWrap, true);
  try {
    const page = unwrap(await request('/api/admin/jobs/runs', {
      query: {
        jobName: state.jobName,
        status: state.status,
        page: state.runsPage,
        size: state.runsSize,
      },
    }));
    renderRunRows(page.content || []);
    renderPageNav(page, (nextPage) => {
      state.runsPage = nextPage;
      loadRuns();
    });
  } catch (error) {
    els.runsBody.innerHTML = safeEmptyRow(7, '실행 이력을 불러오지 못했습니다.');
    showToast(error.message || '실행 이력을 불러오지 못했습니다.', 'error');
  } finally {
    setBusy(els.runsTableWrap, false);
  }
}

function renderRunRows(runs) {
  if (!runs.length) {
    els.runsBody.innerHTML = safeEmptyRow(7, '조회된 실행 이력이 없습니다.');
    return;
  }

  els.runsBody.innerHTML = runs.map((run) => {
    const failed = run.status === 'FAILED';
    const rowAttrs = failed
      ? ` tabindex="0" role="button" data-run-id="${escapeHtml(run.id)}" aria-label="${escapeHtml(run.jobName)} 오류 상세 보기"`
      : '';
    return `
      <tr${rowAttrs}>
        <td>${escapeHtml(run.id)}</td>
        <td>${escapeHtml(run.jobName)}</td>
        <td>${safeBadge(statusLabel(run.status), statusTone(run.status))}</td>
        <td>${escapeHtml(displayDateTime(run.startedAt))}</td>
        <td>${escapeHtml(displayDateTime(run.finishedAt))}</td>
        <td>${escapeHtml(progress(run))}</td>
        <td>${escapeHtml(run.skipReason || '-')}</td>
      </tr>
    `;
  }).join('');
}

async function triggerJob(jobName) {
  const ok = await confirmSafely({
    title: '잡 수동 실행',
    message: `${jobName} 잡을 수동 실행하시겠습니까?`,
    confirmLabel: '실행',
  });
  if (!ok) return;

  setBusy(els.jobsTableWrap, true);
  try {
    unwrap(await request(`/api/admin/jobs/${encodeURIComponent(jobName)}/trigger`, { method: 'POST' }));
    showToast('잡 실행 요청을 보냈습니다.', 'success');
    await loadJobs();
    await loadRuns();
  } catch (error) {
    showToast(error.message || '잡 실행 요청에 실패했습니다.', 'error');
  } finally {
    setBusy(els.jobsTableWrap, false);
  }
}

async function showErrorDetail(runId) {
  try {
    const run = unwrap(await request(`/api/admin/jobs/runs/${encodeURIComponent(runId)}`));
    showErrorMessage(run.jobName, run.errorStack || run.errorMessage);
  } catch (error) {
    showToast(error.message || '오류 상세를 불러오지 못했습니다.', 'error');
  }
}

function showErrorMessage(jobName, errorMessage) {
  const modalRoot = document.querySelector('#modal-root');
  const dialog = document.createElement('dialog');
  dialog.className = 'confirm-dialog';
  dialog.innerHTML = `
    <form method="dialog">
      <p class="eyebrow">실행 실패</p>
      <h2 id="job-error-title">실패 메시지</h2>
      <p class="muted">${escapeHtml(jobName)}</p>
      <pre class="code-block">${escapeHtml(errorMessage || '오류 메시지가 없습니다.')}</pre>
      <div class="dialog-actions">
        <button type="submit" class="button button-primary" value="close">닫기</button>
      </div>
    </form>
  `;
  modalRoot.replaceChildren(dialog);
  dialog.addEventListener('close', () => dialog.remove(), { once: true });
  dialog.showModal();
}

function progress(run) {
  if (!run) return '-';
  if (run.processedCount == null && run.totalCount == null) return '-';
  return `${run.processedCount ?? 0} / ${run.totalCount ?? '-'}`;
}

function clampPageSize(value) {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) return 20;
  return Math.min(50, Math.max(1, parsed));
}

function unwrap(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
}

function setBusy(target, isLoading) {
  target.setAttribute('aria-busy', String(isLoading));
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
    renderPagination(els.runsPagination, page, onChange);
  } catch {
    fallbackPagination(els.runsPagination, page, onChange);
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
  return RUN_STATUSES[status]?.label || status || '-';
}

function statusTone(status) {
  return RUN_STATUSES[status]?.tone || 'neutral';
}

function truncate(value, maxLength) {
  const text = value || '';
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function displayDateTime(value) {
  return value ? formatDateTime(value) : '-';
}
