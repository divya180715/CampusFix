package com.campusfix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    @DisplayName("User POJO getters and setters should function correctly")
    void testUserProperties() {
        User user = new User();
        user.setId(10);
        user.setName("John Student");
        user.setEmail("student@campusfix.edu");
        user.setRole("student");
        user.setDepartment("Computer Science");
        user.setIdNumber("CS2026");

        assertEquals(10, user.getId());
        assertEquals("John Student", user.getName());
        assertEquals("student@campusfix.edu", user.getEmail());
        assertEquals("student", user.getRole());
        assertEquals("Computer Science", user.getDepartment());
        assertEquals("CS2026", user.getIdNumber());
    }

    @Test
    @DisplayName("Complaint POJO properties and defaults")
    void testComplaintProperties() {
        Complaint complaint = new Complaint();
        complaint.setId(101);
        complaint.setTitle("Leaking Pipe");
        complaint.setDescription("Pipe leaking in Restroom Block B");
        complaint.setCategory("Infrastructure");
        complaint.setPriority("Urgent");
        complaint.setStatus("Pending");
        complaint.setAnonymous(true);
        complaint.setFiledBy(2);
        complaint.setFiledByRole("student");

        assertEquals(101, complaint.getId());
        assertEquals("Leaking Pipe", complaint.getTitle());
        assertTrue(complaint.isAnonymous());
        assertEquals("Urgent", complaint.getPriority());
        assertEquals("Pending", complaint.getStatus());
        assertEquals(2, complaint.getFiledBy());
    }
}
