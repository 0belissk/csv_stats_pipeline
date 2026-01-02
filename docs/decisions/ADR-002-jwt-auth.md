# ADR-002: JWT Authentication

## Context
Sprint 1 established basic user authentication without token-based identity propagation.
Sprint 2 requires stateless authentication to support:
- A browser-based Angular client
- Horizontal scalability
- Secure access to protected APIs without server-side sessions

## Decision
- Use JWT (JSON Web Tokens) for stateless authentication
- Issue token on successful login via `/auth/login`
- Sign tokens using HMAC (HS256)
- Include the following claims:
  - `sub` (userId)
  - `email`
  - `iat`, `exp`
- Use short-lived tokens (e.g., ~15 minutes)
- Require `Authorization: Bearer <token>` on all protected endpoints
- Validate JWT on every request via Spring Security

## Consequences
### Positive
- Backend remains fully stateless
- Authentication scales horizontally without session storage
- Identity is reconstructed per request
- Frontend and backend are loosely coupled

### Trade-offs
- Frontend is responsible for token storage
- Requires explicit CORS configuration for browser clients
- Token expiration requires re-authentication or refresh strategy (future work)

## Notes
- JWT validation occurs after CORS approval in Spring Security
- Curl clients bypass CORS, browsers do not
- Refresh tokens are intentionally deferred to a later sprint

