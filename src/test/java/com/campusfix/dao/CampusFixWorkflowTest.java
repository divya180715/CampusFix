package com.campusfix.dao;

import com.campusfix.model.Complaint;
import com.campusfix.model.User;
import com.campusfix.util.DBUtil;
import com.campusfix.util.PasswordUtil;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CampusFixWorkflowTest {

    private static UserDAO userDAO;
    private static ComplaintDAO complaintDAO;

    @BeforeAll
    static void setupDatabase() throws Exception {
        // Configure in-memory H2 database for independent JUnit 5 testing
        String h2Url = "jdbc:h2:mem:campusfix_test;DB_CLOSE_DELAY=-1;MODE=MySQL";
        DBUtil.setCustomConnection("org.h2.Driver", h2Url, "sa", "");

        userDAO = new UserDAO();
        complaintDAO = new ComplaintDAO();

        // Initialize table schema in H2
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS complaints");
            stmt.execute("DROP TABLE IF EXISTS users");

            stmt.execute("CREATE TABLE users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(120) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "department VARCHAR(100), " +
                    "id_number VARCHAR(50), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE complaints (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(200) NOT NULL, " +
                    "location VARCHAR(200) NULL, " +
                    "description TEXT NOT NULL, " +
                    "category VARCHAR(60) NOT NULL, " +
                    "priority VARCHAR(20) NOT NULL DEFAULT 'Medium', " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'Pending', " +
                    "is_anonymous INT NOT NULL DEFAULT 0, " +
                    "filed_by INT NOT NULL, " +
                    "filed_by_role VARCHAR(20) NOT NULL, " +
                    "assigned_to INT NULL, " +
                    "admin_notes TEXT NULL, " +
                    "resolution_notes TEXT NULL, " +
                    "resolution_image TEXT NULL, " +
                    "problem_image TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "resolved_at TIMESTAMP NULL, " +
                    "FOREIGN KEY (filed_by) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Password hashing & BCrypt verification")
    void testPasswordSecurity() {
        String plain = "SecretPass123";
        String hash = PasswordUtil.hashPassword(plain);

        assertNotNull(hash);
        assertNotEquals(plain, hash);
        assertTrue(PasswordUtil.checkPassword(plain, hash));
        assertFalse(PasswordUtil.checkPassword("WrongPass", hash));
    }

    @Test
    @Order(2)
    @DisplayName("User Registration & Retrieval for Student, Admin, and Maintenance Technician")
    void testUserRegistration() {
        User admin = new User(0, "Campus Admin", "admin@test.edu", null, "admin", "Admin Office", "ADM01");
        assertTrue(userDAO.registerUser(admin, "admin123"));

        User student = new User(0, "Alice Student", "alice@test.edu", null, "student", "Civil Eng", "CE202");
        assertTrue(userDAO.registerUser(student, "student123"));

        User technician = new User(0, "Bob Electrician", "bob@test.edu", null, "maintenance", "Electrical Maintenance", "TECH01");
        assertTrue(userDAO.registerUser(technician, "tech123"));

        User foundStudent = userDAO.findByEmail("alice@test.edu");
        assertNotNull(foundStudent);
        assertEquals("Alice Student", foundStudent.getName());
        assertEquals("student", foundStudent.getRole());

        List<User> staff = userDAO.listStaffAndAdmins();
        assertTrue(staff.size() >= 2);
    }

    @Test
    @Order(3)
    @DisplayName("Complete Workflow: Student submits complaint -> Admin assigns it -> Maintenance staff resolves it -> Student verifies resolved status")
    void testEndToEndWorkflow() {
        User student = userDAO.findByEmail("alice@test.edu");
        User technician = userDAO.findByEmail("bob@test.edu");

        assertNotNull(student, "Student user must exist");
        assertNotNull(technician, "Technician user must exist");

        // 1. Student submits complaint
        Complaint c = new Complaint();
        c.setTitle("Broken Light in Hall B");
        c.setLocation("Main Science Block, Hall B, 1st Floor");
        c.setDescription("Ceiling fluorescent fixture sparking and not turning on.");
        c.setCategory("Infrastructure");
        c.setPriority("Urgent");
        c.setAnonymous(false);
        c.setFiledBy(student.getId());
        c.setFiledByRole(student.getRole());

        boolean submitted = complaintDAO.createComplaint(c);
        assertTrue(submitted, "Student complaint submission should succeed");
        assertTrue(c.getId() > 0, "Generated complaint ID must be set");

        // Check complaint initial status is 'Pending'
        Complaint saved = complaintDAO.getComplaintById(c.getId());
        assertNotNull(saved);
        assertEquals("Pending", saved.getStatus());
        assertEquals("Broken Light in Hall B", saved.getTitle());
        assertEquals("Main Science Block, Hall B, 1st Floor", saved.getLocation());

        // 2. Admin assigns complaint to technician
        boolean assigned = complaintDAO.updateStatusAndAssignment(
                c.getId(),
                "Assigned",
                technician.getId(),
                "Assigned to Bob for ballast replacement",
                "Urgent"
        );
        assertTrue(assigned, "Admin should be able to assign complaint to technician");

        Complaint assignedComplaint = complaintDAO.getComplaintById(c.getId());
        assertEquals("Assigned", assignedComplaint.getStatus());
        assertEquals(technician.getId(), assignedComplaint.getAssignedTo());

        // 3. Maintenance technician updates status to 'In Progress' and then 'Resolved'
        boolean inProgress = complaintDAO.updateStatusAndAssignment(
                c.getId(),
                "In Progress",
                technician.getId(),
                "Parts ordered; rewiring scheduled today",
                null
        );
        assertTrue(inProgress);

        boolean resolved = complaintDAO.updateStatusAndAssignment(
                c.getId(),
                "Resolved",
                technician.getId(),
                "Inspection complete",
                null,
                "Ballast replaced and fixture tested successfully. All lights operational.",
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        );
        assertTrue(resolved);

        // 4. Student sees the resolved status
        Complaint resolvedComplaint = complaintDAO.getComplaintById(c.getId());
        assertEquals("Resolved", resolvedComplaint.getStatus());
        assertNotNull(resolvedComplaint.getResolvedAt(), "Resolved timestamp should be recorded");
        assertEquals("Ballast replaced and fixture tested successfully. All lights operational.", resolvedComplaint.getResolutionNotes());
        assertNotNull(resolvedComplaint.getResolutionImage());

        // Verify dashboard statistics update
        Map<String, Object> stats = complaintDAO.getDashboardStats();
        assertTrue((int) stats.get("total") >= 1);
    }
}
