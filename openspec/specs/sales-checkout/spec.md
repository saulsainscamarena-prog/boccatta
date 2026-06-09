# sales-checkout Specification

## Purpose
Define the checkout flow behavior in `SalesViewModelV2` ensuring correct handling of payment methods, offline scenarios, and edge cases.

## Scope
Applies to all checkout operations initiated through `SalesViewModelV2` in the sales module.

## Acceptance Criteria
### ADDED Requirements

### Requirement: Successful Charge with Single Payment Method
The system **MUST** complete a sale when a single valid payment method is provided.

#### Scenario: Successful charge with credit card
- **GIVEN** the cart contains items and a valid credit card is selected
- **WHEN** the user confirms the purchase
- **THEN** the payment is processed, inventory is deducted, and a receipt is generated.

### Requirement: Mixed Payment Methods Support
The system **MUST** allow splitting the payment across multiple methods.

#### Scenario: Split payment between cash and voucher
- **GIVEN** a total amount of $100 with $60 cash and $40 voucher selected
- **WHEN** the user confirms the purchase
- **THEN** the system processes both payments, updates inventory, and records both payment entries.

### Requirement: Prevent Double Charge
The system **SHALL** prevent duplicate processing when the checkout button is clicked multiple times.

#### Scenario: Double click on checkout button
- **GIVEN** a pending checkout request
- **WHEN** the user clicks the checkout button twice rapidly
- **THEN** the system processes the transaction only once and ignores subsequent clicks.

### Requirement: Offline Checkout Capability
The system **MUST** allow checkout when the device is offline, persisting the transaction locally.

#### Scenario: Offline checkout
- **GIVEN** the device has no network connectivity
- **WHEN** the user completes a sale
- **THEN** the transaction is saved locally and will be synced when connectivity is restored.

### Requirement: Insufficient Inventory Handling
The system **SHALL** abort the checkout if required inventory is unavailable.

#### Scenario: Checkout with insufficient stock
- **GIVEN** the cart requests 5 units of an item with only 3 in stock
- **WHEN** the user attempts to finalize the sale
- **THEN** the checkout is prevented and an out‑of‑stock error is shown.

### Requirement: Sale Cancellation with PIN
The system **MUST** require a manager PIN to cancel a completed sale.

#### Scenario: Cancel sale with correct PIN
- **GIVEN** a completed sale exists
- **WHEN** a manager enters the correct PIN to cancel
- **THEN** the sale is reversed, inventory restored, and audit log recorded.

### Requirement: Discount Application
The system **MUST** apply discounts to the total amount before payment processing.

#### Scenario: Apply percentage discount
- **GIVEN** a 10% discount coupon is applied to the cart
- **WHEN** the checkout proceeds
- **THEN** the total reflects the discount and payment is processed on the reduced amount.

### Requirement: Tip Addition
The system **MUST** allow adding a tip amount at checkout.

#### Scenario: Add tip to sale
- **GIVEN** a sale total of $50
- **WHEN** the user adds a $5 tip before confirming
- **THEN** the final amount charged is $55 and the tip is recorded separately.
