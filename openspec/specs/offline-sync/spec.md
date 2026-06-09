# offline-sync Specification

## Purpose
Define the behavior of the synchronization engine handling offline sales, data conflicts, and integrity during network reconnection.

## Scope
Covers offline transaction storage, synchronization queues, conflict resolution, retry/backoff strategies, and data integrity checks.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Offline Sale Persistence
The system **MUST** persist sales locally when the device is offline.

#### Scenario: Save sale offline
- **GIVEN** the device has no network connectivity
- **WHEN** a user completes a sale in `SalesViewModelV2`
- **THEN** the sale is stored in a local SQLite table `OfflineSales` for later synchronization.

### Requirement: Synchronization on Reconnection
The system **SHALL** automatically synchronize pending offline sales once connectivity is restored.

#### Scenario: Sync after network restore
- **GIVEN** one or more entries exist in `OfflineSales`
- **WHEN** the device detects network availability
- **THEN** each sale is sent to the server, marked as synced, and removed from the local table.

### Requirement: Data Conflict Resolution
The system **MUST** detect and resolve conflicts between offline adjustments and server state using a "last write wins" strategy with audit logging.

#### Scenario: Conflict during sync
- **GIVEN** an offline adjustment modifies stock for product X
- **WHEN** the server reports a newer stock version for X
- **THEN** the system logs the conflict, applies the server version, and creates a manual review entry.

### Requirement: Stock Adjustment Queue
The system **MUST** enqueue stock adjustments in `SQLiteStockAdjustmentQueue` for reliable processing.

#### Scenario: Queue stock adjustment
- **GIVEN** a stock change event occurs
- **WHEN** the adjustment is created
- **THEN** it is added to `SQLiteStockAdjustmentQueue` with status pending.

### Requirement: Retry with Exponential Backoff
The system **SHALL** retry failed sync attempts with exponential backoff up to 5 attempts.

#### Scenario: Failed sync retry
- **GIVEN** a sync attempt fails due to temporary server error
- **WHEN** the retry mechanism activates
- **THEN** the system retries with increasing delays (e.g., 1s, 2s, 4s, 8s, 16s) and stops after the fifth failure, marking the entry for manual review.

### Requirement: Post‑Sync Data Integrity Verification
The system **MUST** verify that all synced records match server acknowledgments and raise an alert on mismatch.

#### Scenario: Integrity check after sync
- **GIVEN** a batch of sales has been synced
- **WHEN** the server returns confirmations for each
- **THEN** the system validates each local record against the server response and logs any discrepancies.
