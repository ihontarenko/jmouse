import { sendRequest, requireSuccess, HttpResponseError } from './http-client.js';
import { renderResponse, copyResponse } from './response-inspector.js';

const selectElement = (selector) => document.querySelector(selector);
const sections = {
    routes: {
        title: 'Routes',
        label: 'Explore the contract',
        description: 'Discover endpoints, send a request, inspect the response.',
        load: loadRoutes
    },
    database: {
        title: 'Database',
        label: 'Persist real data',
        description: 'Insert a row, restart the application and find it again.',
        load: loadEntries
    },
    transactions: {
        title: 'Transactions',
        label: 'Observe atomic changes',
        description: 'Commit a transfer or force a failure between two writes.',
        load: loadAccounts
    },
    security: {
        title: 'Security',
        label: 'Exercise access boundaries',
        description: 'Compare a guest, a member and an administrator.',
        load: loadSession
    }
};

// Shared page feedback and response inspection.
function showNotice(message, error = false) {
    const notice = selectElement('#notice');
    notice.textContent = message;
    notice.dataset.error = String(error);
    notice.hidden = !message;
}

function showRequestError(error) {
    showNotice(error.message, true);
    if (error instanceof HttpResponseError && error.status === 401 && error.path !== '/login') {
        selectElement('#identity').textContent = 'Guest · public access';
        const signInLink = document.createElement('a');
        signInLink.href = '#security';
        signInLink.textContent = 'Sign in';
        selectElement('#notice').append(' ', signInLink);
    }
}

async function request(path, { inspect = true, ...options } = {}) {
    const result = await sendRequest(path, options);
    if (inspect) {
        renderResponse(result);
    }
    return result;
}

function appendCell(row, value, { code = false } = {}) {
    const cell = document.createElement('td');
    if (code) {
        const content = document.createElement('code');
        content.textContent = value;
        cell.append(content);
    } else {
        cell.textContent = value;
    }
    row.append(cell);
    return cell;
}

function renderTableMessage(selector, columnCount, message) {
    const row = document.createElement('tr');
    appendCell(row, message).colSpan = columnCount;
    selectElement(selector).replaceChildren(row);
}

// Route catalogue.
async function loadRoutes() {
    try {
        const routes = requireSuccess(await request('/api/routes', { inspect: false }));
        selectElement('#route-rows').replaceChildren(...routes.map(createRouteRow));
    } catch (error) {
        renderTableMessage('#route-rows', 5, 'Routes unavailable. Use Refresh to retry.');
        throw error;
    }
}

function createRouteRow(route) {
    const row = document.createElement('tr');
    appendCell(row, route.method).className = 'method';
    appendCell(row, route.path, { code: true });
    appendCell(row, route.description);
    appendCell(row, route.access);

    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'Use';
    button.setAttribute('aria-label', `Use ${route.method} ${route.path}`);
    button.addEventListener('click', () => {
        selectElement('#request-method').value = route.method;
        selectElement('#request-path').value = route.path.replace('{name}', 'world');
        selectElement('#request-path').focus();
    });
    appendCell(row, '').append(button);
    return row;
}

// Persistent entries.
async function loadEntries() {
    try {
        const entries = requireSuccess(await request('/api/entries', { inspect: false }));
        if (!entries.length) {
            renderTableMessage('#entry-rows', 2, 'No entries yet. Sign in and insert your first row.');
            return;
        }
        const rows = entries.map((entry) => {
            const row = document.createElement('tr');
            appendCell(row, entry.id);
            appendCell(row, entry.name);
            return row;
        });
        selectElement('#entry-rows').replaceChildren(...rows);
    } catch (error) {
        renderTableMessage('#entry-rows', 2, 'Could not load entries.');
        throw error;
    }
}

// Account balances and transaction form.
async function loadAccounts() {
    const accounts = requireSuccess(await request('/api/accounts', { inspect: false }));
    selectElement('#account-balances').replaceChildren(...accounts.map(createBalance));
    updateAccountOptions('#transfer-from', accounts, 0);
    updateAccountOptions('#transfer-to', accounts, 1);
    return accounts;
}

function createBalance(account) {
    const container = document.createElement('div');
    container.className = 'balance';
    const label = document.createElement('span');
    label.textContent = account.name;
    const value = document.createElement('strong');
    value.textContent = account.balance.toLocaleString('en');
    container.append(label, value);
    return container;
}

function updateAccountOptions(selector, accounts, defaultIndex) {
    const select = selectElement(selector);
    const previousValue = select.value;
    select.replaceChildren(...accounts.map((account) => new Option(account.name, String(account.id))));
    if (accounts.some((account) => String(account.id) === previousValue)) {
        select.value = previousValue;
    } else {
        select.selectedIndex = defaultIndex;
    }
}

async function loadSession(inspect = false) {
    const session = requireSuccess(await request('/api/session', { inspect }));
    selectElement('#identity').textContent = session.authenticated
        ? `${session.username} · ${session.authorities.join(', ')}`
        : 'Guest · public access';
    return session;
}

// Navigation.
function showSection() {
    const requestedSection = location.hash.slice(1);
    const sectionName = Object.hasOwn(sections, requestedSection) ? requestedSection : 'routes';
    for (const name of Object.keys(sections)) {
        const active = name === sectionName;
        selectElement(`#${name}`).hidden = !active;
        const link = selectElement(`.navigation a[href="#${name}"]`);
        if (active) {
            link.setAttribute('aria-current', 'page');
        } else {
            link.removeAttribute('aria-current');
        }
    }

    const section = sections[sectionName];
    const number = String(Object.keys(sections).indexOf(sectionName) + 1).padStart(2, '0');
    selectElement('#page-title').textContent = section.title;
    selectElement('#section-label').textContent = `${number} / ${section.label}`;
    selectElement('#page-description').textContent = section.description;
    showNotice('');
    section.load().catch((error) => showRequestError(error));
}

// Named actions keep event registration separate from application behavior.
async function sendPlaygroundRequest() {
    await request(selectElement('#request-path').value, { method: selectElement('#request-method').value });
}

async function insertEntry(event) {
    const form = event.currentTarget;
    const fields = new FormData(form);
    requireSuccess(
        await request('/api/entries', {
            method: 'POST',
            contentType: 'application/json',
            body: JSON.stringify({ name: fields.get('name') })
        })
    );
    form.reset();
    await loadEntries();
    showNotice('Row saved to the H2 file.');
}

async function submitTransfer(event) {
    const fields = new FormData(event.currentTarget);
    const result = requireSuccess(
        await request('/api/transfers', {
            method: 'POST',
            contentType: 'application/json',
            body: JSON.stringify({
                fromId: Number(fields.get('fromId')),
                toId: Number(fields.get('toId')),
                amount: Number(fields.get('amount')),
                rollback: fields.has('rollback')
            })
        })
    );
    await loadAccounts();
    const message = selectElement('#transfer-result');
    message.hidden = false;
    message.textContent =
        result.status === 'rolledBack'
            ? 'Rolled back. The debit was undone; both balances are unchanged.'
            : 'Committed. Both balances and the transfer record were saved together.';
}

async function signIn(event) {
    const form = event.currentTarget;
    const parameters = new URLSearchParams(new FormData(form));
    try {
        requireSuccess(
            await request('/login', {
                method: 'POST',
                contentType: 'application/x-www-form-urlencoded',
                body: parameters.toString()
            })
        );
        await loadSession();
        showNotice('Signed in. The session now carries your identity.');
    } finally {
        form.elements.password.value = '';
    }
}

async function signOut() {
    requireSuccess(await request('/logout', { method: 'POST' }));
    await loadSession();
    showNotice('Signed out. The previous session was invalidated.');
}

async function sendUnprotectedWrite() {
    await request('/api/entries', {
        method: 'POST',
        contentType: 'application/json',
        body: JSON.stringify({ name: 'Must not be saved' }),
        protect: false
    });
}

function registerAction(selector, eventName, action) {
    selectElement(selector).addEventListener(eventName, async (event) => {
        event.preventDefault();
        const button = event.submitter || event.currentTarget;
        const isButton = button.tagName === 'BUTTON';
        if (isButton) {
            button.disabled = true;
        }
        showNotice('');
        try {
            await action(event);
        } catch (error) {
            showRequestError(error);
        } finally {
            if (isButton) {
                button.disabled = false;
            }
        }
    });
}

function initialize() {
    registerAction('#route-form', 'submit', sendPlaygroundRequest);
    registerAction('#refresh-routes', 'click', loadRoutes);
    registerAction('#refresh-entries', 'click', loadEntries);
    registerAction('#refresh-accounts', 'click', loadAccounts);
    registerAction('#refresh-session', 'click', () => loadSession(true));
    registerAction('#entry-form', 'submit', insertEntry);
    registerAction('#transfer-form', 'submit', submitTransfer);
    registerAction('#login-form', 'submit', signIn);
    registerAction('#logout', 'click', signOut);
    registerAction('#csrf-rejection', 'click', sendUnprotectedWrite);
    registerAction('#copy-response', 'click', async () => {
        await copyResponse();
        showNotice('Response body copied.');
    });

    document.querySelectorAll('[data-request]').forEach((button) => {
        button.addEventListener('click', () => {
            request(button.dataset.request, { method: button.dataset.method || 'GET' }).catch((error) =>
                showRequestError(error)
            );
        });
    });
    window.addEventListener('hashchange', showSection);
    showSection();
    loadSession().catch((error) => showRequestError(error));
}

initialize();
