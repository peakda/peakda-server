export function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

export function formatDateTime(value) {
    if (!value) {
        return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '—';
    }
    return new Intl.DateTimeFormat('ko-KR', {
        timeZone: 'Asia/Seoul',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    }).format(date);
}

export function formatDate(value) {
    if (!value) {
        return '—';
    }
    const date = new Date(`${value}T00:00:00+09:00`);
    if (Number.isNaN(date.getTime())) {
        return escapeHtml(value);
    }
    return new Intl.DateTimeFormat('ko-KR', {
        timeZone: 'Asia/Seoul',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    }).format(date);
}

export function badge(value) {
    if (!value) {
        return '<span class="badge badge-neutral">—</span>';
    }
    const normalized = String(value).toLowerCase().replaceAll('_', '-');
    return `<span class="badge badge-${escapeHtml(normalized)}">${escapeHtml(value)}</span>`;
}

export function showToast(message, type = 'success') {
    const region = document.getElementById('toast-region');
    if (!region) {
        return;
    }
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
    toast.textContent = message;
    region.append(toast);
    window.setTimeout(() => toast.remove(), 4200);
}

export function emptyTableRow(columns, message = '표시할 항목이 없습니다.') {
    return `<tr><td class="empty-cell" colspan="${columns}">${escapeHtml(message)}</td></tr>`;
}

export function setLoading(container, columns = 1, message = '불러오는 중…') {
    if (!container) {
        return;
    }
    container.innerHTML = `<tr><td class="empty-cell" colspan="${columns}"><span class="loading-dot" aria-hidden="true"></span>${escapeHtml(message)}</td></tr>`;
}

export function renderPagination(container, page, onPageChange) {
    if (!container) {
        return;
    }
    container.replaceChildren();
    if (!page || page.totalPages <= 1) {
        return;
    }

    const previous = document.createElement('button');
    previous.type = 'button';
    previous.className = 'button button-quiet';
    previous.textContent = '이전';
    previous.disabled = page.page <= 0;
    previous.addEventListener('click', () => onPageChange(page.page - 1));

    const summary = document.createElement('span');
    summary.className = 'page-summary';
    summary.textContent = `${page.page + 1} / ${page.totalPages} 페이지 · ${page.totalElements.toLocaleString('ko-KR')}건`;

    const next = document.createElement('button');
    next.type = 'button';
    next.className = 'button button-quiet';
    next.textContent = '다음';
    next.disabled = !page.hasNext;
    next.addEventListener('click', () => onPageChange(page.page + 1));

    container.append(previous, summary, next);
}

export function confirmAction({
    title,
    message,
    confirmLabel = '확인',
    danger = false,
}) {
    const root = document.getElementById('modal-root') ?? document.body;
    const dialog = document.createElement('dialog');
    dialog.className = 'confirm-dialog';

    const form = document.createElement('form');
    form.method = 'dialog';

    const eyebrow = document.createElement('p');
    eyebrow.className = 'eyebrow';
    eyebrow.textContent = danger ? '되돌릴 수 없는 조치' : '조치 확인';

    const heading = document.createElement('h2');
    heading.textContent = title;

    const body = document.createElement('p');
    body.textContent = message;

    const actions = document.createElement('div');
    actions.className = 'dialog-actions';

    const cancel = document.createElement('button');
    cancel.type = 'submit';
    cancel.value = 'cancel';
    cancel.className = 'button button-quiet';
    cancel.textContent = '취소';

    const confirm = document.createElement('button');
    confirm.type = 'submit';
    confirm.value = 'confirm';
    confirm.className = danger ? 'button button-danger' : 'button button-primary';
    confirm.textContent = confirmLabel;

    actions.append(cancel, confirm);
    form.append(eyebrow, heading, body, actions);
    dialog.append(form);
    root.append(dialog);

    return new Promise((resolve) => {
        dialog.addEventListener('close', () => {
            resolve(dialog.returnValue === 'confirm');
            dialog.remove();
        }, { once: true });
        dialog.addEventListener('cancel', () => {
            resolve(false);
            dialog.remove();
        }, { once: true });
        dialog.showModal();
    });
}
