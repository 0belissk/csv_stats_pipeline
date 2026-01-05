# Sprint 03 — CSV Validation Pipeline (Trigger → Parse → Validate → Result)
**Sprint length:** 1 day  
**Team:** 1 (PO / Dev / SM)  
**Context:** Auth + JWT + Angular login completed. Now we validate CSV uploads via an event-driven path.

## Sprint Goal
Validate uploaded CSVs asynchronously: an upload event triggers parsing and schema validation, producing a success/failure outcome with structured logs.

## Sprint Backlog (thin vertical slice)
- Event trigger wiring: on upload (S3 or API placeholder), emit a processing event/message (EventBridge/queue stub acceptable for this sprint).
- Worker/Lambda stub: fetch CSV from provided location; process small sample files to keep scope tight.
- Schema validation (minimal): required columns, data types, non-empty rows; fail fast; collect per-row reasons.
- Orchestration glue: Trigger → Worker → Validate → Result; minimal Step Functions/queue consumer wiring to prove flow.
- Structured logging: correlate upload/request ID; log success/failure and validation errors in JSON (CloudWatch-friendly).
- Tests: unit tests for validation rules with small valid/invalid CSV fixtures.

## Acceptance Criteria
- Given an upload event, the pipeline runs without manual intervention.
- Valid CSV logs `validated: success` with the upload ID; invalid CSV logs `validated: failed` with at least one reason.
- Trigger, parse, and validate steps are observable via structured logs.
- Works for a small sample CSV (happy + sad path).

## Definition of Done
- Code committed; validation tests passing.
- Manual demo: fire an upload event; observe logs for success and failure cases.
- No persistence or analytics yet; focus is event-driven validation.

## Risks / Assumptions
- Lambda/worker timeouts vs file size (keep fixtures small this sprint).
- Event contract is stable (upload ID + file location).
- Schema rules are minimal now; extensible later.
- Using stubbed infrastructure locally (e.g., localstack or in-memory) is acceptable for this sprint.

## Next Actions (immediate)
- [ ] Add sample CSV fixtures (`valid.csv`, `invalid.csv`) under `src/test/resources` (backend) and/or `src/assets/fixtures` (frontend, if needed).
- [ ] Implement validation rules + unit tests first.
- [ ] Wire trigger → worker → logs (happy + sad path).
- [ ] Capture demo notes for Sprint Review (log excerpts showing success/failure).
