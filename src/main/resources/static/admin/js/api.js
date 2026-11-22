const LOGIN_PROVIDERS = [
    ['kakao', '카카오로 로그인'],
    ['naver', '네이버로 로그인'],
    ['google', '구글로 로그인'],
];
const LOGOUT_PATH = '/api/auth/logout';

// 서버는 백오피스 로그인을 마치면 항상 이 화면으로 돌려보낸다. 원래 보던 화면은 여기서 복원한다.
const LANDING_PATH = '/admin/index.html';
const RETURN_PATH_KEY = 'peakda-admin-login-return';
// 서버가 인가 요청을 보관하는 시간과 같다. 이보다 오래된 기록은 끝나지 않은 로그인으로 본다.
const RETURN_PATH_TTL_MS = 10 * 60 * 1000;

const LOGIN_ERROR_MESSAGES = {
    LOGIN_FAILED: '로그인을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    USER_DISABLED: '이용이 제한되었거나 탈퇴한 계정입니다. 다른 계정으로 로그인해 주세요.',
    SIGNUP_REQUIRED: 'PEAKDA에 가입되지 않은 계정입니다. 관리자 권한이 있는 가입 계정으로 로그인해 주세요.',
};
const FORBIDDEN_MESSAGE = '관리자 권한이 없는 계정으로 로그인되어 있습니다. 로그아웃한 뒤 관리자 계정으로 다시 로그인해 주세요.';
const LOGOUT_FAILED_MESSAGE = '로그아웃하지 못했습니다. 잠시 후 다시 시도해 주세요.';

export class ApiError extends Error {
    constructor(message, { status, code } = {}) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.code = code;
        this.isAuthError = status === 401 || status === 403;
    }
}

function buildUrl(path, query) {
    const url = new URL(path, window.location.origin);
    if (!query) {
        return url;
    }

    Object.entries(query).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            return;
        }
        if (Array.isArray(value)) {
            value.forEach((item) => url.searchParams.append(key, item));
            return;
        }
        url.searchParams.set(key, String(value));
    });
    return url;
}

function currentPath() {
    return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

function rememberReturnPath() {
    try {
        window.sessionStorage.setItem(
            RETURN_PATH_KEY,
            JSON.stringify({ path: currentPath(), savedAt: Date.now() }),
        );
    } catch {
        // 저장소를 쓸 수 없으면 로그인 후 첫 화면에 머문다.
    }
}

function takeReturnPath() {
    try {
        const saved = window.sessionStorage.getItem(RETURN_PATH_KEY);
        window.sessionStorage.removeItem(RETURN_PATH_KEY);
        if (!saved) {
            return null;
        }
        const { path, savedAt } = JSON.parse(saved);
        const isAdminPath = typeof path === 'string' && path.startsWith('/admin/');
        const isFresh = Date.now() - Number(savedAt) < RETURN_PATH_TTL_MS;
        return isAdminPath && isFresh ? path : null;
    } catch {
        return null;
    }
}

function textElement(tagName, className, text) {
    const element = document.createElement(tagName);
    if (className) {
        element.className = className;
    }
    element.textContent = text;
    return element;
}

function providerLinks() {
    const actions = document.createElement('div');
    actions.className = 'login-actions';
    LOGIN_PROVIDERS.forEach(([provider, label]) => {
        const link = textElement('a', 'button button-secondary', label);
        link.href = `/oauth2/authorization/${provider}?client=admin`;
        link.addEventListener('click', rememberReturnPath);
        actions.append(link);
    });
    return actions;
}

function logoutButton() {
    const actions = document.createElement('div');
    actions.className = 'login-actions';
    const button = textElement('button', 'button button-primary', '로그아웃하고 다른 계정으로 로그인');
    button.type = 'button';
    button.addEventListener('click', async () => {
        button.disabled = true;
        try {
            const response = await fetch(LOGOUT_PATH, { method: 'POST', credentials: 'same-origin' });
            // 이미 세션이 끝났다면(401) 로그아웃된 것과 같다.
            if (!response.ok && response.status !== 401) {
                throw new Error(`logout failed: ${response.status}`);
            }
            showLogin();
        } catch {
            showLogin({ forbidden: true, message: LOGOUT_FAILED_MESSAGE });
        }
    });
    actions.append(button);
    return actions;
}

/**
 * 로그인 안내를 그린다. 호출할 때마다 내용을 다시 그려, 로그인 필요·권한 없음·로그인 실패를 구분해 보여준다.
 * @param {{ forbidden?: boolean, message?: string }} [options]
 */
function showLogin({ forbidden = false, message } = {}) {
    const shell = document.getElementById('app-shell');
    if (shell) {
        shell.classList.add('is-hidden');
    }

    let loginView = document.getElementById('login-view');
    if (!loginView) {
        loginView = document.createElement('main');
        loginView.id = 'login-view';
        loginView.className = 'login-view';
        document.body.append(loginView);
    }

    const card = document.createElement('section');
    card.className = 'login-card';
    card.append(
        textElement('p', 'eyebrow', 'PEAKDA OPERATIONS'),
        textElement('h1', null, forbidden ? '관리자 권한이 필요합니다' : '관리자 로그인이 필요합니다'),
        textElement('p', null, forbidden ? FORBIDDEN_MESSAGE : '관리자 권한이 있는 계정으로 로그인해 주세요.'),
    );
    if (message) {
        const alert = textElement('p', 'login-message', message);
        alert.setAttribute('role', 'alert');
        card.append(alert);
    }
    card.append(forbidden ? logoutButton() : providerLinks());

    loginView.replaceChildren(card);
    loginView.classList.remove('is-hidden');
}

/**
 * 백오피스 로그인에서 돌아온 직후를 처리한다. 화면 초기화 전에 호출한다.
 * - 로그인 실패 사유가 붙어 오면 안내를 보여주고 초기화를 멈춘다.
 * - 로그인 전에 보던 화면이 기억되어 있으면 그 화면으로 옮긴다.
 */
export async function resumeAfterLogin() {
    if (window.location.pathname !== LANDING_PATH) {
        return;
    }

    const url = new URL(window.location.href);
    const error = url.searchParams.get('error');
    if (error !== null) {
        takeReturnPath();
        // 다시 로그인할 때 실패 사유가 붙은 주소를 기억하지 않도록 주소에서 지운다.
        url.searchParams.delete('error');
        window.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`);
        // 주소창 값이므로 상속 속성(constructor 등)을 걸러 알려진 사유만 쓴다.
        const message = Object.hasOwn(LOGIN_ERROR_MESSAGES, error)
            ? LOGIN_ERROR_MESSAGES[error]
            : LOGIN_ERROR_MESSAGES.LOGIN_FAILED;
        showLogin({ message });
        throw new ApiError(message, { status: 401, code: error });
    }

    const returnPath = takeReturnPath();
    if (returnPath && returnPath !== currentPath()) {
        window.location.replace(returnPath);
        // 페이지가 바뀌므로 이 화면의 초기화는 이어가지 않는다.
        await new Promise(() => {});
    }
}

async function parseEnvelope(response) {
    const contentType = response.headers.get('content-type') ?? '';
    if (!contentType.includes('application/json')) {
        throw new ApiError(
            response.ok ? '서버 응답 형식을 확인할 수 없습니다.' : `요청에 실패했습니다. (${response.status})`,
            { status: response.status },
        );
    }

    const envelope = await response.json();
    if (envelope.code !== 'SUCCESS') {
        throw new ApiError(envelope.message || '요청을 처리하지 못했습니다.', {
            status: response.status,
            code: envelope.code,
        });
    }
    return envelope.data;
}

export async function request(path, { method = 'GET', body, query } = {}) {
    const headers = new Headers();
    let requestBody = body;

    if (body !== undefined && body !== null && !(body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
        requestBody = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(buildUrl(path, query), {
            method,
            body: requestBody,
            headers,
            credentials: 'same-origin',
        });
    } catch {
        throw new ApiError('서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.');
    }

    if (response.status === 401) {
        showLogin();
    } else if (response.status === 403) {
        showLogin({ forbidden: true });
    }

    try {
        return await parseEnvelope(response);
    } catch (error) {
        if (error instanceof ApiError) {
            if (response.status === 401 || response.status === 403) {
                error.isAuthError = true;
            }
            throw error;
        }
        throw new ApiError('서버 응답을 읽지 못했습니다.', { status: response.status });
    }
}

export function upload(path, file, query) {
    const formData = new FormData();
    formData.append('file', file);
    return request(path, { method: 'POST', body: formData, query });
}
