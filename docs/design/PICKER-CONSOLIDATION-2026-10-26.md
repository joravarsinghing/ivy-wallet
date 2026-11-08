# One date/time picker family — 26 October 2026

The owner correctly identified that the Goals repair introduced a separate picker beside existing transaction and legacy selectors. Shared colors alone did not make their interactions coherent. This follow-up consolidates the implementation without changing navigation, the ledger, stored dates, backup schemas or dependencies.

## Ownership and boundaries

`JvyPickerDialogs.kt` owns shared dialog hosting, calendar state setup and time presentation. Both date and time use `JvyFormDialogFrame`, the same typography/shapes, persistent cancellation and a pinned translated **Done** action. Date navigation/title aliases retain Material's existing translations. Time's native hour/minute and AM/PM labels remain localized; new title/toggle copy has English fallback pending localization review.

| Entry point | Shared route | Value semantics |
| --- | --- | --- |
| Income/expense and loan date editing | Existing `DateTimePicker.pickDate` → shared calendar dialog | Convert the existing Instant to its device-local calendar day. Return LocalDate; the existing consumer preserves its time and performs UTC conversion. |
| Transaction/loan time editing | Existing `DateTimePicker.pickTime` → shared time dialog | Respect system 12/24-hour preference; return hour/minute precision as before. |
| Goals and Net worth fields | Thin `JvyDateField` → shared calendar dialog | Keep typed LocalDate and existing deadline/wealth storage formats; no timestamp round-trip. |
| Legacy recurring rules, custom-period limits and transaction due dates | Existing `IvyWalletCtx` callbacks → `pickCalendarDate` / `pickTime` → shared dialogs | Preserve date-only values, enforce supplied inclusive limits and retain the caller's existing time-of-day policy. |

Fields inside another dialog host the shared picker locally so it appears above that form. ViewModel and legacy callers use the existing root-hosted facade. These are adapters to one renderer, not competing calendar implementations. Month/period selection remains a separate concept from choosing a single calendar day.

## Behavioral corrections

- Removed RootActivity's native MaterialDatePicker/MaterialTimePicker construction. Previously its date limits were not enforced and several cancellation listeners could report values on Cancel. Closing now delivers no value.
- A single pending request replaces the facade's independent date/time states. Each request has a fresh identity, so a previous dialog's completion/dismissal cannot affect a newer request. Clear the request before delivering its callback so date→time chaining works.
- Date bounds are validated before replacing an active request and checked again at confirmation. An out-of-bounds initial value displays a reachable month without pretending that a new valid date was selected. Existing valid dates before 1900/after 2100 extend the selectable year range rather than crashing on opening.
- Transaction Instants are interpreted in the device time zone only at the API adapter. Picker milliseconds represent UTC calendar dates, not local-midnight timestamps. No transaction consumer, UTC conversion formula or reservation/wealth calculation is rewritten.
- Keep Material clock editing on suitable windows; native numeric entry serves narrow/short/large-text windows, and a keyboard/clock toggle is available when the clock fits. Changing presentation retains the selected hour/minute. No custom time parser or second time-picker implementation is introduced.
- Picker drafts use saveable Material state; callbacks remain memory-only. Do not persist callback closures or manufacture completion events when restoring/dismissing a dialog.

## Verification

Focused shared-UI tests passed, including twelve facade contract tests, five date-bound tests, 32 calendar candidates and 24 time candidates. Calendar candidates cover valid/invalid bounds, exact selected days and an existing year beyond 2100. Time candidates cover 24-hour late evening, 12-hour afternoon and midnight. Matrix: 320dp phone, dark phone, AMOLED, 2× text phone/fold, square fold, DeX and short landscape.

Manual inspection confirms readable clock and numeric entry in AMOLED, narrow-phone, 2× text and landscape candidates, with pinned confirmation. The facade tests cover cancellation, stale events, request replacement, callback chaining, exactly-once delivery, inclusive limits, minute precision and a DST-boundary date edit preserving a transaction's local time.

The broader regression run passed **1,117 checks, zero failures/errors/skips**, including shared UI 143, base/time utilities 40, domain 94, data/import-backup core 183, Goals 118, Net worth 74, Loans 26, Home 162, navigation shell 56, Accounts 36, Budgets 33, Security 85, import UI 48 and the existing planned-payment/report/transaction/category suites. `:app:compileDebugKotlin` succeeded. Settings, transaction editing and onboarding have no unit tests in these tasks; they were compiled through the app graph, not certified by nonexistent tests.

`git diff --check` passes. Candidate previews are under ignored `artifacts/ui-review/pickers-2026-10-26/`. No APK was built, version/signing identity changed or approved screenshot golden replaced.

## Limits

Paparazzi and unit tests do not prove real-device dialog stacking, IME behavior, TalkBack traversal, hardware keyboard navigation or Android restoration after process death. These remain device-QA checks. Existing cross-currency Goals limitations from the preceding audit remain unchanged; picker consolidation is not a currency-model migration.

Android adaptive and UI/UX Pro Max guidance informed the reusable layout, sizing, native time controls and predictable cancellation. No Graphify skill or dependency-graph tool was needed: the relevant callers and bridges were directly traced and re-searched in the codebase.
