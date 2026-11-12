/** Displays only response data. Submitted passwords never enter the inspector. */
let responseText = '';

export function renderResponse({ response, data, text, method, path, duration }) {
    responseText = text
        ? typeof data === 'string'
            ? data
            : JSON.stringify(data, null, 2)
        : '(empty response body)';

    document.querySelector('#response-body').textContent = responseText;
    document.querySelector('#response-request').textContent = `${method} ${path}`;
    document.querySelector('#response-time').textContent = `${Math.round(duration)} ms`;

    const status = document.querySelector('#response-status');
    status.textContent = `${response.status} ${response.statusText}`.trim();
    status.dataset.error = String(!response.ok);
}

export async function copyResponse() {
    await navigator.clipboard.writeText(responseText);
}
