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

---

## Architecture

```
                  +----------------+
                  |     Client     |
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

---

## Project Structure

```
src
├── main
│   ├── java
│   │   └── com.blueforge
│   │       ├── ai
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

Run the application.

```bash
./mvnw spring-boot:run
```

---

## Testing

Run the complete test suite.

```bash
./mvnw verify
```

Current test coverage includes:

- Service layer tests
- Controller tests
- AI client tests
- Persistence verification
- Transaction rollback verification

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

### Later

- Architecture recommendations
- Frontend application
- Authentication
- Export support

---

## Project Status

Current version:

**v0.5.0**

Sprints 1 through 5 have been completed. The full planning pipeline is implemented end to end:

```
Project → Clarifying Questions → Answers → Requirements → Epics → User Stories → Tasks
```

The application currently supports idea submission, AI-assisted clarification, requirement generation, epic generation, user story generation (with acceptance criteria), task generation (with priority and effort estimate), project versioning, and retrieval. Each pipeline stage is exposed as its own endpoint and tracked via its own project version status.

---

## Author

**Zeynep Ateş**

Management Information Systems Graduate

Backend Developer focused on Java, Spring Boot, and AI-powered backend systems.

GitHub: https://github.com/zeynep-ates