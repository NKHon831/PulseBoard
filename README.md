# PulseBoard

## Structure
- `frontend/` — React + TypeScript app, deployed on Vercel
- `backend/` — Spring Boot app (modular monolith, organized by domain package under `com.pulseboard.*`), deployed on Render/Fly.io
- Database — Supabase (Postgres)

## Domains (backend/src/main/java/com/pulseboard/)
- `user/` — user-related features
- `core/` — core app features (rename/split as the product takes shape)
- `common/` — shared config, DTOs, exceptions

Each domain package should stay decoupled (own controller/service/repository, no cross-package DB access) so it can be extracted into a standalone microservice later without rework.
