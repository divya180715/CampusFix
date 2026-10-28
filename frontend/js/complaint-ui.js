// Shared rendering helpers used by student/faculty/admin dashboards.

function renderComplaintList(container, complaints, currentUser) {
  if (!complaints.length) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="emoji">📭</div>
        <p>No complaints found.</p>
      </div>`;
    return;
  }

  container.innerHTML = complaints.map(c => `
    <div class="complaint-item" data-id="${c.id}">
      <div class="ci-left">
        <h3>${escapeHtml(c.title)}</h3>
        <div class="ci-desc">${escapeHtml(c.description)}</div>
        <div class="ci-meta">
          <span class="cat-tag">${escapeHtml(c.category)}</span>
          <span class="badge ${priorityBadgeClass(c.priority)}">${c.priority}</span>
          <span>👤 ${escapeHtml(c.filed_by_name || "Unknown")}</span>
          <span>🕒 ${formatDate(c.created_at)}</span>
          ${c.assigned_to_name ? `<span>➡️ ${escapeHtml(c.assigned_to_name)}</span>` : ""}
        </div>
      </div>
      <div class="ci-right">
        <span class="badge ${statusBadgeClass(c.status)}">${c.status}</span>
        <span style="font-size:0.72rem;color:var(--text-muted);">#${c.id}</span>
      </div>
    </div>
  `).join("");

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
  modalBody.innerHTML = `<p style="text-align:center;color:var(--text-muted);padding:30px;">Loading...</p>`;

  try {
    const { complaint } = await api.getComplaint(id);
    modalBody.innerHTML = buildComplaintDetailHtml(complaint, currentUser);
    attachModalHandlers(complaint, currentUser);
  } catch (err) {
    modalBody.innerHTML = `<p style="color:var(--danger);padding:20px;">${err.message}</p>`;
  }
}

function buildComplaintDetailHtml(c, currentUser) {
  const isAdmin = currentUser.role === "admin";

  const commentsHtml = c.comments.length
    ? c.comments.map(cm => `
        <div class="comment">
          <div class="comment-head">
            <span>${escapeHtml(cm.user_name)} <span class="role-tag">(${cm.user_role})</span></span>
            <span style="font-weight:400;color:var(--text-muted);">${formatDate(cm.created_at)}</span>
          </div>
          <div>${escapeHtml(cm.comment)}</div>
        </div>`).join("")
    : `<p style="color:var(--text-muted);font-size:0.85rem;">No comments yet.</p>`;

  const adminControls = isAdmin ? `
    <div class="admin-controls">
      <h4>Admin Actions</h4>
      <div class="form-row">
        <div class="form-group">
          <label>Update Status</label>
          <select id="statusSelect">
            ${["Pending", "In Progress", "Resolved", "Rejected"].map(s =>
              `<option value="${s}" ${s === c.status ? "selected" : ""}>${s}</option>`).join("")}
          </select>
        </div>
        <div class="form-group">
          <label>Assign to (User ID, optional)</label>
          <input type="number" id="assignInput" placeholder="e.g. 3" value="${c.assigned_to || ""}" />
        </div>
      </div>
      <div class="form-group">
        <label>Admin Notes</label>
        <textarea id="adminNotes" placeholder="Internal notes about resolution...">${escapeHtml(c.admin_notes || "")}</textarea>
      </div>
      <button class="btn btn-primary btn-sm" id="saveStatusBtn">Save Changes</button>
    </div>
  ` : (c.admin_notes ? `
    <div class="admin-controls">
      <h4>Admin Notes</h4>
      <p style="font-size:0.87rem;">${escapeHtml(c.admin_notes)}</p>
    </div>` : "");

  const deleteBtn = (isAdmin || (c.filed_by === currentUser.id && c.status === "Pending"))
    ? `<button class="btn btn-danger btn-sm" id="deleteComplaintBtn" style="margin-top:14px;">Delete Complaint</button>`
    : "";

  return `
    <div class="detail-row"><span>Complaint ID</span><span>#${c.id}</span></div>
    <div class="detail-row"><span>Status</span><span class="badge ${statusBadgeClass(c.status)}">${c.status}</span></div>
    <div class="detail-row"><span>Priority</span><span class="badge ${priorityBadgeClass(c.priority)}">${c.priority}</span></div>
    <div class="detail-row"><span>Category</span><span class="cat-tag">${escapeHtml(c.category)}</span></div>
    <div class="detail-row"><span>Filed by</span><span>${escapeHtml(c.filed_by_name || "Unknown")} ${c.is_anonymous ? "(anonymous)" : ""}</span></div>
    <div class="detail-row"><span>Assigned to</span><span>${escapeHtml(c.assigned_to_name || "Unassigned")}</span></div>
    <div class="detail-row"><span>Filed on</span><span>${formatDate(c.created_at)}</span></div>
    <div class="detail-row"><span>Last updated</span><span>${formatDate(c.updated_at)}</span></div>

    <h4 style="margin-top:18px;color:var(--purple-900);font-size:0.9rem;">Description</h4>
    <div class="detail-desc">${escapeHtml(c.description)}</div>

    ${adminControls}

    <h4 style="margin-top:18px;color:var(--purple-900);font-size:0.9rem;">Comments / Updates</h4>
    <div class="comment-list" id="commentList">${commentsHtml}</div>

    <div class="form-group" style="margin-top:14px;">
      <textarea id="newComment" placeholder="Add a comment or update..." style="min-height:70px;"></textarea>
    </div>
    <button class="btn btn-outline btn-sm" id="addCommentBtn">Post Comment</button>

    ${deleteBtn}
  `;
}

function attachModalHandlers(complaint, currentUser) {
  const isAdmin = currentUser.role === "admin";

  if (isAdmin) {
    document.getElementById("saveStatusBtn").addEventListener("click", async () => {
      const status = document.getElementById("statusSelect").value;
      const admin_notes = document.getElementById("adminNotes").value;
      const assignedRaw = document.getElementById("assignInput").value;
      const assigned_to = assignedRaw ? parseInt(assignedRaw) : null;
      try {
        await api.updateStatus(complaint.id, { status, admin_notes, assigned_to });
        await refreshCurrentView();
        openComplaintModal(complaint.id, currentUser);
      } catch (err) {
        alert(err.message);
      }
    });
  }

  document.getElementById("addCommentBtn").addEventListener("click", async () => {
    const text = document.getElementById("newComment").value.trim();
    if (!text) return;
    try {
      await api.addComment(complaint.id, text);
      openComplaintModal(complaint.id, currentUser);
    } catch (err) {
      alert(err.message);
    }
  });

  const delBtn = document.getElementById("deleteComplaintBtn");
  if (delBtn) {
    delBtn.addEventListener("click", async () => {
      if (!confirm("Delete this complaint? This cannot be undone.")) return;
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
  document.getElementById("modalOverlay").classList.remove("show");
}

// Each dashboard page defines this to reload its own list/stats after a change.
async function refreshCurrentView() {
  if (typeof loadDashboardData === "function") {
    await loadDashboardData();
  }
}
