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
        ${c.location ? `<div style="font-size:0.83rem;color:var(--purple-800);font-weight:600;margin-bottom:4px;">📍 ${escapeHtml(c.location)}</div>` : ""}
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

function renderStatusTrackerHtml(status) {
  if (status === "Rejected") {
    return `
      <div class="status-tracker">
        <div class="status-step completed">
          <div class="step-circle">✓</div>
          <div class="step-label">Submitted</div>
        </div>
        <div class="status-step rejected">
          <div class="step-circle">✕</div>
          <div class="step-label">Rejected</div>
        </div>
      </div>
    `;
  }

  const steps = [
    { key: "Pending", label: "Submitted" },
    { key: "Assigned", label: "Assigned" },
    { key: "In Progress", label: "In Progress" },
    { key: "Resolved", label: "Resolved" }
  ];

  const statusOrder = { "Pending": 0, "Assigned": 1, "In Progress": 2, "Resolved": 3 };
  const currentIdx = statusOrder[status] !== undefined ? statusOrder[status] : 0;

  return `
    <div class="status-tracker">
      ${steps.map((st, idx) => {
        let cls = "";
        let icon = idx + 1;
        if (idx < currentIdx) {
          cls = "completed";
          icon = "✓";
        } else if (idx === currentIdx) {
          cls = "active";
          if (idx === 3) { cls += " completed"; icon = "✓"; }
        }
        return `
          <div class="status-step ${cls}">
            <div class="step-circle">${icon}</div>
            <div class="step-label">${st.label}</div>
          </div>
        `;
      }).join("")}
    </div>
  `;
}

function buildComplaintDetailHtml(c, currentUser) {
  const isAdmin = currentUser.role === "admin";
  const isMaintenance = currentUser.role === "maintenance";
  const filedName = c.filedByName || (c.anonymous ? "Anonymous" : "User #" + c.filedBy);
  const assignedName = c.assignedToName || (c.assignedTo ? "Technician #" + c.assignedTo : "Unassigned (Pending Admin Assignment)");
  const createdDate = c.createdAt ? c.createdAt.toString().substring(0, 19) : "-";
  const updatedDate = c.updatedAt ? c.updatedAt.toString().substring(0, 19) : "-";
  const resolvedDate = c.resolvedAt ? c.resolvedAt.toString().substring(0, 19) : null;

  // Status Stepper: visible to all users (Student, Faculty, Maintenance, Admin)
  const trackerHtml = renderStatusTrackerHtml(c.status);

  // Resolution summary box: shown to everyone once technician/admin completes the work
  let resolutionBox = "";
  if (c.resolutionNotes || c.resolutionImage) {
    resolutionBox = `
      <div class="resolution-box">
        <h4>✅ Technician Work Summary & Resolution Proof</h4>
        ${c.resolutionNotes ? `<p>${escapeHtml(c.resolutionNotes)}</p>` : ""}
        ${c.resolutionImage ? `<img src="${c.resolutionImage}" class="resolution-preview-img" alt="Resolution Proof" />` : ""}
        ${resolvedDate ? `<div style="margin-top:8px;font-size:0.78rem;color:#166534;font-weight:600;">Resolved on: ${resolvedDate}</div>` : ""}
      </div>
    `;
  }

  // Admin Controls: full overview, priority adjustment, technician assignment, admin notes
  let adminControls = "";
  if (isAdmin) {
    adminControls = `
      <div class="admin-controls" style="margin-top:20px; border-left:4px solid var(--purple-600);">
        <h4 style="color:var(--purple-900);margin-bottom:12px;">🛡️ Administrator Management & Assignment</h4>
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
          <label>Assign to Maintenance Staff</label>
          <select id="assignStaffSelect">
            <option value="">-- Select Technician / Staff --</option>
          </select>
        </div>
        <div class="form-group">
          <label>Admin Triage / Internal Notes</label>
          <textarea id="adminNotes" placeholder="Admin notes or instructions for technician...">${escapeHtml(c.adminNotes || "")}</textarea>
        </div>
        <button class="btn btn-primary btn-sm" id="saveAdminBtn">Save Assignment & Updates</button>
      </div>
    `;
  }

  // Maintenance Controls: detailed issue review, work log, completion summary, image upload
  let maintenanceControls = "";
  if (isMaintenance) {
    maintenanceControls = `
      <div class="admin-controls" style="margin-top:20px; border-left:4px solid #10b981; background:#fafffd;">
        <h4 style="color:#065f46;margin-bottom:12px;">🛠️ Technician Workstation & Resolution</h4>
        <div class="form-row">
          <div class="form-group">
            <label>Update Ticket Progress</label>
            <select id="maintStatusSelect">
              <option value="Assigned" ${c.status === "Assigned" ? "selected" : ""}>Assigned</option>
              <option value="In Progress" ${c.status === "In Progress" ? "selected" : ""}>In Progress (Work Underway)</option>
              <option value="Resolved" ${c.status === "Resolved" ? "selected" : ""}>Resolved (Issue Completed)</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>Diagnostic & Work Progress Log</label>
          <textarea id="maintWorkLog" placeholder="Document diagnostic findings, parts needed, or actions taken...">${escapeHtml(c.adminNotes || "")}</textarea>
        </div>
        <div class="form-group">
          <label><strong>Completion Summary (shown to student/faculty upon resolve):</strong></label>
          <textarea id="maintResolutionSummary" placeholder="Explain what was fixed, repaired, or replaced...">${escapeHtml(c.resolutionNotes || "")}</textarea>
        </div>
        <div class="form-group">
          <label>Add Completion Photo / Proof Image:</label>
          <input type="file" id="maintImageFile" accept="image/*" style="display:block;margin-bottom:8px;" />
          <input type="hidden" id="maintImageBase64" value="${c.resolutionImage || ""}" />
          <div id="imagePreviewContainer">
            ${c.resolutionImage ? `<img src="${c.resolutionImage}" class="resolution-preview-img" style="max-height:180px;" />` : ""}
          </div>
        </div>
        <button class="btn btn-primary btn-sm" id="saveMaintenanceBtn">Submit Work Update</button>
      </div>
    `;
  }

  const deleteBtn = (currentUser.role === "admin" || (c.filedBy === currentUser.id && c.status === "Pending"))
    ? `<button class="btn btn-danger btn-sm" id="deleteComplaintBtn" style="margin-top:14px;">Delete Complaint</button>`
    : "";

  return `
    <div style="margin-bottom:12px;">
      <span style="font-size:0.85rem;color:var(--text-muted);font-weight:600;">STATUS TRACKING</span>
      ${trackerHtml}
    </div>

    <div class="detail-row"><span>Complaint ID</span><span style="font-weight:700;">#${c.id}</span></div>
    <div class="detail-row"><span>Status</span><span class="badge ${statusBadgeClass(c.status)}">${c.status}</span></div>
    <div class="detail-row"><span>Priority</span><span class="badge ${priorityBadgeClass(c.priority)}">${c.priority}</span></div>
    <div class="detail-row"><span>Category</span><span class="cat-tag">${escapeHtml(c.category)}</span></div>
    ${c.location ? `<div class="detail-row"><span>Location / Venue</span><span style="font-weight:600;color:var(--purple-800);">📍 ${escapeHtml(c.location)}</span></div>` : ""}
    <div class="detail-row"><span>Filed by</span><span>${escapeHtml(filedName)} ${c.anonymous ? "(anonymous)" : ""}</span></div>
    <div class="detail-row"><span>Assigned Maintenance Staff</span><span style="font-weight:600;color:var(--purple-800);">${escapeHtml(assignedName)}</span></div>
    <div class="detail-row"><span>Filed Date</span><span>${createdDate}</span></div>
    <div class="detail-row"><span>Last Updated</span><span>${updatedDate}</span></div>

    <h4 style="margin-top:18px;color:var(--purple-900);font-size:0.95rem;">Issue Description</h4>
    <div class="detail-desc">${escapeHtml(c.description)}</div>

    ${c.problemImage ? `
      <div style="margin-top:14px;">
        <span style="font-size:0.85rem;color:var(--text-muted);font-weight:600;">ATTACHED PROBLEM PHOTO:</span>
        <img src="${c.problemImage}" class="resolution-preview-img" alt="Problem Photo" style="border:1.5px solid var(--purple-200);border-radius:8px;max-height:240px;margin-top:6px;display:block;" />
      </div>
    ` : ""}

    ${c.adminNotes && !isAdmin && !isMaintenance ? `
      <div style="background:var(--purple-50);padding:12px 16px;border-radius:10px;margin-top:14px;border-left:3px solid var(--purple-400);">
        <strong style="color:var(--purple-900);font-size:0.88rem;">Maintenance / Work Log:</strong>
        <p style="font-size:0.86rem;margin-top:4px;white-space:pre-wrap;">${escapeHtml(c.adminNotes)}</p>
      </div>
    ` : ""}

    ${resolutionBox}
    ${adminControls}
    ${maintenanceControls}
    ${deleteBtn}
  `;
}

async function attachModalHandlers(complaint, currentUser) {
  // 1. Admin Handlers
  if (currentUser.role === "admin") {
    const staffSelect = document.getElementById("assignStaffSelect");
    if (staffSelect) {
      try {
        const { staff } = await api.adminStaff();
        staffSelect.innerHTML = `<option value="">-- Unassigned --</option>` +
          (staff || []).map(s => `
            <option value="${s.id}" ${complaint.assignedTo === s.id ? "selected" : ""}>
              ${escapeHtml(s.name)} (${escapeHtml(s.department || s.role)}) [ID: ${s.id}]
            </option>
          `).join("");
      } catch (err) {
        console.error("Failed to load staff list", err);
      }
    }

    const saveAdminBtn = document.getElementById("saveAdminBtn");
    if (saveAdminBtn) {
      saveAdminBtn.addEventListener("click", async () => {
        saveAdminBtn.disabled = true;
        saveAdminBtn.textContent = "Saving...";
        try {
          const status = document.getElementById("statusSelect").value;
          const priority = document.getElementById("prioritySelect").value;
          const admin_notes = document.getElementById("adminNotes").value;
          const assigned_to = staffSelect ? (parseInt(staffSelect.value) || 0) : 0;

          await api.updateStatus(complaint.id, { status, priority, admin_notes, assigned_to });
          await refreshCurrentView();
          openComplaintModal(complaint.id, currentUser);
        } catch (err) {
          alert(err.message);
          saveAdminBtn.disabled = false;
          saveAdminBtn.textContent = "Save Assignment & Updates";
        }
      });
    }
  }

  // 2. Maintenance Staff Handlers
  if (currentUser.role === "maintenance") {
    const fileInput = document.getElementById("maintImageFile");
    const base64Input = document.getElementById("maintImageBase64");
    const previewContainer = document.getElementById("imagePreviewContainer");

    if (fileInput) {
      fileInput.addEventListener("change", (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.size > 2 * 1024 * 1024) {
          alert("Image size should be less than 2MB.");
          return;
        }
        const reader = new FileReader();
        reader.onload = (loadEvt) => {
          base64Input.value = loadEvt.target.result;
          previewContainer.innerHTML = `<img src="${loadEvt.target.result}" class="resolution-preview-img" style="max-height:180px;" />`;
        };
        reader.readAsDataURL(file);
      });
    }

    const saveMaintBtn = document.getElementById("saveMaintenanceBtn");
    if (saveMaintBtn) {
      saveMaintBtn.addEventListener("click", async () => {
        saveMaintBtn.disabled = true;
        saveMaintBtn.textContent = "Submitting...";
        try {
          const status = document.getElementById("maintStatusSelect").value;
          const admin_notes = document.getElementById("maintWorkLog").value;
          const resolution_notes = document.getElementById("maintResolutionSummary").value;
          const resolution_image = base64Input ? base64Input.value : null;

          await api.updateStatus(complaint.id, {
            status,
            admin_notes,
            resolution_notes,
            resolution_image
          });

          await refreshCurrentView();
          openComplaintModal(complaint.id, currentUser);
        } catch (err) {
          alert(err.message);
          saveMaintBtn.disabled = false;
          saveMaintBtn.textContent = "Submit Work Update";
        }
      });
    }
  }

  // 3. Delete Handler
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
