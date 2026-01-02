# Sprint 2 – Authentication & Client Integration

## Sprint Goal
Enable stateless JWT authentication and verify it end-to-end from an Angular client.

## Sprint Backlog
- Implement JWT issuance on login
- Validate JWT on protected endpoints
- Configure Spring Security (stateless)
- Enable CORS for browser-based clients
- Scaffold Angular app
- Implement login flow
- Add HTTP interceptor for JWT
- Verify protected endpoint (`/me`) from Angular

## Completed Work
- Stateless JWT authentication implemented
- `/auth/login` issues signed JWTs
- `/me` endpoint protected via Spring Security
- Angular client successfully logs in and calls protected APIs
- JWT automatically attached via HTTP interceptor
- CORS configured within SecurityFilterChain for browser support

## Increment
A secure, stateless authentication system verified end-to-end from a real browser client.

## Verification
- Successful login returns JWT
- JWT stored in browser localStorage
- Authenticated `/me` request succeeds via Angular client
- Unauthorized requests return 401

## Notes / Learnings
- CORS must be configured inside Spring Security, not via standalone filters
- Browsers enforce CORS; curl does not
- Angular standalone apps require interceptors to be registered via `bootstrapApplication`

