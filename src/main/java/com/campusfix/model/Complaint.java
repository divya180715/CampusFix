package com.campusfix.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Complaint implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String location;
    private String description;
    private String category;
    private String priority; // Low, Medium, High, Urgent
    private String status;   // Pending, Assigned, In Progress, Resolved, Rejected
    private boolean isAnonymous;
    private int filedBy;
    private String filedByRole;
    private String filedByName;      // Joined display helper
    private Integer assignedTo;       // Nullable technician/admin user ID
    private String assignedToName;   // Joined display helper
    private String adminNotes;
    private String resolutionNotes;
    private String resolutionImage;
    private String problemImage;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp resolvedAt;

    public Complaint() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public int getFiledBy() { return filedBy; }
    public void setFiledBy(int filedBy) { this.filedBy = filedBy; }

    public String getFiledByRole() { return filedByRole; }
    public void setFiledByRole(String filedByRole) { this.filedByRole = filedByRole; }

    public String getFiledByName() { return filedByName; }
    public void setFiledByName(String filedByName) { this.filedByName = filedByName; }

    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolutionImage() { return resolutionImage; }
    public void setResolutionImage(String resolutionImage) { this.resolutionImage = resolutionImage; }

    public String getProblemImage() { return problemImage; }
    public void setProblemImage(String problemImage) { this.problemImage = problemImage; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
}
