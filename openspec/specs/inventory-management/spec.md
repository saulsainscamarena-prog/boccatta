# inventory-management Specification

## Purpose
Define how inventory is deducted, synchronized, and managed across sales, offline adjustments, recipes, combos, and alerts.

## Scope
Applies to inventory changes triggered by sales, offline adjustments, recipe preparations, and manual operations.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Stock Deduction on Successful Sale
The system **MUST** deduct inventory quantities when a sale completes successfully.

#### Scenario: Deduct stock after sale
- **GIVEN** a sale for 3 units of product X is completed
- **WHEN** the transaction is finalized
- **THEN** the inventory count for product X is reduced by 3.

### Requirement: Offline Stock Adjustment Queue
The system **MUST** queue stock adjustments when offline and apply them upon reconnection.

#### Scenario: Offline sale creates adjustment
- **GIVEN** the device is offline and a sale of 2 units of product Y occurs
- **WHEN** the sale is persisted locally
- **THEN** a record is added to `SQLiteStockAdjustmentQueue` for later sync.

### Requirement: Synchronize Adjustments on Reconnect
The system **SHALL** process queued adjustments when network connectivity is restored.

#### Scenario: Sync queued adjustments after reconnect
- **GIVEN** pending entries in `SQLiteStockAdjustmentQueue`
- **WHEN** the device regains network connectivity
- **THEN** each adjustment is sent to the server and the local queue is cleared.

### Requirement: Recipe‑Based Inventory Consumption
The system **MUST** deduct all ingredient stocks when a recipe is sold.

#### Scenario: Prepare recipe with multiple ingredients
- **GIVEN** a recipe requiring 2 units of ingredient A and 1 unit of ingredient B
- **WHEN** the recipe sale is completed
- **THEN** inventory for A is reduced by 2 and B by 1.

### Requirement: Combo/Duo Product Handling
The system **MUST** treat combo products as multiple item deductions.

#### Scenario: Sell combo product consisting of item C and D
- **GIVEN** a combo sale includes 1 unit of C and 1 unit of D
- **WHEN** checkout succeeds
- **THEN** inventory for C and D each decrease by 1.

### Requirement: Minimum Stock Alerts
The system **SHALL** trigger an alert when stock falls below the defined minimum threshold.

#### Scenario: Stock falls below minimum
- **GIVEN** product Z has a minimum threshold of 5 units and current stock is 6
- **WHEN** a sale reduces stock to 4
- **THEN** an alert is generated indicating low stock for product Z.

### Requirement: Physical Count and Manual Adjustment
The system **MUST** allow a manual inventory count and adjustment operation.

#### Scenario: Perform physical count and adjust
- **GIVEN** a physical audit reports 20 units of product Q, but system records 18
- **WHEN** a manager updates the count to 20
- **THEN** the system records a manual adjustment entry and updates the stock to 20.
