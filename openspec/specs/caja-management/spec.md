# caja-management Specification

## Purpose
Define the cash register operations including opening, closing, reporting, expense registration, and audit of cancellations.

## Scope
Applies to all cash register (caja) interactions performed by cashiers and managers within the POS system.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Cash Register Opening
The system **MUST** allow a cashier to open a cash register with an initial amount.

#### Scenario: Open cash register
- **GIVEN** a cashier authenticates and provides an opening amount of $200
- **WHEN** the "Open Register" action is triggered
- **THEN** a new cash session is created, the opening balance is recorded, and the register state becomes *open*.

### Requirement: Cash Register Closing (Cut)
The system **MUST** support closing a cash register, calculating the final balance, and generating a closure report.

#### Scenario: Close cash register
- **GIVEN** an open cash session with recorded transactions
- **WHEN** the cashier selects "Close Register"
- **THEN** the system computes the closing balance, marks the session as *closed*, and produces a printable closure report.

### Requirement: End‑of‑Day Report Generation
The system **SHALL** generate a daily report summarizing sales, refunds, tips, and cash movements.

#### Scenario: Generate daily report
- **GIVEN** the business day has ended and the register is closed
- **WHEN** the manager requests the daily report
- **THEN** the report includes total sales, total cash received, total tips, refunds, and net cash.

### Requirement: Record Expenses in Cash Register
The system **MUST** allow expenses to be recorded against the open cash register.

#### Scenario: Register an expense
- **GIVEN** an open cash register and an expense of $15 for supplies
- **WHEN** the cashier logs the expense
- **THEN** the expense is deducted from the cash balance and appears in the daily report.

### Requirement: Audit of Cancelled Sales
The system **SHALL** log every sale cancellation with reason and responsible user for audit purposes.

#### Scenario: Audit cancelled sale
- **GIVEN** a completed sale is cancelled by a manager with a reason "Customer returned items"
- **WHEN** the cancellation is processed
- **THEN** an audit entry is created containing sale ID, cancellation time, manager ID, and reason.
