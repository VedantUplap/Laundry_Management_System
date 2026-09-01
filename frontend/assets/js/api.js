/**
 * LSMS — Centralized API client
 * All fetch calls go through this module.
 */

const API_BASE = '/api';

const Auth = {
  getToken: ()  => localStorage.getItem('lsms_token'),
  getUser:  ()  => JSON.parse(localStorage.getItem('lsms_user') || 'null'),
  setSession: (token, user) => {
    localStorage.setItem('lsms_token', token);
    localStorage.setItem('lsms_user', JSON.stringify(user));
  },
  clearSession: () => {
    localStorage.removeItem('lsms_token');
    localStorage.removeItem('lsms_user');
  },
  isLoggedIn: () => !!localStorage.getItem('lsms_token'),
  hasRole: (roles) => {
    const u = Auth.getUser();
    return u && roles.includes(u.role);
  },
};

async function apiFetch(endpoint, options = {}) {
  const token = Auth.getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    // Auto-logout on 401
    if (res.status === 401) {
      Auth.clearSession();
      window.location.href = '/index.html';
      return;
    }
    throw new Error(data.message || `Request failed (${res.status})`);
  }
  return data;
}

const api = {
  // ── Auth ──────────────────────────────────────────
  login:    (body)       => apiFetch('/auth/login',    { method: 'POST', body }),
  register: (body)       => apiFetch('/auth/register', { method: 'POST', body }),
  me:       ()           => apiFetch('/auth/me'),

  // ── Customers ─────────────────────────────────────
  getCustomers:   (q)    => apiFetch(`/customers${q ? `?search=${q}` : ''}`),
  getCustomer:    (id)   => apiFetch(`/customers/${id}`),
  updateCustomer: (id, b)=> apiFetch(`/customers/${id}`, { method: 'PUT', body: b }),
  deleteCustomer: (id)   => apiFetch(`/customers/${id}`, { method: 'DELETE' }),
  getCustomerOrders:(id) => apiFetch(`/customers/${id}/orders`),

  // ── Services ──────────────────────────────────────
  getServices:    (activeOnly) => apiFetch(`/services${activeOnly ? '?active=true' : ''}`),
  getService:     (id)   => apiFetch(`/services/${id}`),
  createService:  (b)    => apiFetch('/services',     { method: 'POST', body: b }),
  updateService:  (id,b) => apiFetch(`/services/${id}`, { method: 'PUT', body: b }),
  deleteService:  (id)   => apiFetch(`/services/${id}`, { method: 'DELETE' }),

  // ── Orders ────────────────────────────────────────
  getOrders:      (params) => apiFetch(`/orders?${new URLSearchParams(params || {})}`),
  getOrder:       (id)   => apiFetch(`/orders/${id}`),
  createOrder:    (b)    => apiFetch('/orders',       { method: 'POST', body: b }),
  updateStatus:   (id,s) => apiFetch(`/orders/${id}/status`, { method: 'PUT', body: { status: s } }),
  cancelOrder:    (id)   => apiFetch(`/orders/${id}`, { method: 'DELETE' }),

  // ── Billing ───────────────────────────────────────
  getAllBilling:  ()      => apiFetch('/billing'),
  getBilling:    (orderId)=> apiFetch(`/billing/order/${orderId}`),
  createBilling: (b)     => apiFetch('/billing',      { method: 'POST', body: b }),

  // ── Payments ──────────────────────────────────────
  getPayments:   ()      => apiFetch('/payments'),
  createPayment: (b)     => apiFetch('/payments',     { method: 'POST', body: b }),

  // ── Inventory ─────────────────────────────────────
  getInventory:  ()      => apiFetch('/inventory'),
  getUsageLogs:  ()      => apiFetch('/inventory/usage'),
  createItem:    (b)     => apiFetch('/inventory',    { method: 'POST', body: b }),
  updateItem:    (id,b)  => apiFetch(`/inventory/${id}`, { method: 'PUT', body: b }),
  deleteItem:    (id)    => apiFetch(`/inventory/${id}`, { method: 'DELETE' }),
  logUsage:      (id,b)  => apiFetch(`/inventory/${id}/usage`, { method: 'POST', body: b }),

  // ── Delivery Agents ───────────────────────────────
  getAgents:     ()      => apiFetch('/delivery-agents'),
  createAgent:   (b)     => apiFetch('/delivery-agents', { method: 'POST', body: b }),
  updateAgent:   (id,b)  => apiFetch(`/delivery-agents/${id}`, { method: 'PUT', body: b }),

  // ── Deliveries ────────────────────────────────────
  getDeliveries: (params)=> apiFetch(`/deliveries?${new URLSearchParams(params || {})}`),
  updateDelivery:(id,b)  => apiFetch(`/deliveries/${id}`, { method: 'PUT', body: b }),

  // ── Feedback ──────────────────────────────────────
  getAllFeedback: ()      => apiFetch('/feedback'),
  getMyFeedback: ()      => apiFetch('/feedback/my'),
  submitFeedback:(b)     => apiFetch('/feedback',     { method: 'POST', body: b }),

  // ── Dashboard ─────────────────────────────────────
  getDashboardStats: ()  => apiFetch('/dashboard/stats'),

  // ── Reports ───────────────────────────────────────
  reportDailyRevenue:    (p) => apiFetch(`/reports/daily-revenue?${new URLSearchParams(p||{})}`),
  reportMonthlyRevenue:  ()  => apiFetch('/reports/monthly-revenue'),
  reportPendingDel:      ()  => apiFetch('/reports/pending-deliveries'),
  reportCustomerHistory: (id)=> apiFetch(`/reports/customer-history/${id}`),
  reportInventoryUsage:  ()  => apiFetch('/reports/inventory-usage'),
  reportPopularServices: ()  => apiFetch('/reports/popular-services'),
  reportOutstanding:     ()  => apiFetch('/reports/outstanding-payments'),
};

// ── Toast notifications ────────────────────────────────────────
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icons[type]}</span>
    <span class="toast-msg">${message}</span>
    <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
  `;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 4500);
}

// ── Modal helpers ──────────────────────────────────────────────
function openModal(id)  {
  const el = document.getElementById(id);
  if (el) el.classList.add('open');
}
function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('open');
}

// ── Loading state helpers ──────────────────────────────────────
function setLoading(btnId, loading, text = 'Save') {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  btn.innerHTML = loading
    ? '<span class="spinner" style="width:16px;height:16px;border-width:2px"></span> Please wait…'
    : text;
}

// ── Status badge ──────────────────────────────────────────────
function statusBadge(status) {
  const map = {
    'Pending':             'badge-yellow',
    'Picked Up':           'badge-blue',
    'Received':            'badge-blue',
    'Processing':          'badge-purple',
    'Washing':             'badge-purple',
    'Drying':              'badge-purple',
    'Ironing':             'badge-purple',
    'Packed':              'badge-cyan',
    'Ready for Delivery':  'badge-cyan',
    'Out for Delivery':    'badge-blue',
    'Delivered':           'badge-green',
    'Cancelled':           'badge-red',
    'Unpaid':              'badge-red',
    'Partially Paid':      'badge-yellow',
    'Paid':                'badge-green',
    'Overdue':             'badge-red',
    'In Stock':            'badge-green',
    'Low Stock':           'badge-yellow',
    'Out of Stock':        'badge-red',
    'Assigned':            'badge-blue',
    'In Transit':          'badge-purple',
    'Failed':              'badge-red',
  };
  return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

// ── Format helpers ────────────────────────────────────────────
const fmt = {
  currency: (v) => `₹${parseFloat(v || 0).toFixed(2)}`,
  date:     (d) => d ? new Date(d).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' }) : '—',
  datetime: (d) => d ? new Date(d).toLocaleString('en-IN') : '—',
  weight:   (v) => `${parseFloat(v || 0).toFixed(2)} kg`,
  stars:    (r) => '⭐'.repeat(r) + '☆'.repeat(5 - r),
};

// ── Guard: redirect if not logged in ──────────────────────────
function requireAuth(roles) {
  if (!Auth.isLoggedIn()) {
    window.location.href = '/index.html';
    return false;
  }
  if (roles && !Auth.hasRole(roles)) {
    showToast('Access denied.', 'error');
    setTimeout(() => window.location.href = '/index.html', 1500);
    return false;
  }
  return true;
}

// ── Populate sidebar user info ─────────────────────────────────
function populateSidebarUser() {
  const user = Auth.getUser();
  if (!user) return;
  const nameEl = document.getElementById('sidebar-user-name');
  const roleEl = document.getElementById('sidebar-user-role');
  const avatarEl = document.getElementById('sidebar-avatar');
  if (nameEl) nameEl.textContent = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
  if (roleEl) roleEl.textContent = user.role;
  if (avatarEl) avatarEl.textContent = (user.firstName || user.email || 'U')[0].toUpperCase();
}

// ── Logout ────────────────────────────────────────────────────
function logout() {
  Auth.clearSession();
  window.location.href = '/index.html';
}
