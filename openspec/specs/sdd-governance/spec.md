# sdd-governance Specification

## Purpose
Define process governance rules to ensure SDD changes are fully completed, tracked, and auditable.

## Scope
Applies to all SDD change lifecycles within the project, covering proposal, spec, design, verification, and archiving stages.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Soft Gate for Open Changes
The system **MUST** require a mandatory justification in `proposal.md` when **3 or more** SDD changes are open without an accompanying `archive.md`.

#### Scenario: Submit proposal with 3+ open changes
- **GIVEN** there are three active SDD change folders lacking `archive.md`
- **WHEN** a developer creates a new `proposal.md`
- **THEN** the proposal must include a `Justification:` section and the change is accepted only if the section is present.

### Requirement: Mandatory Phase Completion
The system **SHALL** block progression of a change if required phases are missing:
- Proposal without Spec → blocked
- Spec without Design → blocked

#### Scenario: Attempt to move change to review without spec
- **GIVEN** a change has a completed `proposal.md` but no `spec.md`
- **WHEN** a reviewer tries to mark the change as `in_review`
- **THEN** the system rejects the transition and prompts to add `spec.md`.

### Requirement: Auto‑Archive Inactive Changes
The system **MUST** automatically generate an `archive.md` with status **"suspended"** for any change with no activity for **14 days**.

#### Scenario: Inactive change after 14 days
- **GIVEN** a change folder has no file modifications for 14 consecutive days
- **WHEN** the daily housekeeping job runs
- **THEN** an `archive.md` is created with `status: suspended` and the change is marked inactive.

### Requirement: Offline Verification Gate
Any change affecting **sales**, **inventory**, or **sync** **MUST** include an `verification.md` that defines offline verification steps.

#### Scenario: Change touches sales module
- **GIVEN** a change modifies files under `src/sales/`
- **WHEN** the change is packaged
- **THEN** the build fails unless `verification.md` contains an offline verification checklist.

### Requirement: Beta Definition Gate
The six critical flows identified for Beta certification **MUST** have complete **Spec**, **Design**, and **Verification** documents before release.

#### Scenario: Beta flow missing verification
- **GIVEN** a critical flow has `spec.md` and `design.md` but no `verification.md`
- **WHEN** the release pipeline validates Beta readiness
- **THEN** the pipeline blocks the release and reports the missing verification.

## Definitions
- **Open Change**: An SDD change folder without an `archive.md`.
- **Critical Flow**: One of the six flows listed in the proposal's success criteria.
