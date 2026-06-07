# Spec

## Scenario: Lock overlay remains visually clear

Given the POS locks by inactivity
When the lock screen is displayed
Then the background is a dark scrim over the current screen
And the PIN card is the only bright foreground surface.

## Scenario: Exact cash remains one tap

Given a cart with total greater than zero
When the cashier opens payment and selects exact cash
Then the sale is finalized through the existing ViewModel checkout path
And no additional confirmation or keyboard entry is required.

## Scenario: No checkout behavior change

Given payment is confirmed
When checkout runs
Then stock validation, online save, offline fallback, and success dialog behavior remain owned by the existing ViewModel/repository flow.
