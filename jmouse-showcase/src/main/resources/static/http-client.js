/** Same-origin transport and CSRF handling. This module does not manipulate the page. */
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export async function sendRequest(path, { method = 'GET', body, contentType, protect = true } = {}) {
    if (!path.startsWith('/') || path.startsWith('//')) {
        throw new Error('Use a local application path.');
    }

    const headers = { Accept: 'application/json' };
    if (contentType) {
        headers['Content-Type'] = contentType;
    }
    if (!SAFE_METHODS.has(method) && protect) {
        const token = await acquireCsrfToken();
        headers[token.headerName] = token.token;
    }

    const startedAt = performance.now();
    const response = await fetch(path, { method, headers, body, credentials: 'same-origin' });
    const text = await response.text();
    return {
        response,
        data: parseResponse(text),
        text,
        method,
        path,
        duration: performance.now() - startedAt
    };
}

export function requireSuccess({ response, data, path }) {
    if (!response.ok) {
        throw new HttpResponseError(response.status, path);
    }
    return data;
}

async function acquireCsrfToken() {
    const response = await fetch('/api/csrf', {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' }
    });
    if (!response.ok) {
        throw new Error('Could not acquire CSRF token.');
    }
    return response.json();
}

function parseResponse(text) {
    if (!text) {
        return null;
    }
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

/** Keeps HTTP failure details available to page actions without coupling transport to the DOM. */
export class HttpResponseError extends Error {
    constructor(status, path) {
        super(failureMessage(status, path));
        this.name = 'HttpResponseError';
        this.status = status;
        this.path = path;
    }
}

function failureMessage(status, path) {
    if (status === 401) {
        return path === '/login'
            ? 'Sign-in failed. Check the username and password, then try again.'
            : 'Sign in to continue. Your session may have expired.';
    }
    const messages = {
        400: 'The request was rejected. Check the submitted values.',
        403: 'Access denied. Check your permissions and the CSRF token.',
        404: 'The requested endpoint was not found.',
        405: 'This endpoint does not accept the selected HTTP method.',
        413: 'The request body exceeds the allowed size.',
        415: 'This endpoint does not accept the submitted content type.',
        429: 'Too many requests. Wait briefly and try again.'
    };
    return messages[status] || `The server could not complete the request (HTTP ${status}).`;
}
