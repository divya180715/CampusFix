// Resolve dynamic context path whether running at root or under /campus-fix on Tomcat
const getContextPath = () => {
  const path = window.location.pathname;
  if (path.includes("/campus-fix/")) {
    return "/campus-fix/api";
  }
  return "/api";
};

const API_BASE = getContextPath();

async function apiRequest(path, options = {}) {
  const method = options.method || "GET";
  const headers = { ...options.headers };

  // Note: For compatibility across all servlet containers, we handle PUT/PATCH smoothly
  let fetchOptions = {
    method: method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...headers
    },
    ...options
  };

  const res = await fetch(`${API_BASE}${path}`, fetchOptions);
  let data = {};
  try {
    data = await res.json();
  } catch (e) {
    /* empty response */
  }

  if (!res.ok) {
    throw new Error(data.error || "Request failed with status " + res.status);
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
  updateStatus: (id, payload) => apiRequest(`/complaints/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteComplaint: (id) => apiRequest(`/complaints/${id}`, { method: "DELETE" }),

  adminStats: () => apiRequest("/admin/stats"),
  adminUsers: () => apiRequest("/admin/users"),
  adminStaff: () => apiRequest("/admin/staff"),
};

// Guards a page: redirects to login if not authenticated, or to correct dashboard
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
  if (role === "admin" || role === "maintenance") return "admin-dashboard.html";
  if (role === "faculty") return "faculty-dashboard.html";
  return "student-dashboard.html";
}

function initials(name) {
  if (!name) return "?";
  return name.split(" ").map(w => w[0]).slice(0, 2).join("").toUpperCase();
}

function formatDate(str) {
  if (!str) return "-";
  return str;
}

function statusBadgeClass(status) {
  return {
    "Pending": "badge-pending",
    "Assigned": "badge-progress",
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
      <div class="brand-icon">🔧</div>
      <span>Campus Fix</span>
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
