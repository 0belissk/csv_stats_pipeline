# System Architecture

## Architectural Constraints
- Single-page application frontend (Angular)
- Stateless backend (Spring Boot, JWT-ready)
- Layered backend architecture (controller → service → repository)
- RESTful JSON APIs
- Password-based authentication with secure hashing (BCrypt)
- Event-driven processing for CSV workflows (future sprints)
- Infrastructure defined via Terraform (later sprint)

## Authentication Architecture (Current State – Sprint 1)
- Users can register and log in via REST endpoints
- Passwords are stored as BCrypt hashes
- No server-side sessions
- No JWT issuance or validation yet
- Security boundaries are explicitly defined
- System is designed to introduce JWT without refactoring

## Authentication Architecture (Planned – Sprint 2)
- JWT issued on successful login
- JWT validated on protected endpoints
- User identity propagated end-to-end
- Frontend stores and sends JWT on API requests

## Out of Scope for Sprint 1
- JWT issuance and validation
- Angular frontend implementation
- CSV upload
- Data processing pipeline
- AWS infrastructure provisioning

