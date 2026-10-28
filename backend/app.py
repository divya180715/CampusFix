import sqlite3
import os
from datetime import datetime
from functools import wraps
from flask import Flask, request, jsonify, session, g
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "complaints.db")

app = Flask(__name__)
app.secret_key = "university-complaint-system-secret-key-change-in-production"
CORS(app, supports_credentials=True)

CATEGORIES = [
    "Academic", "Hostel/Accommodation", "Infrastructure", "Faculty Conduct",
    "Administration", "Library", "IT Services", "Harassment/Ragging",
    "Fees/Finance", "Examination", "Other"
]

STATUSES = ["Pending", "In Progress", "Resolved", "Rejected"]


# ---------------------------------------------------------------------------
# Database helpers
# ---------------------------------------------------------------------------
def get_db():
    if "db" not in g:
        g.db = sqlite3.connect(DB_PATH)
        g.db.row_factory = sqlite3.Row
        g.db.execute("PRAGMA foreign_keys = ON")
    return g.db


@app.teardown_appcontext
def close_db(exception=None):
    db = g.pop("db", None)
    if db is not None:
        db.close()


def init_db():
    first_time = not os.path.exists(DB_PATH)
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL CHECK(role IN ('student','faculty','admin')),
            department TEXT,
            id_number TEXT,
            created_at TEXT DEFAULT (datetime('now'))
        )
    """)

    cur.execute("""
        CREATE TABLE IF NOT EXISTS complaints (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            category TEXT NOT NULL,
            priority TEXT NOT NULL DEFAULT 'Medium',
            status TEXT NOT NULL DEFAULT 'Pending',
            is_anonymous INTEGER NOT NULL DEFAULT 0,
            filed_by INTEGER,
            filed_by_role TEXT,
            assigned_to INTEGER,
            admin_notes TEXT,
            created_at TEXT DEFAULT (datetime('now')),
            updated_at TEXT DEFAULT (datetime('now')),
            resolved_at TEXT,
            FOREIGN KEY (filed_by) REFERENCES users(id),
            FOREIGN KEY (assigned_to) REFERENCES users(id)
        )
    """)

    cur.execute("""
        CREATE TABLE IF NOT EXISTS complaint_comments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            complaint_id INTEGER NOT NULL,
            user_id INTEGER NOT NULL,
            user_name TEXT NOT NULL,
            user_role TEXT NOT NULL,
            comment TEXT NOT NULL,
            created_at TEXT DEFAULT (datetime('now')),
            FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
    """)

    # Seed a default admin account on first run
    if first_time:
        admin_hash = generate_password_hash("admin123")
        cur.execute(
            "INSERT INTO users (name, email, password_hash, role, department) VALUES (?,?,?,?,?)",
            ("System Admin", "admin@university.edu", admin_hash, "admin", "Administration")
        )
        conn.commit()
        print("Seeded default admin -> email: admin@university.edu | password: admin123")

    conn.commit()
    conn.close()


# ---------------------------------------------------------------------------
# Auth helpers
# ---------------------------------------------------------------------------
def login_required(roles=None):
    def decorator(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            if "user_id" not in session:
                return jsonify({"error": "Authentication required"}), 401
            if roles and session.get("role") not in roles:
                return jsonify({"error": "Not authorized for this action"}), 403
            return f(*args, **kwargs)
        return wrapper
    return decorator


def current_user_row():
    db = get_db()
    return db.execute("SELECT * FROM users WHERE id = ?", (session["user_id"],)).fetchone()


def row_to_dict(row):
    return dict(row) if row else None


# ---------------------------------------------------------------------------
# Auth routes
# ---------------------------------------------------------------------------
@app.route("/api/auth/register", methods=["POST"])
def register():
    data = request.get_json(force=True) or {}
    name = (data.get("name") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""
    role = data.get("role") or ""
    department = (data.get("department") or "").strip()
    id_number = (data.get("id_number") or "").strip()

    if role not in ("student", "faculty"):
        return jsonify({"error": "Role must be student or faculty for self-registration"}), 400
    if not name or not email or not password:
        return jsonify({"error": "Name, email and password are required"}), 400
    if len(password) < 6:
        return jsonify({"error": "Password must be at least 6 characters"}), 400

    db = get_db()
    existing = db.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()
    if existing:
        return jsonify({"error": "An account with this email already exists"}), 409

    pw_hash = generate_password_hash(password)
    cur = db.execute(
        "INSERT INTO users (name, email, password_hash, role, department, id_number) VALUES (?,?,?,?,?,?)",
        (name, email, pw_hash, role, department, id_number)
    )
    db.commit()

    session["user_id"] = cur.lastrowid
    session["role"] = role
    session["name"] = name

    return jsonify({"message": "Registered successfully", "user": {
        "id": cur.lastrowid, "name": name, "email": email, "role": role, "department": department
    }}), 201


@app.route("/api/auth/login", methods=["POST"])
def login():
    data = request.get_json(force=True) or {}
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""

    db = get_db()
    user = db.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
    if not user or not check_password_hash(user["password_hash"], password):
        return jsonify({"error": "Invalid email or password"}), 401

    session["user_id"] = user["id"]
    session["role"] = user["role"]
    session["name"] = user["name"]

    return jsonify({"message": "Login successful", "user": {
        "id": user["id"], "name": user["name"], "email": user["email"],
        "role": user["role"], "department": user["department"]
    }})


@app.route("/api/auth/logout", methods=["POST"])
def logout():
    session.clear()
    return jsonify({"message": "Logged out"})


@app.route("/api/auth/me", methods=["GET"])
def me():
    if "user_id" not in session:
        return jsonify({"user": None})
    user = current_user_row()
    if not user:
        session.clear()
        return jsonify({"user": None})
    return jsonify({"user": {
        "id": user["id"], "name": user["name"], "email": user["email"],
        "role": user["role"], "department": user["department"]
    }})


# ---------------------------------------------------------------------------
# Meta routes
# ---------------------------------------------------------------------------
@app.route("/api/meta", methods=["GET"])
def meta():
    return jsonify({"categories": CATEGORIES, "statuses": STATUSES})


# ---------------------------------------------------------------------------
# Complaint routes
# ---------------------------------------------------------------------------
@app.route("/api/complaints", methods=["POST"])
@login_required(roles=["student", "faculty"])
def create_complaint():
    data = request.get_json(force=True) or {}
    title = (data.get("title") or "").strip()
    description = (data.get("description") or "").strip()
    category = data.get("category") or "Other"
    priority = data.get("priority") or "Medium"
    is_anonymous = 1 if data.get("is_anonymous") else 0

    if not title or not description:
        return jsonify({"error": "Title and description are required"}), 400
    if category not in CATEGORIES:
        category = "Other"
    if priority not in ("Low", "Medium", "High", "Urgent"):
        priority = "Medium"

    db = get_db()
    cur = db.execute(
        """INSERT INTO complaints (title, description, category, priority, status, is_anonymous, filed_by, filed_by_role)
           VALUES (?,?,?,?, 'Pending', ?,?,?)""",
        (title, description, category, priority, is_anonymous, session["user_id"], session["role"])
    )
    db.commit()
    complaint = db.execute("SELECT * FROM complaints WHERE id = ?", (cur.lastrowid,)).fetchone()
    return jsonify({"message": "Complaint filed successfully", "complaint": row_to_dict(complaint)}), 201


@app.route("/api/complaints", methods=["GET"])
@login_required()
def list_complaints():
    db = get_db()
    role = session["role"]

    status_filter = request.args.get("status")
    category_filter = request.args.get("category")
    search = request.args.get("search")

    query = """
        SELECT c.*, 
               CASE WHEN c.is_anonymous = 1 THEN 'Anonymous' ELSE u.name END AS filed_by_name,
               a.name AS assigned_to_name
        FROM complaints c
        LEFT JOIN users u ON c.filed_by = u.id
        LEFT JOIN users a ON c.assigned_to = a.id
        WHERE 1=1
    """
    params = []

    if role in ("student", "faculty"):
        query += " AND c.filed_by = ?"
        params.append(session["user_id"])
    # admin sees everything

    if status_filter and status_filter != "All":
        query += " AND c.status = ?"
        params.append(status_filter)
    if category_filter and category_filter != "All":
        query += " AND c.category = ?"
        params.append(category_filter)
    if search:
        query += " AND (c.title LIKE ? OR c.description LIKE ?)"
        like = f"%{search}%"
        params.extend([like, like])

    query += " ORDER BY c.created_at DESC"
    rows = db.execute(query, params).fetchall()
    return jsonify({"complaints": [row_to_dict(r) for r in rows]})


@app.route("/api/complaints/<int:complaint_id>", methods=["GET"])
@login_required()
def get_complaint(complaint_id):
    db = get_db()
    row = db.execute(
        """SELECT c.*, 
               CASE WHEN c.is_anonymous = 1 THEN 'Anonymous' ELSE u.name END AS filed_by_name,
               a.name AS assigned_to_name
           FROM complaints c
           LEFT JOIN users u ON c.filed_by = u.id
           LEFT JOIN users a ON c.assigned_to = a.id
           WHERE c.id = ?""",
        (complaint_id,)
    ).fetchone()

    if not row:
        return jsonify({"error": "Complaint not found"}), 404

    if session["role"] != "admin" and row["filed_by"] != session["user_id"]:
        return jsonify({"error": "Not authorized to view this complaint"}), 403

    comments = db.execute(
        "SELECT * FROM complaint_comments WHERE complaint_id = ? ORDER BY created_at ASC",
        (complaint_id,)
    ).fetchall()

    result = row_to_dict(row)
    result["comments"] = [row_to_dict(c) for c in comments]
    return jsonify({"complaint": result})


@app.route("/api/complaints/<int:complaint_id>/status", methods=["PATCH"])
@login_required(roles=["admin"])
def update_status(complaint_id):
    data = request.get_json(force=True) or {}
    status = data.get("status")
    admin_notes = data.get("admin_notes")
    assigned_to = data.get("assigned_to")

    if status and status not in STATUSES:
        return jsonify({"error": "Invalid status"}), 400

    db = get_db()
    row = db.execute("SELECT * FROM complaints WHERE id = ?", (complaint_id,)).fetchone()
    if not row:
        return jsonify({"error": "Complaint not found"}), 404

    fields, params = [], []
    if status:
        fields.append("status = ?")
        params.append(status)
        if status == "Resolved":
            fields.append("resolved_at = datetime('now')")
    if admin_notes is not None:
        fields.append("admin_notes = ?")
        params.append(admin_notes)
    if assigned_to is not None:
        fields.append("assigned_to = ?")
        params.append(assigned_to)

    fields.append("updated_at = datetime('now')")
    params.append(complaint_id)

    db.execute(f"UPDATE complaints SET {', '.join(fields)} WHERE id = ?", params)
    db.commit()

    updated = db.execute("SELECT * FROM complaints WHERE id = ?", (complaint_id,)).fetchone()
    return jsonify({"message": "Complaint updated", "complaint": row_to_dict(updated)})


@app.route("/api/complaints/<int:complaint_id>/comments", methods=["POST"])
@login_required()
def add_comment(complaint_id):
    data = request.get_json(force=True) or {}
    comment = (data.get("comment") or "").strip()
    if not comment:
        return jsonify({"error": "Comment cannot be empty"}), 400

    db = get_db()
    row = db.execute("SELECT * FROM complaints WHERE id = ?", (complaint_id,)).fetchone()
    if not row:
        return jsonify({"error": "Complaint not found"}), 404
    if session["role"] != "admin" and row["filed_by"] != session["user_id"]:
        return jsonify({"error": "Not authorized"}), 403

    user = current_user_row()
    db.execute(
        "INSERT INTO complaint_comments (complaint_id, user_id, user_name, user_role, comment) VALUES (?,?,?,?,?)",
        (complaint_id, user["id"], user["name"], user["role"], comment)
    )
    db.execute("UPDATE complaints SET updated_at = datetime('now') WHERE id = ?", (complaint_id,))
    db.commit()
    return jsonify({"message": "Comment added"}), 201


@app.route("/api/complaints/<int:complaint_id>", methods=["DELETE"])
@login_required()
def delete_complaint(complaint_id):
    db = get_db()
    row = db.execute("SELECT * FROM complaints WHERE id = ?", (complaint_id,)).fetchone()
    if not row:
        return jsonify({"error": "Complaint not found"}), 404

    if session["role"] == "admin":
        pass
    elif row["filed_by"] == session["user_id"] and row["status"] == "Pending":
        pass
    else:
        return jsonify({"error": "You can only delete your own complaints while they are still Pending"}), 403

    db.execute("DELETE FROM complaints WHERE id = ?", (complaint_id,))
    db.commit()
    return jsonify({"message": "Complaint deleted"})


# ---------------------------------------------------------------------------
# Admin routes
# ---------------------------------------------------------------------------
@app.route("/api/admin/stats", methods=["GET"])
@login_required(roles=["admin"])
def admin_stats():
    db = get_db()
    total = db.execute("SELECT COUNT(*) c FROM complaints").fetchone()["c"]
    by_status = db.execute("SELECT status, COUNT(*) c FROM complaints GROUP BY status").fetchall()
    by_category = db.execute("SELECT category, COUNT(*) c FROM complaints GROUP BY category").fetchall()
    by_role = db.execute("SELECT filed_by_role, COUNT(*) c FROM complaints GROUP BY filed_by_role").fetchall()
    recent = db.execute(
        """SELECT c.*, CASE WHEN c.is_anonymous=1 THEN 'Anonymous' ELSE u.name END AS filed_by_name
           FROM complaints c LEFT JOIN users u ON c.filed_by = u.id
           ORDER BY c.created_at DESC LIMIT 5"""
    ).fetchall()

    return jsonify({
        "total": total,
        "by_status": {r["status"]: r["c"] for r in by_status},
        "by_category": {r["category"]: r["c"] for r in by_category},
        "by_role": {r["filed_by_role"]: r["c"] for r in by_role},
        "recent": [row_to_dict(r) for r in recent]
    })


@app.route("/api/admin/users", methods=["GET"])
@login_required(roles=["admin"])
def admin_users():
    db = get_db()
    rows = db.execute(
        "SELECT id, name, email, role, department, id_number, created_at FROM users ORDER BY created_at DESC"
    ).fetchall()
    return jsonify({"users": [row_to_dict(r) for r in rows]})


if __name__ == "__main__":
    init_db()
    app.run(debug=True, port=5000)
