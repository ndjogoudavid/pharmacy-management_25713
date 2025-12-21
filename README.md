# Pharmacy Management (Refactored)

This repository contains a Spring Boot backend and a React + Vite frontend for the Pharmacy Management System (Final Project - Web Technology Semester 1 2025/2026).

Quick start (development):

- Backend
  - Copy `backend/.env.example` to `backend/.env` and set values.
  - To preload demo data (admin/pharmacist/patient + sample meds/prescriptions), set `APP_SEED_DEMO=true` (and optional `APP_SEED_DEMO_PASSWORD`) in your env before starting.
  - Create `backend/application-secrets.properties` from `backend/application-secrets.properties.example` and add your real secrets (DB password, mail password, JWT secret). This file is ignored by git.
  - Run the backend:
```
cd backend
./mvnw.cmd spring-boot:run   (Windows)
./mvnw spring-boot:run       (Mac/Linux)
```

- Frontend
  - Copy `frontend/.env.example` to `frontend/.env` if needed.
  - Install deps and start dev server:
```
cd frontend
npm install
npm run dev
```

Environment variables are documented in `backend/.env.example` and `frontend/.env.example`. The backend will use environment values when present and fall back to defaults in `application.properties`.

Default roles: `ADMIN`, `PHARMACIST`, `PATIENT`.
