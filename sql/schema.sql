-- =====================================================================
-- Campus Fix - Smart Campus Complaint and Maintenance Management System
-- Database Schema & Sample Initial Seed Data
-- =====================================================================

CREATE DATABASE IF NOT EXISTS campusfix_db;
USE campusfix_db;

-- 1. Users Table (Students, Faculty, Maintenance Staff, Admin)
DROP TABLE IF EXISTS complaints;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('student', 'faculty', 'maintenance', 'admin') NOT NULL,
    department VARCHAR(100),
    id_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Complaints Table
CREATE TABLE complaints (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    location VARCHAR(200) NULL,
    description TEXT NOT NULL,
    category VARCHAR(60) NOT NULL,
    priority ENUM('Low', 'Medium', 'High', 'Urgent') NOT NULL DEFAULT 'Medium',
    status ENUM('Pending', 'Assigned', 'In Progress', 'Resolved', 'Rejected') NOT NULL DEFAULT 'Pending',
    is_anonymous TINYINT(1) NOT NULL DEFAULT 0,
    filed_by INT NOT NULL,
    filed_by_role VARCHAR(20) NOT NULL,
    assigned_to INT NULL,
    admin_notes TEXT NULL,
    resolution_notes TEXT NULL,
    resolution_image MEDIUMTEXT NULL,
    problem_image MEDIUMTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_complaints_filed_by FOREIGN KEY (filed_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Sample Accounts
-- Passwords are set using standard BCrypt hash for "admin123" ($2a$10$w8T.N0zP9v0iLp2H5bY/Te5B5GZ0bOQ3F0tT0Lz9sUe6H8QdY2DTC)
-- Also compatible with plain fallback "admin123" handled by DBUtil/BCrypt helper
INSERT INTO users (id, name, email, password_hash, role, department, id_number) VALUES
(1, 'System Administrator', 'admin@campusfix.edu', '$2a$10$7qN/z.3C.o09hA0UuL5wO.V.HqSZZj9F3v.iT4L7cZ5O3A9I9h2G2', 'admin', 'Administration', 'ADM001'),
(2, 'John Doe (Student)', 'student@campusfix.edu', '$2a$10$7qN/z.3C.o09hA0UuL5wO.V.HqSZZj9F3v.iT4L7cZ5O3A9I9h2G2', 'student', 'Computer Science', 'STU202601'),
(3, 'Alex Smith (Technician)', 'tech@campusfix.edu', '$2a$10$7qN/z.3C.o09hA0UuL5wO.V.HqSZZj9F3v.iT4L7cZ5O3A9I9h2G2', 'maintenance', 'Facilities & Maintenance', 'MNT101'),
(4, 'Dr. Robert Brown', 'faculty@campusfix.edu', '$2a$10$7qN/z.3C.o09hA0UuL5wO.V.HqSZZj9F3v.iT4L7cZ5O3A9I9h2G2', 'faculty', 'Electrical Engineering', 'FAC502');

-- Sample Complaints
INSERT INTO complaints (id, title, location, description, category, priority, status, is_anonymous, filed_by, filed_by_role, assigned_to, admin_notes) VALUES
(1, 'Broken AC in Lab 304', 'CS Building, 3rd Floor, Lab 304', 'Air conditioner in CS Lab 304 is leaking water and not cooling.', 'Infrastructure', 'High', 'Pending', 0, 2, 'student', NULL, NULL),
(2, 'Projector flickering in Hall A', 'Main Auditorium, Hall A', 'The HDMI projector display cuts out every 2 minutes during lectures.', 'IT Services', 'Urgent', 'Assigned', 0, 4, 'faculty', 3, 'Assigned to Alex for hardware inspection'),
(3, 'Library WiFi disconnecting', 'Central Library, 2nd Floor Reading Room', 'WiFi access point in 2nd floor library reading room drops connections.', 'IT Services', 'Medium', 'In Progress', 1, 2, 'student', 3, 'Replacing wireless switch port');
