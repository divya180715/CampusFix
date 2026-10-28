const API_BASE = "http://localhost:5000/api";

async function apiRequest(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  let data = {};
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    throw new Error(data.error || "Something went wrong");
  }
  return data;
}

const api = {
  register: (payload) => apiRequest("/auth/register", { method: "POST", body: JSON.stringify(payload) }),
  login: (payload) => apiRequest("/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  logout: () => apiRequest("/auth/logout", { method: "POST" }),
  me: () => apiRequest("/auth/me"),
  meta: () => apiRequest("/meta"),

  createComplaint: (payload) => apiRequest("/complaints", { method: "POST", body: JSON.stringify(payload) }),
  listComplaints: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return apiRequest(`/complaints${qs ? `?${qs}` : ""}`);
  },
  getComplaint: (id) => apiRequest(`/complaints/${id}`),
  updateStatus: (id, payload) => apiRequest(`/complaints/${id}/status`, { method: "PATCH", body: JSON.stringify(payload) }),
  addComment: (id, comment) => apiRequest(`/complaints/${id}/comments`, { method: "POST", body: JSON.stringify({ comment }) }),
  deleteComplaint: (id) => apiRequest(`/complaints/${id}`, { method: "DELETE" }),

  adminStats: () => apiRequest("/admin/stats"),
  adminUsers: () => apiRequest("/admin/users"),
};

// Guards a page: redirects to login if not authenticated, or to correct
// dashboard if role doesn't match what's expected.
async function requireAuth(expectedRoles = null) {
  try {
    const { user } = await api.me();
    if (!user) {
      window.location.href = "login.html";
      return null;
    }
    if (expectedRoles && !expectedRoles.includes(user.role)) {
      window.location.href = dashboardUrlForRole(user.role);
      return null;
    }
    return user;
  } catch (e) {
    window.location.href = "login.html";
    return null;
  }
}

function dashboardUrlForRole(role) {
  if (role === "admin") return "admin-dashboard.html";
  if (role === "faculty") return "faculty-dashboard.html";
  return "student-dashboard.html";
}

function initials(name) {
  if (!name) return "?";
  return name.split(" ").map(w => w[0]).slice(0, 2).join("").toUpperCase();
}

function formatDate(str) {
  if (!str) return "-";
  const d = new Date(str.replace(" ", "T") + "Z");
  return d.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

function statusBadgeClass(status) {
  return {
    "Pending": "badge-pending",
    "In Progress": "badge-progress",
    "Resolved": "badge-resolved",
    "Rejected": "badge-rejected",
  }[status] || "badge-pending";
}

function priorityBadgeClass(priority) {
  return {
    "Low": "badge-low",
    "Medium": "badge-medium",
    "High": "badge-high",
    "Urgent": "badge-urgent",
  }[priority] || "badge-medium";
}

async function renderTopbar(user, activePage) {
  const el = document.getElementById("topbar");
  if (!el) return;
  el.innerHTML = `
    <div class="brand">
      <div class="brand-icon">🎓</div>
      <span>UniResolve</span>
    </div>
    <div class="topbar-right">
      <div class="user-chip">
        <div class="avatar">${initials(user.name)}</div>
        <span>${user.name}</span>
        <span class="role-badge">${user.role}</span>
      </div>
      <button class="btn btn-ghost btn-sm" id="logoutBtn">Logout</button>
    </div>
  `;
  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await api.logout();
    window.location.href = "login.html";
  });
}
