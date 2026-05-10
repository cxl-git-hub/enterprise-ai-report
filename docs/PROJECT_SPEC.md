# Enterprise AI Automated Reporting Platform - Project Specification

## System Overview
Turn enterprise data → KPI system → AI analysis → workflow execution → automated reports.

## Tech Stack
- **Backend**: Java 17, Spring Boot 3, Spring Security + JWT, MyBatis Plus, Redis, MySQL
- **AI Service**: Python 3.10+, FastAPI, LangGraph
- **Frontend**: Vue 3, Vite, Pinia, Ant Design Vue
- **Storage**: MySQL, Redis, MinIO
- **Deploy**: Docker Compose

## Core Modules
1. Auth + RBAC + Tenant System
2. DataHub (Excel/DB/API Ingestion)
3. Config Center (Schema/KPI/Workflow/Prompt/Template)
4. KPI Engine (DSL-based)
5. Workflow Engine (DAG Execution)
6. AI Service (NL2SQL + Analysis + Report)
7. Output Engine (Word/PPT/PDF)
8. Frontend Admin System

## Commercial Extension Layers
1. **Config Consistency Engine** - Version binding, dependency graph, validation, rollback
2. **Execution State & Observability Engine** - State machine, persistence, recovery, tracing, cost tracking
3. **AI Control & Safety Engine** - Policy engine, SQL safety, prompt pipeline, output validation
