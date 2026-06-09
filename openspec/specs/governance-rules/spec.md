# governance-rules Specification

## Purpose
Define the governance rules that enforce the SDD change process, visibility of change states, and regular auditing.

## Scope
Applies to every SDD change lifecycle within the project, covering gate enforcement, state tracking, and weekly audit procedures.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Automatic Gate Execution
The system **MUST** automatically run all defined gates (Soft Gate, Phase Completion, Offline Verification, Beta Definition) before allowing a new SDD change to be created or advanced.

#### Scenario: Attempt to create new change without satisfying Soft Gate
- **GIVEN** there are already three open changes lacking `archive.md`
- **WHEN** a developer attempts to add a new change folder
- **THEN** the creation is blocked and the developer is prompted to provide a justification in the new `proposal.md`.

### Requirement: Visible Change State
Each SDD change **SHALL** expose a visible `state` field with one of the values: `planned`, `in_progress`, `in_review`, `completed`, `canceled`, `suspended`.

#### Scenario: State displayed in change dashboard
- **GIVEN** a change folder exists
- **WHEN** a user views the change list
- **THEN** the current state of the change is shown alongside its name.

### Requirement: Weekly Auditing Process
The process **MUST** perform a weekly audit that reviews all active changes, validates gate compliance, and updates statuses as needed.

#### Scenario: Weekly audit execution
- **GIVEN** the scheduled audit job runs every Sunday
- **WHEN** it scans all change folders
- **THEN** it verifies gate compliance, flags any violations, and records an audit log entry for the week.
