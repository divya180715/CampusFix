# Campus Fix — Smart Campus Complaint and Maintenance Management System

A full-stack, enterprise-ready Java Web application built with **Java Servlets (Jakarta EE 6.0)**, **JDBC**, **MySQL**, **HTML5/CSS3**, and **Apache Tomcat 10+**, managed through **Maven WAR packaging**.

---

## 🌟 Key Features

1. **Role-Based Access Control**:
   - **Student & Faculty**: Submit complaints, set urgency level, select campus category (Infrastructure, IT Services, Academic, etc.), submit anonymously, view live resolution updates.
   - **Maintenance Staff / Technicians**: View assigned repair tickets, log diagnostic/repair notes, update progress states (`Assigned` → `In Progress` → `Resolved`).
   - **Campus Administrator**: Complete operations dashboard, metric analytics by status and category, ticket assignment to technicians, priority triage, user directory management.

2. **Full End-to-End Workflow**:
   - Student files complaint → Admin prioritizes & assigns to technician → Technician logs progress & resolves → Student observes updated status and resolution log.

3. **Enterprise Testing & CI/CD**:
   - **JUnit 5** automated integration test covering the entire lifecycle with in-memory H2 database.
   - **Maven WAR** build producing production-ready `campus-fix.war`.
   - **Docker & Docker Compose** for one-command containerized deployment of MySQL 8 and Apache Tomcat 10.1.
   - **Jenkins Pipeline (`Jenkinsfile`)** with stages for Checkout, JUnit test execution, SonarQube static analysis, WAR packaging, and Docker build.
   - **SonarQube Configuration (`sonar-project.properties`)** for code quality scans.
   - **GitHub Actions CI (`.github/workflows/ci.yml`)** running on push and PRs.

---

## 🏗️ Project Architecture

```
cmpfix/
├── .github/workflows/ci.yml             # GitHub Actions CI workflow
├── .settings/                           # Eclipse WTP & JDT project facet settings
├── .classpath, .project                 # Native Eclipse IDE project files
├── sql/
│   └── schema.sql                       # MySQL schema & sample seed accounts
├── src/
│   ├── main/
│   │   ├── java/com/campusfix/
│   │   │   ├── dao/
│   │   │   │   ├── ComplaintDAO.java    # JDBC Complaint operations & metrics
│   │   │   │   └── UserDAO.java         # JDBC User registration & queries
│   │   │   ├── model/
│   │   │   │   ├── Complaint.java       # Complaint POJO
│   │   │   │   └── User.java            # User POJO
│   │   │   ├── servlet/
│   │   │   │   ├── BaseServlet.java     # JSON helper & error handler
│   │   │   │   ├── AuthServlet.java     # /api/auth (Login, Register, Logout, Me)
│   │   │   │   ├── ComplaintServlet.java# /api/complaints (CRUD, status, assign)
│   │   │   │   └── AdminServlet.java    # /api/admin (Stats, users, staff)
│   │   │   └── util/
│   │   │       ├── DBUtil.java          # JDBC Connection Manager (MySQL/H2)
│   │   │       └── PasswordUtil.java    # BCrypt password hashing
│   │   ├── resources/
│   │   │   └── db.properties            # Database connection configuration
│   │   └── webapp/
│   │       ├── WEB-INF/web.xml          # Servlet container deployment descriptor
│   │       ├── css/style.css            # Responsive UI stylesheet
│   │       ├── js/
│   │       │   ├── api.js               # Frontend API client
│   │       │   └── complaint-ui.js      # Modal, lists, and badge rendering
│   │       ├── pages/
│   │       │   ├── login.html           # Authentication page
│   │       │   ├── register.html        # User registration (Student/Faculty/Staff)
│   │       │   ├── student-dashboard.html # Student issue submission & tracking
│   │       │   ├── faculty-dashboard.html # Faculty issue submission & tracking
│   │       │   └── admin-dashboard.html   # Admin triage & maintenance portal
│   │       └── index.html               # Landing page
│   └── test/java/com/campusfix/
│       ├── dao/CampusFixWorkflowTest.java # JUnit 5 End-to-End Workflow test
│       └── model/UserTest.java          # JUnit 5 POJO tests
├── Dockerfile                           # Multi-stage Maven + Tomcat 10.1 image
├── docker-compose.yml                   # MySQL 8 + Tomcat 10.1 composition
├── Jenkinsfile                          # Jenkins declarative CI/CD pipeline
├── sonar-project.properties             # SonarQube quality analysis
└── pom.xml                              # Maven WAR build configuration
```

---

## 🚀 Getting Started

### 1. Database Setup (MySQL)
Run the script in [sql/schema.sql](file:///c:/Users/divya/Downloads/cmpfix/sql/schema.sql) in MySQL Workbench, phpMyAdmin, or mysql CLI:
```sql
mysql -u root -p < sql/schema.sql
```

Default credentials seeded:
- **Admin**: `admin@campusfix.edu` / `admin123`
- **Maintenance Technician**: `tech@campusfix.edu` / `admin123`
- **Student**: `student@campusfix.edu` / `admin123`

### 2. Importing into Eclipse IDE
1. Open Eclipse IDE (for Enterprise Java and Web Developers).
2. Click **File** → **Import...** → **Maven** → **Existing Maven Projects**.
3. Browse to `c:\Users\divya\Downloads\cmpfix` and click **Finish**.
4. In the **Servers** tab, right-click → **New** → **Server** → select **Apache Tomcat v10.1** (or Tomcat 10.0).
5. Add `campus-fix` to the configured resources and click **Start**.
6. Access the application in your browser at: `http://localhost:8080/campus-fix/`

### 3. Command Line Build & Testing
Run tests:
```powershell
.\mvnw.cmd clean test
```
Build production WAR package:
```powershell
.\mvnw.cmd package -DskipTests
```
The resulting deployable WAR will be located at `target/campus-fix.war`.

### 4. Running with Docker Compose
To run both MySQL 8 and Apache Tomcat 10.1 with zero manual software installation:
```bash
docker compose up --build
```
Then visit `http://localhost:8080/`.

---

## 🧪 Testing and Verification Results
- **Framework**: JUnit 5 (Jupiter 5.10.2).
- **Execution**: 5 tests executed, 0 failures, 0 errors.
- **Workflow Tested**:
  1. Student registration & password BCrypt hashing.
  2. Student complaint submission (`Pending`).
  3. Admin assignment of technician (`Assigned`).
  4. Technician work progression (`In Progress` → `Resolved`).
  5. Student verification of resolution notes and timestamps.
