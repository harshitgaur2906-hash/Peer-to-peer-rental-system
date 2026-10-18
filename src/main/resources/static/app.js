(() => {
  'use strict';

  const CATEGORIES = ['Electronics', 'Books & Notes', 'Sports & Fitness', 'Cycles & Vehicles',
    'Furniture', 'Clothing & Costumes', 'Tools & Lab Gear', 'Kitchen', 'Other'];

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const money = (n) => '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 });
  const todayISO = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const fmtDate = (d) => new Date(d + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });

  let me = null;
  let currentView = null;
  const itemCache = new Map();

  // ---------- API ----------
  async function api(path, { method = 'GET', body } = {}) {
    const res = await fetch('/api' + path, {
      method,
      credentials: 'same-origin',
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
    let data = null;
    if (res.status !== 204) { try { data = await res.json(); } catch { /* no body */ } }
    if (!res.ok) {
      const err = new Error((data && data.error) || 'Something went wrong');
      err.status = res.status;
      throw err;
    }
    return data;
  }

  let toastTimer;
  function toast(msg, isError = false) {
    const t = $('#toast');
    t.textContent = msg;
    t.className = 'toast show' + (isError ? ' err' : '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.className = 'toast'; }, 3300);
  }

  function handleError(err, form) {
    if (err.status === 401 && me) { me = null; showAuth(); toast('Session expired. Please log in again.', true); return; }
    const box = form && $('[data-error]', form);
    if (box) box.textContent = err.message; else toast(err.message, true);
  }

  // ---------- Views ----------
  function showAuth() {
    currentView = null;
    $$('.view').forEach((v) => v.classList.add('hidden'));
    $('#view-auth').classList.remove('hidden');
    $('#nav').classList.add('hidden');
    $('#user-chip').classList.add('hidden');
  }

  function enterApp() {
    $('#nav').classList.remove('hidden');
    $('#user-chip').classList.remove('hidden');
    $('#user-name').textContent = me.name;
    showView('browse');
    refreshBadge();
  }

  function showView(name) {
    currentView = name;
    $$('.view').forEach((v) => v.classList.add('hidden'));
    $('#view-' + name).classList.remove('hidden');
    $$('.nav-btn').forEach((b) => b.classList.toggle('active', b.dataset.view === name));
    ({ browse: loadBrowse, mine: loadMine, requests: loadRequests, profile: loadProfile })[name]();
  }

  // ---------- Browse ----------
  function itemCard(i, mode) {
    itemCache.set(i.id, i);
    const own = me && i.ownerId === me.id;
    let actions = '';
    if (mode === 'mine') {
      actions = `<button class="btn btn-ghost btn-sm" data-action="edit" data-id="${i.id}">Edit</button>
                 <button class="btn btn-bad btn-sm" data-action="delete" data-id="${i.id}">Delete</button>`;
    } else if (own) {
      actions = '<span class="muted">Your listing</span>';
    } else {
      actions = `<button class="btn btn-primary btn-sm" data-action="request" data-id="${i.id}">Request to rent</button>`;
    }
    return `<article class="card item">
      <span class="chip">${esc(i.category)}</span>
      <h3>${esc(i.title)}</h3>
      <div class="price">${money(i.pricePerDay)} <small>/ day</small></div>
      <p class="desc">${esc(i.description || 'No description provided.')}</p>
      <div class="meta">${i.location ? '📍 ' + esc(i.location) + ' · ' : ''}by ${esc(i.ownerName)}
        ${mode === 'mine' ? ' · ' + (i.available ? '🟢 Available' : '⚪ Hidden') : ''}</div>
      <div class="actions">${actions}</div>
    </article>`;
  }

  async function loadBrowse() {
    const grid = $('#browse-grid');
    const f = new FormData($('#filters'));
    const params = new URLSearchParams();
    for (const k of ['q', 'category', 'maxPrice']) { const v = (f.get(k) || '').toString().trim(); if (v) params.set(k, v); }
    try {
      const items = await api('/items' + (params.toString() ? '?' + params : ''));
      grid.innerHTML = items.length ? items.map((i) => itemCard(i, 'browse')).join('')
        : '<div class="empty">No items found. Try different filters.</div>';
    } catch (e) { handleError(e); }
  }

  // ---------- My listings ----------
  async function loadMine() {
    try {
      const items = await api('/items/mine');
      $('#mine-grid').innerHTML = items.length ? items.map((i) => itemCard(i, 'mine')).join('')
        : '<div class="empty">You have no listings yet. Click “+ New listing” to add one.</div>';
    } catch (e) { handleError(e); }
  }

  function openItemDialog(item) {
    const f = $('#item-form');
    f.reset();
    $('[data-error]', f).textContent = '';
    $('#item-dialog-title').textContent = item ? 'Edit listing' : 'New listing';
    f.elements['id'].value = item ? item.id : '';
    if (item) {
      f.elements['title'].value = item.title; f.category.value = item.category; f.pricePerDay.value = item.pricePerDay;
      f.location.value = item.location || ''; f.description.value = item.description || '';
      f.available.checked = item.available;
    }
    $('#item-dialog').showModal();
  }

  // ---------- Requests ----------
  function bookingCard(b, mode) {
    const other = mode === 'received' ? 'Requested by ' + esc(b.renterName) : 'Owner: ' + esc(b.ownerName);
    let actions = '';
    if (mode === 'received' && b.status === 'PENDING') {
      actions = `<button class="btn btn-ok btn-sm" data-action="approve" data-id="${b.id}">Approve</button>
                 <button class="btn btn-bad btn-sm" data-action="decline" data-id="${b.id}">Decline</button>`;
    } else if (mode === 'sent' && (b.status === 'PENDING' || b.status === 'APPROVED')) {
      actions = `<button class="btn btn-ghost btn-sm" data-action="cancel" data-id="${b.id}">Cancel request</button>`;
    }
    return `<article class="card req">
      <div class="info">
        <h3>${esc(b.itemTitle)} <span class="status ${esc(b.status)}">${esc(b.status)}</span></h3>
        <p class="muted">${other} · ${fmtDate(b.startDate)} → ${fmtDate(b.endDate)} · <strong>${money(b.totalPrice)}</strong></p>
        ${b.message ? `<p>“${esc(b.message)}”</p>` : ''}
      </div>
      <div class="actions">${actions}</div>
    </article>`;
  }

  async function loadRequests() {
    try {
      const [received, sent] = await Promise.all([api('/bookings/received'), api('/bookings/mine')]);
      $('#req-received').innerHTML = received.length ? received.map((b) => bookingCard(b, 'received')).join('')
        : '<div class="empty">No requests received yet.</div>';
      $('#req-sent').innerHTML = sent.length ? sent.map((b) => bookingCard(b, 'sent')).join('')
        : '<div class="empty">You have not requested any items yet.</div>';
      setBadge(received.filter((b) => b.status === 'PENDING').length);
    } catch (e) { handleError(e); }
  }

  function setBadge(n) {
    for (const id of ['req-badge', 'tab-received-count']) {
      const el = $('#' + id);
      el.textContent = n;
      el.classList.toggle('hidden', n === 0);
    }
  }

  async function refreshBadge() {
    try {
      const received = await api('/bookings/received');
      setBadge(received.filter((b) => b.status === 'PENDING').length);
    } catch { /* ignore */ }
  }

  function switchReqTab(name) {
    $$('[data-req-tab]').forEach((t) => t.classList.toggle('active', t.dataset.reqTab === name));
    $('#req-received').classList.toggle('hidden', name !== 'received');
    $('#req-sent').classList.toggle('hidden', name !== 'sent');
  }

  async function changeStatus(id, status) {
    try {
      await api(`/bookings/${id}/status`, { method: 'PATCH', body: { status } });
      toast('Request ' + status.toLowerCase());
      loadRequests();
    } catch (e) { handleError(e); }
  }

  // ---------- Booking dialog ----------
  function openBookDialog(item) {
    const f = $('#book-form');
    f.reset();
    $('[data-error]', f).textContent = '';
    f.itemId.value = item.id;
    $('#book-title').textContent = 'Rent: ' + item.title;
    $('#book-sub').textContent = money(item.pricePerDay) + ' per day · owner ' + item.ownerName;
    f.startDate.min = todayISO();
    f.endDate.min = todayISO();
    updateEstimate();
    $('#book-dialog').showModal();
  }

  function updateEstimate() {
    const f = $('#book-form');
    const el = $('#book-estimate');
    const item = itemCache.get(Number(f.itemId.value));
    if (!item || !f.startDate.value || !f.endDate.value) { el.textContent = ''; return; }
    const days = Math.round((Date.parse(f.endDate.value) - Date.parse(f.startDate.value)) / 86400000) + 1;
    el.textContent = days < 1 ? 'End date must be after the start date'
      : `${days} day${days > 1 ? 's' : ''} × ${money(item.pricePerDay)} = ${money(days * item.pricePerDay)}`;
  }

  // ---------- Profile ----------
  async function loadProfile() {
    $('#profile-name').textContent = me.name;
    $('#profile-email').textContent = me.email;
    $('#profile-avatar').textContent = me.name.trim().charAt(0).toUpperCase();
    try {
      const [mine, rec, sent] = await Promise.all([api('/items/mine'), api('/bookings/received'), api('/bookings/mine')]);
      $('#stat-listings').textContent = mine.length;
      $('#stat-received').textContent = rec.length;
      $('#stat-sent').textContent = sent.length;
    } catch (e) { handleError(e); }
  }

  // ---------- Events ----------
  document.addEventListener('click', async (e) => {
    const el = e.target.closest('[data-action], [data-view], [data-auth-tab], [data-req-tab]');
    if (!el) return;

    if (el.dataset.view) return showView(el.dataset.view);
    if (el.dataset.reqTab) return switchReqTab(el.dataset.reqTab);
    if (el.dataset.authTab) {
      const t = el.dataset.authTab;
      $$('[data-auth-tab]').forEach((b) => b.classList.toggle('active', b.dataset.authTab === t));
      $('#login-form').classList.toggle('hidden', t !== 'login');
      $('#register-form').classList.toggle('hidden', t !== 'register');
      return;
    }

    const id = Number(el.dataset.id);
    switch (el.dataset.action) {
      case 'new-item': return openItemDialog(null);
      case 'edit': return openItemDialog(itemCache.get(id));
      case 'request': return openBookDialog(itemCache.get(id));
      case 'close-dialog': return el.closest('dialog').close();
      case 'approve': return changeStatus(id, 'APPROVED');
      case 'decline': return changeStatus(id, 'DECLINED');
      case 'cancel': if (confirm('Cancel this request?')) changeStatus(id, 'CANCELLED'); return;
      case 'delete':
        if (!confirm('Delete this listing permanently?')) return;
        try { await api('/items/' + id, { method: 'DELETE' }); toast('Listing deleted'); loadMine(); }
        catch (err) { handleError(err); }
        return;
    }
  });

  async function submitAuth(e, path) {
    e.preventDefault();
    const form = e.target;
    const btn = $('button[type=submit]', form);
    $('[data-error]', form).textContent = '';
    btn.disabled = true;
    try {
      const data = Object.fromEntries(new FormData(form));
      me = await api(path, { method: 'POST', body: data });
      form.reset();
      enterApp();
    } catch (err) { handleError(err, form); }
    finally { btn.disabled = false; }
  }
  $('#login-form').addEventListener('submit', (e) => submitAuth(e, '/auth/login'));
  $('#register-form').addEventListener('submit', (e) => submitAuth(e, '/auth/register'));

  $('#logout-btn').addEventListener('click', async () => {
    try { await api('/auth/logout', { method: 'POST' }); } catch { /* ignore */ }
    me = null; itemCache.clear(); showAuth();
  });

  $('#filters').addEventListener('submit', (e) => { e.preventDefault(); loadBrowse(); });
  let debounce;
  $('#filters [name=q]').addEventListener('input', () => { clearTimeout(debounce); debounce = setTimeout(loadBrowse, 350); });
  $('#filters [name=category]').addEventListener('change', loadBrowse);

  $('#item-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    const body = {
      title: f.elements['title'].value.trim(), category: f.category.value, pricePerDay: Number(f.pricePerDay.value),
      location: f.location.value.trim(), description: f.description.value.trim(), available: f.available.checked,
    };
    const id = f.elements['id'].value;
    try {
      await api(id ? '/items/' + id : '/items', { method: id ? 'PUT' : 'POST', body });
      $('#item-dialog').close();
      toast(id ? 'Listing updated' : 'Listing created');
      if (currentView === 'mine') loadMine(); else showView('mine');
    } catch (err) { handleError(err, f); }
  });

  $('#book-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    if (f.endDate.value < f.startDate.value) { $('[data-error]', f).textContent = 'End date must be on or after the start date'; return; }
    const body = { itemId: Number(f.itemId.value), startDate: f.startDate.value, endDate: f.endDate.value, message: f.message.value.trim() };
    try {
      await api('/bookings', { method: 'POST', body });
      $('#book-dialog').close();
      toast('Request sent to the owner');
      showView('requests');
      switchReqTab('sent');
    } catch (err) { handleError(err, f); }
  });
  for (const n of ['startDate', 'endDate']) {
    $('#book-form')[n].addEventListener('change', () => {
      const f = $('#book-form');
      if (n === 'startDate' && f.startDate.value) f.endDate.min = f.startDate.value;
      updateEstimate();
    });
  }

  // ---------- Init ----------
  for (const sel of ['#filters [name=category]', '#item-form [name=category]']) {
    const s = $(sel);
    for (const c of CATEGORIES) s.insertAdjacentHTML('beforeend', `<option value="${esc(c)}">${esc(c)}</option>`);
  }

  (async () => {
    try { me = await api('/auth/me'); enterApp(); } catch { showAuth(); }
  })();
})();
