# Public-release UI acceptance matrix

This is the per-surface worklist, not a whole-app approval. Updated 2026-11-04.
Use it with [the execution ledger](../plans/004-public-release-readiness.md) and
[design language](../design/JVY-DESIGN-LANGUAGE.md). Registered routes are guarded
by `NavigationLayoutContractTest`; every row below also needs its child controls
tested, not just a screenshot of its heading.

Evidence labels: **adaptive** = a current production-content candidate test exists;
**legacy render** = an older light/dark render exists but is not adaptive acceptance;
**source** = source/contract review only. A test existing is not proof its latest
run passed: use the execution ledger's final verification record. All device,
TalkBack, keyboard, process-death and owner visual approvals remain **pending**.
Do not promote screenshot references from this matrix alone.

The [4 November Home window checkpoint](../plans/004-v2-home-window-checkpoint-2026-11-04.md)
tracks the approved common scope for plot/spending/activity, fixed-date spending
drill-down and separate upcoming actions. Refer to its final gate/artifact record;
these changes do not certify unrelated child controls or hardware acceptance.

The [31 October spacing review](../design/SPACING-REVIEW-2026-10-31.md) tracks
shared title/body/footer alignment, 17 titled legacy adapters, actual selector
candidates and large-text recurrence overflow. Use its final verification record;
spacing changes do not approve untouched pages or hardware interactions.

## Pages and root destinations

| Surface | Current candidate evidence | Required acceptance cases |
| --- | --- | --- |
| Welcome / local account | adaptive onboarding | Small/short windows, 2× text, legal links, no fake Back or cloud login |
| Choose import or start fresh | adaptive onboarding | Both paths, Back returns to welcome, interrupted import |
| Set initial currency | adaptive onboarding | Search, empty search, selection retained on rotation, explicit confirmation |
| Initial accounts | adaptive onboarding; acknowledged writes, retained host/suggestions and startup recreation tests | Empty/populated, suggestions, long names/amounts, Add/Edit, failure/retry, pending Next/Skip/Back; device/process-death acceptance pending |
| Initial categories | adaptive onboarding; acknowledged writes, retained host/suggestions and startup recreation tests | Empty/populated, suggestions, Add/Edit, failure/retry, pending Finish/Skip/Back; device/process-death acceptance pending |
| Home | existing trend/activity renders; cross-month/quiet/zero-balance coverage regressions and corrected empty-period copy | Common card date scope awaits owner choice; every card visible/hidden, loading, real rollover/resume, page scroll and root navigation |
| Accounts | adaptive accounts | Empty/populated, compact/full modes, balances/reserved/available, inclusion, long labels |
| Plan hub | source | All four planning destinations, bottom bar/rail, large text |
| Net worth — overview | adaptive net-worth | Complete/incomplete/stale, negative/zero/unknown, dated totals and help |
| Net worth — items | adaptive net-worth | Filter/type/source, archived items, linked/manual, large values |
| Net worth — history | existing net-worth renders | Missing observations, historic coverage, hidden geometry and currency |
| More | source | Unique destinations, no duplicated global dropdown, keyboard traversal |
| All transactions | legacy page render; 80 adaptive actual-content/semantics cases cover headers, date/section totals, receiving equivalents, crypto precision and bounded transfer badges | Full page/long lists, actual focus/Back, hidden money during transitions, remembered scroll; component candidates are not whole-page acceptance |
| Transaction / transfer editor | adaptive transaction | Income/expense/transfer, conversion, save failure, IME, title/date/time/category/accounts/tags |
| Reports | adaptive report navigation; actual VM restoration/queued-calculation tests and strict saved-query regressions pass | Useful default, no results, independent filters, all-time/default, saved reports, preserved Clear/draft, compare/export; real lifecycle/IME acceptance pending |
| Search | eight-window production loading/retry candidates; actual VM query/retry/stale-result tests pass | Empty query, untitled entries, no results, restored query/scroll, latest request, return refresh, keyboard/Back, privacy; device acceptance pending |
| Category statistics / pie chart | source | Colour contrast, totals/percentages, zero/missing, hidden geometry, back stack |
| Budget list | adaptive budgets | Empty/populated, overspent, zero remaining, reorder, per-category totals |
| Planned payments | adaptive planned list | Empty/populated, forecast only when useful, overdue/upcoming, one Add action |
| Planned-payment editor | adaptive planned editor | Recurrence/one-time, save failure, account/category/date, keyboard and short windows |
| Goals | adaptive goals | Empty/populated, progress/deadline/allocations, hidden amounts/bars |
| Loans | adaptive loans; currency-grouped summaries and hidden-summary/progress renders passed | Pending/completed, borrow/lend, empty/populated, reorder, multicurrency totals, hidden money; device acceptance pending |
| Loan details | adaptive populated/hidden/error candidates passed; name/type/metric wrapping inspected at 200%, hidden state at AMOLED | Principal/repayments, balances, links, edit/delete, privacy, real navigation/IME acceptance; legacy styled header not fully redesigned |
| Balance calculator | legacy render | Scroll at 2× text, long amounts, arithmetic correctness, privacy |
| Categories | legacy render | Compact/full, search/sort/reorder, keyboard alternatives, colour icons |
| Exchange rates | legacy render | Offline/stale/manual, base guard, retry and failure, no implicit conversion |
| Settings | source | Theme/language/currency/month start/hiding/notifications, error recovery and app info |
| Security settings / unlock | existing security renders | PIN/biometric/either/legacy device credential, recovery proof, capture/recents and rotation |
| Import source/instructions/progress/result | existing renders, shared strict parsing/currency preflight, signed transfer/offset/fingerprint tests, failed-row/privacy regressions and atomic coordinator use | Every real source, interruption/SQLite rollback, loading write guard, partial failures, long failure lists and genuine-export reconciliation; synthetic/reversible-store tests do not close these gates |
| Manual CSV import | real importer regressions for separate transfer values/assets, strict parsing, currency conflicts, rollback orchestration and retry; actual VM single-exit regression | Mapping/encoding/delimiters, validate/confirm/failure without losing original file, real storage interruption, lifecycle/keyboard and source-currency reconciliation |
| AI conversion guide/preview | adaptive AI import | Copy public schema, validation counts, explicit confirm, unsupported fields, no automatic upload |
| Features / help | source | Accurate supported-feature copy, links, no old paid-survey promotion |
| Contributors | legacy render | Scroll/links, bounded content, large text |
| Attributions / licences | legacy render | Required upstream licences retained, links and readability |
| Releases | source | Safe external links, no implication of Play Store release |
| Disclaimer / legal | legacy render | Consent semantics, scrolling/actions at 2× text, no hidden required control |
| Retired poll route | source | Closed notice and Back only; no survey writes or automatic network request |

## Editors, dialogs and nested controls

| Surface | Evidence / open implementation | Required acceptance cases |
| --- | --- | --- |
| Account create/edit/adjust | adaptive real forms including pending/error/review; acknowledged callbacks and creation replay; seven Room tests, connected execution pending | Rollback/recovery, opening identity, changed/deleted account review, retained draft/error, dirty Close, pending IME/rotation, local-host/process restoration and concurrent edits |
| Account selector | stable-ID/index regressions + transaction candidates | Long names, accessible selection, add-account return, selected destination visible on load/refresh/reorder |
| Category create/edit | adaptive real forms including pending/error candidates; writer and live-session regressions | Colour/icon/name, failure retention, stable operation, duplicate taps; actual IME/Back, nested-host recreation and process-death acceptance pending |
| Category selector / remembered category rule | source | Search, no selection, new/edit category return, one rule per intended transaction |
| Budget create/edit/delete | adaptive real form + submission tests | Finite positive amounts, category selection, failed write/retry, pending-close protection |
| Goal create/edit/deadline | adaptive goal forms | Localized decimals, actual date, legacy invalid date error, draft restoration, failed save |
| Goal reservation | adaptive goal forms | Available funds, multiple accounts, currency restrictions, preserved unsupported allocations |
| Loan create/edit | acknowledged atomic linked writes; creator tests and adaptive pending/error/review renders passed | Borrow/lend, no accidental transaction, account change, invalid amounts, failure/retry, pending rotation/keyboard/device acceptance |
| Repayment create/edit | atomic linked writes; stable creation/conversion replay; tests and adaptive missing-rate/pending/error/review renders passed | Remaining principal, actual linked transaction, account/currency, correction, failure/retry, real Room execution and device acceptance |
| Tag create/edit/delete/select | acknowledged writes/selection, stable ID and retained host; atomic association deletion; writer tests and adaptive editor/selector renders passed | Authoritative draft, explicit Edit, failure retained, selection/delete association integrity and device/process-death acceptance |
| Title / description / note editors | source + transaction candidates | Long text, IME Done/Next, explicit save/discard, restored draft |
| Amount keypad | shared legacy host + source | Decimal locale, sign/precision, zero/nonfinite/large inputs, keyboard and overflow |
| Date picker | adaptive shared picker | All seven columns, UTC date-only conversion, locale/week start, limits, no change on cancel |
| Time picker | adaptive shared picker | 12/24h, clock/numeric, confirmed values survive resize, no change on cancel |
| Recurrence selector | adaptive actual recurring/one-time/invalid-count candidates; wrapping modes and stacking number/unit controls; invalid-count policy tests | Interval/unit/one-time changes, clearing/malformed input cannot retain old valid count, Back/cancel, restored choices; device interaction pending |
| Currency selector | shared native search/radio list + adaptive legacy entry-point candidates | Long currency names, no results, restored selection/search, short-window scrolling |
| Manual exchange-rate editor | submission tests + source | Different valid codes, finite positive rate, offline failed save, retry, dirty Close |
| Icon / colour selectors | adaptive actual icon selector and management-form colour candidates; primitive month/icon state retained | Accessible target labels, contrast, selection state, cancel and return to parent; hardware acceptance pending |
| Sort / reorder | shared button alternatives + source | Drag and Move Up/Down give same order, boundaries, keyboard focus, persisted order |
| Report filter sheet | adaptive report navigation; primitive draft/explicit Clear/invalid-range codec and VM regressions | Hidden sheet has no targets, Clear and invalid drafts restore separately, Cancel discards, Apply commits once, disabled invalid bounds explained; actual nested chooser interruption/IME acceptance pending |
| Saved-report name / delete | source | Validation, duplicate names, failure/retry and cancellation |
| Home card visibility/order | adaptive home editor | Native checkbox state, stable order, keyboard reorder, discard/commit |
| Home trend ranges/series/values | adaptive chart/editor | One shared scale, line-only legend, reduced motion, loading continuity, private value list |
| Net-worth guide / help disclosures | existing net-worth renders | Plain language, no repeated wall of text, correct financial meaning |
| Linked accounts/loans review | existing net-worth renders | Each item once, editable choices, return from nested editor, pinned Save |
| Wealth item / dated replacement | existing net-worth renders + domain tests | Ownership/units/source/type, effective date, old history retained, unknown vs zero |
| Valuation / historical date / rate | existing wealth/picker renders | Missing prices/rates/coverage, confirmation and safe cancellation |
| PIN setup/change / current-method proof | existing security renders + unit tests | Protected memory-only fields, keyboard/paste, correct authorization and failure handling |
| Recovery generation/replacement/reset | security authorization tests | One active code, fresh proof, show once, replacement invalidates old, no saved-state/log leakage |
| Screenshot/recording preference | capture-policy unit tests | Default protected, explicit opt-in, focus changes consistent, sensitive screens always protected |
| Export / document chooser | source | Private-data warning, MIME/name, cancellation/recreated callback, write failure, share only complete file |
| Destructive reset / deletion confirmations | source | Clear consequence, keyboard reachability, pending guard, no optimistic success |

## Android and distribution surfaces

| Surface | Required evidence (pending unless noted) |
| --- | --- |
| Bottom navigation / wide-window rail | Existing adaptive source tests; physical fold/resize/DeX window and keyboard traversal |
| System Back / nested dialog cleanup | Navigation contract regressions; real keyboard, gesture and nested-sheet lifecycle |
| Balance widget | Masked/zeroed private presentation cache implemented; actual widget refresh/lock/reboot checks |
| Add-transaction widget / shortcuts / tile | Source review; locked/unlocked cold/warm starts, malformed extras, no automatic financial write |
| Planned notifications / reminders | Denied/granted/revoked permissions, tapping correct destination, no duplicate financial action |
| File picker / share sheet | Cancel/Back, revoked URI, unavailable provider, private backup warning, interrupted export |
| Recents / screenshot / recording | Real-device sensitive-screen capture and background restoration |
| Network / diagnostics | No Firebase in actual release runtime graph; merged release manifest checked. Actual traffic observation pending |
| Signing refusal | Actual Gradle task rejected repository public test certificate; owner-key artifact verification pending |
| In-place owner-signed update | No candidate APK authorized; future same-key, higher-version update and reconciled backup/restore |
| Large-wallet responsiveness | Realistic large fixture, scroll/search/import/export timing and memory on a regular phone |

Record a dated result, device/window, theme, font scale, steps, expected result,
actual result and reviewer for each accepted row. A failed child flow blocks that
surface; a failed financial/privacy/auth/import gate blocks public distribution.
