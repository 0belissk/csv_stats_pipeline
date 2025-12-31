# ADR-002: JWT Authentication

## Context
Sprint 1 established user authentication without tokens.
Sprint 2 introduces stateless authentication across requests.

## Decision
- Use JWT for stateless authentication
- Token issued on successful login
- Token signed with HMAC (HS256)
- Token includes userId and email
- Token expiration: short-lived (e.g. 15 minutes)

## Consequences
- Backend remains stateless
- Frontend responsible for token storage
- All protected endpoints require Authorization header

