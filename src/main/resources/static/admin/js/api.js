const LOGIN_PATH = '/oauth2/authorization/kakao';

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

function showLogin() {
    const shell = document.getElementById('app-shell');
    if (shell) {
        shell.classList.add('is-hidden');
    }

    let loginView = document.getElementById('login-view');
    if (!loginView) {
        loginView = document.createElement('main');
        loginView.id = 'login-view';
        loginView.className = 'login-view';

        const card = document.createElement('section');
        card.className = 'login-card';

        const eyebrow = document.createElement('p');
        eyebrow.className = 'eyebrow';
        eyebrow.textContent = 'PEAKDA OPERATIONS';

        const title = document.createElement('h1');
        title.textContent = '관리자 로그인이 필요합니다';

        const description = document.createElement('p');
        description.textContent = '관리자 권한이 있는 카카오 계정으로 로그인해 주세요.';

        const link = document.createElement('a');
        link.className = 'button button-primary';
        link.href = LOGIN_PATH;
        link.textContent = '카카오로 로그인';

        card.append(eyebrow, title, description, link);
        loginView.append(card);
        document.body.append(loginView);
    }
    loginView.classList.remove('is-hidden');
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

    if (response.status === 401 || response.status === 403) {
        showLogin();
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

export function upload(path, file) {
    const formData = new FormData();
    formData.append('file', file);
    return request(path, { method: 'POST', body: formData });
}
