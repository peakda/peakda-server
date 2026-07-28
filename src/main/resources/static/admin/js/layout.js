import { request } from './api.js';

const NAV_ITEMS = [
    ['dashboard', '/admin/index.html', '대시보드'],
    ['reports', '/admin/reports.html', '신고 심사'],
    ['plants', '/admin/plants.html', '식물 검수'],
    ['curations', '/admin/curations.html', '큐레이션'],
    ['festivals', '/admin/festivals.html', '축제 에디토리얼'],
    ['notices', '/admin/notices.html', '공지 발송'],
    ['users', '/admin/users.html', '사용자'],
    ['jobs', '/admin/jobs.html', '스케줄러 잡'],
    ['audit-logs', '/admin/audit-logs.html', '감사 로그'],
];

function renderSidebar(activePage) {
    const sidebar = document.getElementById('app-sidebar');
    if (!sidebar) {
        return;
    }

    const brand = document.createElement('a');
    brand.className = 'brand';
    brand.href = '/admin/index.html';
    brand.innerHTML = '<span class="brand-mark" aria-hidden="true">P</span><span><strong>PEAKDA</strong><small>운영 콘솔</small></span>';

    const nav = document.createElement('nav');
    nav.className = 'side-nav';
    nav.setAttribute('aria-label', '관리자 메뉴');
    NAV_ITEMS.forEach(([key, href, label]) => {
        const link = document.createElement('a');
        link.href = href;
        link.textContent = label;
        if (key === activePage) {
            link.classList.add('is-active');
            link.setAttribute('aria-current', 'page');
        }
        nav.append(link);
    });

    const footer = document.createElement('p');
    footer.className = 'sidebar-note';
    footer.textContent = '운영 조치는 감사 로그에 기록됩니다.';

    sidebar.replaceChildren(brand, nav, footer);
}

function renderHeader({ title, description, session }) {
    const header = document.getElementById('app-header');
    if (!header) {
        return;
    }

    const heading = document.createElement('div');
    heading.className = 'page-heading';

    const eyebrow = document.createElement('p');
    eyebrow.className = 'eyebrow';
    eyebrow.textContent = 'ADMIN WORKSPACE';

    const titleElement = document.createElement('h1');
    titleElement.textContent = title;

    const descriptionElement = document.createElement('p');
    descriptionElement.textContent = description;

    heading.append(eyebrow, titleElement, descriptionElement);

    const operator = document.createElement('div');
    operator.className = 'operator';
    operator.innerHTML = '<span class="operator-dot" aria-hidden="true"></span>';

    const operatorText = document.createElement('span');
    const operatorName = document.createElement('strong');
    operatorName.textContent = session.nickname;
    const operatorRole = document.createElement('small');
    operatorRole.textContent = `${session.role} · #${session.userId}`;
    operatorText.append(operatorName, operatorRole);
    operator.append(operatorText);

    header.replaceChildren(heading, operator);
}

export async function initLayout({ title, description }) {
    const shell = document.getElementById('app-shell');
    if (shell) {
        shell.classList.add('is-loading');
    }

    const session = await request('/api/admin/session');
    const activePage = document.body.dataset.page || 'dashboard';
    renderSidebar(activePage);
    renderHeader({ title, description, session });

    if (shell) {
        shell.classList.remove('is-loading');
    }
    document.title = `${title} · PEAKDA 운영 콘솔`;
    return session;
}
