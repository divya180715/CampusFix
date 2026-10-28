# UniResolve — University Complaint Management System

A full-stack complaint management system for universities, supporting **students**, **faculty**, and **admin** roles. Purple-themed UI, built with vanilla HTML/CSS/JS on the frontend and Python (Flask) + SQLite on the backend.

## Features

- **Student & Faculty**: register/login, file complaints (with category, priority, optional anonymity), track status, comment on their own complaints.
- **Admin**: dashboard with stats (totals by status/category), view & filter all complaints, assign complaints to staff, update status (Pending → In Progress → Resolved/Rejected), add internal notes, view all registered users.
- Session-based authentication (cookies), password hashing (werkzeug).
- SQLite database — zero setup, file-based (`complaints.db` is auto-created).

## Project Structure

```
complaint-system/
├── backend/
│   ├── app.py              # Flask app: routes, models, auth
│   └── requirements.txt
└── frontend/
    ├── index.html           # Landing page
    ├── css/style.css        # Purple theme
    ├── js/
    │   ├── api.js           # API client + auth helpers
    │   └── complaint-ui.js  # Shared complaint list/modal rendering
    └── pages/
        ├── login.html
        ├── register.html
        ├── student-dashboard.html
        ├── faculty-dashboard.html
        └── admin-dashboard.html
```

## Setup

### 1. Backend (Flask API)

```bash
cd backend
pip install -r requirements.txt
python app.py
```

The API runs at `http://localhost:5000`. The SQLite database (`complaints.db`) and a default admin account are created automatically on first run:

- **Admin email:** `admin@university.edu`
- **Admin password:** `admin123`

> Change or remove this seeded account before deploying anywhere real.

### 2. Frontend

The frontend is plain HTML/CSS/JS — no build step needed. Just serve the `frontend` folder with any static server, e.g.:

```bash
cd frontend
python -m http.server 8080
```

Then open `http://localhost:8080` in your browser.

> The frontend calls the API at `http://localhost:5000/api` (see `API_BASE` in `frontend/js/api.js`). Update this if you deploy the backend elsewhere.

## Roles & Flow

1. **Student / Faculty** — register an account, choose the correct role toggle, then log in. From their dashboard they can file a new complaint and track ones they've already filed, including chatting via comments.
2. **Admin** — log in with the seeded admin account (or promote a user manually in the database). From the admin dashboard: see overview stats, browse/filter/search every complaint, open a complaint to change its status, assign it to a staff member (by user ID, visible on the Users tab), add internal notes, and reply in the comment thread.

## Notes on the tech stack

- Backend: **Python 3 + Flask**, session cookies for auth, **SQLite** for storage (no external DB server needed). Swap in Postgres/MySQL later by changing the `sqlite3` calls in `app.py` if you outgrow SQLite.
- Frontend: plain **HTML/CSS/JS**, no frameworks or build tools — easy to host on GitHub Pages, Netlify, or any static host (with the backend hosted separately, e.g. Render/Railway).
- CORS is enabled with credentials so the two can run on different ports/origins during development.
