// Shared rendering helpers used by student/faculty/maintenance/admin dashboards.

function renderComplaintList(container, complaints, currentUser) {
  if (!complaints || !complaints.length) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="emoji">📭</div>
        <p>No complaints found.</p>
      </div>`;
    return;
  }

  container.innerHTML = complaints.map(c => {
    const filedName = c.filedByName || (c.anonymous ? "Anonymous" : "User #" + c.filedBy);
    const assignedName = c.assignedToName || (c.assignedTo ? "Technician #" + c.assignedTo : "");
    const dateFormatted = c.createdAt ? c.createdAt.toString().substring(0, 19) : "-";

    return `
    <div class="complaint-item" data-id="${c.id}">
      <div class="ci-left">
        <h3>${escapeHtml(c.title)}</h3>
        <div class="ci-desc">${escapeHtml(c.description)}</div>
        <div class="ci-meta">
          <span class="cat-tag">${escapeHtml(c.category)}</span>
          <span class="badge ${priorityBadgeClass(c.priority)}">${c.priority}</span>
          <span>👤 ${escapeHtml(filedName)}</span>
          <span>🕒 ${dateFormatted}</span>
          ${assignedName ? `<span>➡️ ${escapeHtml(assignedName)}</span>` : ""}
        </div>
      </div>
      <div class="ci-right">
        <span class="badge ${statusBadgeClass(c.status)}">${c.status}</span>
        <span style="font-size:0.75rem;color:var(--text-muted);font-weight:600;">#${c.id}</span>
      </div>
    </div>
  `}).join("");

  container.querySelectorAll(".complaint-item").forEach(item => {
    item.addEventListener("click", () => openComplaintModal(parseInt(item.dataset.id), currentUser));
  });
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str || "";
  return div.innerHTML;
}

async function openComplaintModal(id, currentUser) {
  const overlay = document.getElementById("modalOverlay");
  const modalBody = document.getElementById("modalBody");
  overlay.classList.add("show");
  modalBody.innerHTML = `<p style="text-align:center;color:var(--text-muted);padding:30px;">Loading complaint details...</p>`;

  try {
    const { complaint } = await api.getComplaint(id);
    modalBody.innerHTML = buildComplaintDetailHtml(complaint, currentUser);
    attachModalHandlers(complaint, currentUser);
  } catch (err) {
    modalBody.innerHTML = `<p style="color:var(--danger);padding:20px;">${err.message}</p>`;
  }
}

function buildComplaintDetailHtml(c, currentUser) {
  const isPrivileged = currentUser.role === "admin" || currentUser.role === "maintenance";
  const filedName = c.filedByName || (c.anonymous ? "Anonymous" : "User #" + c.filedBy);
  const assignedName = c.assignedToName || (c.assignedTo ? "Technician #" + c.assignedTo : "Unassigned");
  const createdDate = c.createdAt ? c.createdAt.toString().substring(0, 19) : "-";
  const updatedDate = c.updatedAt ? c.updatedAt.toString().substring(0, 19) : "-";

  const adminControls = isPrivileged ? `
    <div class="admin-controls">
      <h4>Maintenance & Status Management</h4>
      <div class="form-row">
        <div class="form-group">
          <label>Update Status</label>
          <select id="statusSelect">
            ${["Pending", "Assigned", "In Progress", "Resolved", "Rejected"].map(s =>
              `<option value="${s}" ${s === c.status ? "selected" : ""}>${s}</option>`).join("")}
          </select>
        </div>
        <div class="form-group">
          <label>Priority</label>
          <select id="prioritySelect">
            ${["Low", "Medium", "High", "Urgent"].map(p =>
              `<option value="${p}" ${p === c.priority ? "selected" : ""}>${p}</option>`).join("")}
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>Assign to Technician (User ID)</label>
        <input type="number" id="assignInput" placeholder="e.g. 3 (Staff ID)" value="${c.assignedTo || ""}" />
      </div>
      <div class="form-group">
        <label>Work Log / Maintenance Notes</label>
        <textarea id="adminNotes" placeholder="Resolution details or inspection findings...">${escapeHtml(c.adminNotes || "")}</textarea>
      </div>
      <button class="btn btn-primary btn-sm" id="saveStatusBtn">Save Update</button>
    </div>
  ` : (c.adminNotes ? `
    <div class="admin-controls">
      <h4>Maintenance Log</h4>
      <p style="font-size:0.87rem;white-space:pre-wrap;">${escapeHtml(c.adminNotes)}</p>
    </div>` : "");

  const deleteBtn = (currentUser.role === "admin" || (c.filedBy === currentUser.id && c.status === "Pending"))
    ? `<button class="btn btn-danger btn-sm" id="deleteComplaintBtn" style="margin-top:14px;">Delete Complaint</button>`
    : "";

  return `
    <div class="detail-row"><span>Complaint ID</span><span>#${c.id}</span></div>
    <div class="detail-row"><span>Status</span><span class="badge ${statusBadgeClass(c.status)}">${c.status}</span></div>
    <div class="detail-row"><span>Priority</span><span class="badge ${priorityBadgeClass(c.priority)}">${c.priority}</span></div>
    <div class="detail-row"><span>Category</span><span class="cat-tag">${escapeHtml(c.category)}</span></div>
    <div class="detail-row"><span>Filed by</span><span>${escapeHtml(filedName)} ${c.anonymous ? "(anonymous)" : ""}</span></div>
    <div class="detail-row"><span>Assigned Staff</span><span>${escapeHtml(assignedName)}</span></div>
    <div class="detail-row"><span>Filed Date</span><span>${createdDate}</span></div>
    <div class="detail-row"><span>Last Updated</span><span>${updatedDate}</span></div>

    <h4 style="margin-top:18px;color:var(--purple-900);font-size:0.95rem;">Issue Description</h4>
    <div class="detail-desc">${escapeHtml(c.description)}</div>

    ${adminControls}
    ${deleteBtn}
  `;
}

function attachModalHandlers(complaint, currentUser) {
  const isPrivileged = currentUser.role === "admin" || currentUser.role === "maintenance";

  if (isPrivileged) {
    const saveBtn = document.getElementById("saveStatusBtn");
    if (saveBtn) {
      saveBtn.addEventListener("click", async () => {
        const status = document.getElementById("statusSelect").value;
        const priority = document.getElementById("prioritySelect").value;
        const admin_notes = document.getElementById("adminNotes").value;
        const assignedRaw = document.getElementById("assignInput").value;
        const assigned_to = assignedRaw ? parseInt(assignedRaw) : 0;
        try {
          await api.updateStatus(complaint.id, { status, priority, admin_notes, assigned_to });
          await refreshCurrentView();
          openComplaintModal(complaint.id, currentUser);
        } catch (err) {
          alert(err.message);
        }
      });
    }
  }

  const delBtn = document.getElementById("deleteComplaintBtn");
  if (delBtn) {
    delBtn.addEventListener("click", async () => {
      if (!confirm("Are you sure you want to delete this complaint?")) return;
      try {
        await api.deleteComplaint(complaint.id);
        closeModal();
        await refreshCurrentView();
      } catch (err) {
        alert(err.message);
      }
    });
  }
}

function closeModal() {
  const overlay = document.getElementById("modalOverlay");
  if (overlay) overlay.classList.remove("show");
}

async function refreshCurrentView() {
  if (typeof loadDashboardData === "function") {
    await loadDashboardData();
  }
}
