import { request } from './api.js';
import { initLayout } from './layout.js';
import { showToast } from './ui.js';

function setCount(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = Number(value ?? 0).toLocaleString('ko-KR');
    }
}

async function loadCounts() {
    const since = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const results = await Promise.allSettled([
        request('/api/admin/reports', { query: { status: 'PENDING', page: 0, size: 1 } }),
        request('/api/admin/plants', { query: { status: 'ACTIVE', suggestedOnly: true, page: 0, size: 1 } }),
        request('/api/admin/notices', { query: { status: 'DRAFT', page: 0, size: 1 } }),
        request('/api/admin/jobs/runs', { query: { status: 'FAILED', since, page: 0, size: 1 } }),
    ]);

    const bindings = [
        'pending-report-count',
        'pending-plant-count',
        'draft-notice-count',
        'failed-job-count',
    ];
    results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
            setCount(bindings[index], result.value.totalElements);
        } else if (!result.reason?.isAuthError) {
            setCount(bindings[index], 0);
        }
    });

    const failed = results.filter((result) => result.status === 'rejected' && !result.reason?.isAuthError);
    if (failed.length > 0) {
        showToast(`현황 ${failed.length}건을 불러오지 못했습니다.`, 'error');
    }
}

async function init() {
    try {
        const session = await initLayout({
            title: '운영 대시보드',
            description: '검토가 필요한 항목과 운영 상태를 한눈에 확인합니다.',
        });
        if (session) {
            await loadCounts();
        }
    } catch (error) {
        if (!error.isAuthError) {
            showToast(error.message, 'error');
        }
    }
}

init();
