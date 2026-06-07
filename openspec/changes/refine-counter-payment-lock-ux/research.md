# Research

No external research required. Evidence came from AVD flows on `bocatta_tablet_api36`.

## AVD Evidence

- `PagoSheetV2` completed a sale through `Cobrar efectivo exacto`.
- The sheet still uses an internal scroll area for payment controls.
- The lock screen screenshot showed a light outer panel plus a light inner card.
- Code showed lock overlay using `MaterialTheme.colorScheme.scrim`, which can be affected by custom theme configuration.

## UX Decision

Keep this phase tactical. Do not redesign checkout logic. Improve visible counter friction without touching stock, repositories, sync, or payment persistence.
