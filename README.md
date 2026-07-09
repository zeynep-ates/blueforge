# BlueForge

BlueForge is a software planning platform built with Java and Spring Boot that helps transform early-stage product ideas into structured technical plans.

Instead of generating an entire specification from a single prompt, BlueForge follows an iterative planning workflow. It asks clarifying questions, captures missing requirements, versions every planning step, and gradually builds a more complete project specification.

The project is designed to explore backend architecture, AI integration, and production-oriented software engineering practices.

---

## Current Features

- Create software projects from natural language descriptions
- Generate AI-powered clarifying questions
- Submit answers and generate functional/non-functional requirements
- Generate epics from requirements
- Generate user stories (with acceptance criteria) from epics
- Generate engineering tasks (with priority and effort estimate) from user stories
- Version project planning sessions, with each pipeline stage tracked via status
- Retrieve previous project versions
- Provider-independent AI abstraction
- Transaction-safe persistence at every stage
- Flyway database migrations
- Docker-based local development
- Input validation
- Architecture documentation
- React frontend covering the full pipeline, with a hierarchical Epic → User Story → Task view
- Light / dark / system theme support

---

## Architecture

```
                  +----------------+
                  | React Frontend |
                  +-------+--------+
                          |
                          v
                 Spring Boot REST API
                          |
                +---------+---------+
                |                   |
                v                   v
          Application Service    AI Client
                |                   |
                |             OpenRouter
                |
                v
          Spring Data JPA
                |
                v
            PostgreSQL
```

BlueForge follows a layered architecture where each layer has a single responsibility.

- Controllers expose REST endpoints.
- Services contain business logic.
- Repositories handle persistence.
- AI providers are isolated behind a common interface.
- Prompt templates are stored outside the application code.

---

## Technology Stack

### Backend

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Database Migration | Flyway |
| AI Gateway | OpenRouter |
| Build Tool | Maven |
| Testing | JUnit 5 |
| Containerization | Docker |

### Frontend

| Category | Technology |
|----------|------------|
| Language | TypeScript |
| Framework | React 19 + Vite |
| Styling | Tailwind CSS v4 + shadcn/ui |
| Routing | React Router |
| Server state | TanStack Query |
| API client | Generated from the backend's OpenAPI spec via `orval` |
| Testing | Vitest + React Testing Library |

---

## Project Structure

```
src
├── main
│   ├── java
│   │   └── com.blueforge
│   │       ├── ai
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       └── service
│   │
│   └── resources
│       ├── db
│       │   └── migration
│       └── prompts
│
└── test

frontend
└── src
    ├── api            # generated API client + TanStack Query hooks
    ├── components
    │   ├── layout
    │   ├── pipeline    # per-stage sections (Questions, Requirements, Epics, ...)
    │   └── ui          # shadcn/ui primitives
    ├── lib             # pipeline state derivation, recent-projects storage
    └── pages           # NewIdeaPage, WorkspacePage

docs
└── architecture
```

---

## Workflow

```
User submits an idea
        │
        ▼
POST /api/projects
        │
        ▼
Generate clarifying questions
        │
        ▼
POST .../answers
        │
        ▼
Generate requirements
        │
        ▼
POST .../epics
        │
        ▼
Generate epics
        │
        ▼
POST .../user-stories
        │
        ▼
Generate user stories
        │
        ▼
POST .../tasks
        │
        ▼
Generate tasks
        │
        ▼
Retrieve project version
```

Each stage has its own endpoint, its own AI prompt, and its own persistence model. A project version tracks which stage it has reached and only allows the next stage to run once the previous one has completed.

---

## API

### Create Project

```
POST /api/projects
```

Request

```json
{
  "name": "BlueForge",
  "ideaDescription": "A platform that transforms software ideas into technical plans."
}
```

Response

```json
{
  "projectId": 1,
  "versionId": 1,
  "questions": [
    "Who are the primary users?",
    "Will authentication be required?",
    "Should multiple users collaborate on the same project?"
  ]
}
```

---

### Get Project Version

```
GET /api/projects/{projectId}/versions/{versionNumber}
```

---

### Submit Answers

```
POST /api/projects/{projectId}/versions/{versionNumber}/answers
```

Persists answers to the clarifying questions and synchronously generates requirements.

---

### Generate Epics

```
POST /api/projects/{projectId}/versions/{versionNumber}/epics
```

Generates epics from the version's requirements.

---

### Generate User Stories

```
POST /api/projects/{projectId}/versions/{versionNumber}/user-stories
```

Generates user stories (with acceptance criteria) for every epic in the version, in a single AI call.

---

### Generate Tasks

```
POST /api/projects/{projectId}/versions/{versionNumber}/tasks
```

Generates engineering tasks (with priority and effort estimate) for every user story in the version, making one AI call per epic.

---

## API Documentation

Once the application is running, interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI spec is available at:

```
http://localhost:8080/v3/api-docs
```

---

## Frontend

A React frontend covers the full pipeline end to end — submit an idea,
answer the clarifying questions, then generate and browse Requirements,
Epics, User Stories, and Tasks as an expandable hierarchy.

> **Screenshots**: pending. To capture them: run both the backend and the
> frontend (see below), walk through the pipeline for one project, and save
> screenshots of the New Idea page and the Workspace page (light and dark)
> into `docs/screenshots/`, then reference them here.

Key characteristics:

- The UI's state (which stage is locked/current/done) is derived directly
  from the backend's `ProjectVersionResponse.status` — no separate frontend
  wizard state to keep in sync.
- The API client is generated from the backend's live OpenAPI spec
  (`npm run generate:api`), so frontend types can't drift from the Java DTOs.
- Light / dark / system theme, persisted in `localStorage`.

---

## Running Locally

Clone the repository.

```bash
git clone https://github.com/zeynep-ates/blueforge.git
cd blueforge
```

Start PostgreSQL.

```bash
docker compose up -d
```

Configure the required environment variables.

```text
OPENROUTER_API_KEY=your_api_key
OPENROUTER_MODEL=your_model
```

Run the backend.

```bash
./mvnw spring-boot:run
```

Run the frontend (in a separate terminal).

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173` and expects the backend at
`http://localhost:8080` by default (override via `VITE_API_BASE_URL` in a
`frontend/.env` file — see `frontend/.env.example`).

---

## Testing

Run the backend test suite.

```bash
./mvnw verify
```

Current backend test coverage includes:

- Service layer tests
- Controller tests
- AI client tests
- Persistence verification
- Transaction rollback verification

Run the frontend test suite.

```bash
cd frontend
npm test
```

---

## Design Decisions

Some of the architectural decisions made during development include:

- Flyway is the single source of truth for the database schema.
- Hibernate validates the schema instead of generating it.
- AI providers are isolated behind a common interface.
- Prompt templates are stored outside Java code.
- DTOs are separated from persistence entities.
- Database operations are transactional to guarantee consistency.

Additional documentation is available under:

```
docs/architecture
```

---

## Development Roadmap

### Sprint 1

- Project creation
- Clarifying question generation
- Version retrieval

### Sprint 2

- Answer clarification questions
- Requirement generation
- Swagger / OpenAPI

### Sprint 3

- Epic generation

### Sprint 4

- User story generation

### Sprint 5

- Task generation

### Sprint 6

- React frontend covering the full pipeline
- Hierarchical Epic → User Story → Task visualization
- Blue-centered visual identity, custom logo, light/dark/system theme

### Later

- Sprint 7: Browse + Edit APIs (list projects/versions, edit generated content)
- Sprint 8: Authentication & authorization
- Sprint 9: Export support (Markdown/PDF, then Jira/GitHub integrations)
- Sprint 10: Regeneration of individual entities

---

## Project Status

Current version:

**v0.6.0**

Sprints 1 through 6 have been completed. The full planning pipeline is implemented end to end, backend and frontend:

```
Project → Clarifying Questions → Answers → Requirements → Epics → User Stories → Tasks
```

The application currently supports idea submission, AI-assisted clarification, requirement generation, epic generation, user story generation (with acceptance criteria), task generation (with priority and effort estimate), project versioning, and retrieval — all through a React frontend as well as the REST API directly. Each pipeline stage is exposed as its own endpoint and tracked via its own project version status.

---

## Author

**Zeynep Ateş**

Management Information Systems Graduate

Backend Developer focused on Java, Spring Boot, and AI-powered backend systems.

GitHub: https://github.com/zeynep-ates