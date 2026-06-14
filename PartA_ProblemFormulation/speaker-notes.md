# Speaker notes — Part A (Problem Formulation / ideation)

Part A presents the **idea and plan**, not a finished system — so everything is framed as a
proposal ("we will…"). The slides are written to be self-explanatory; these notes just give the
one-line intent of each. ~40–50 s per slide. Two members can split the deck.

---

**Slide 1 — Title.** Introduce the team and the pitch: a tool to assign outstation participants to
hostel rooms *optimally and fairly*, chosen under Track 1 because room allocation is a genuine
mathematical-optimisation problem (the assignment problem).

**Slide 2 — The problem.** Today the warden does this by hand. At festival scale that means
gender/accessibility errors, ignored preferences, wasted beds and an unfair waitlist. State the
need: an automated, optimal, constraint-safe allocation with a fair waitlist.

**Slide 3 — Our idea in one slide.** The core insight: model it as the classic *assignment
problem* — beds are slots, hard rules are forbidden pairings, soft preferences are penalties — and
solve it **exactly** with the Hungarian algorithm. Overflow goes to a priority waitlist.

**Slide 4 — Inputs & outputs.** Walk through what we will read (participants, rooms) and produce
(allocations with the wallet charge, waitlist, metrics). Emphasise CSV/JSON so it fits the
platform's existing file exchange.

**Slide 5 — Proposed mathematical model.** State the objective and constraints, the two cost
regimes (∞ for hard, penalties for soft), and the plan to pad to a square matrix and solve with the
Hungarian algorithm in O(K³). Mention that a dummy column = waitlisted.

**Slide 6 — Modelling approach (diagram).** Explain the key trick visually: expand rooms into beds,
pad to a square matrix; empty beds and the waitlist both come out of the padding.

**Slide 7 — Proposed solution approach.** Lay out the planned optimise→repair→finalise pipeline
(validate, build matrix, Hungarian, bipartite fallback, roommate co-location, waitlist). Stress
that later stages only improve the result.

**Slide 8 — Proposed pipeline (diagram).** The same six stages as a visual; Hungarian is the exact
optimiser at the centre.

**Slide 9 — Innovation.** Call out what's beyond a textbook solve: bed-expansion + padding for
capacity, priority as a matrix-only bias, optimise-then-repair, and a real-time arrival stream.

**Slide 10 — Planned integration.** We will reuse the platform's file-exchange channel; map each
output to its consumer (Wallet billing, Mobile notification, Admin waitlist) plus a simulated
real-time feed.

**Slide 11 — Integration diagram.** Show the engine sitting between the Accommodation module and
the Wallet/Mobile/Admin consumers, with the live arrival stream.

**Slide 12 — Java features we will use.** This satisfies the "justify ≥ 2 advanced features"
criterion: generics, concurrency, collections, serialization, file I/O, custom exceptions, design
patterns — each tied to a concrete need in the plan.

**Slide 13 — Success metrics.** How we'll judge success: zero hard-constraint violations, high
placement rate, minimal dissatisfaction, good utilisation, sub-second runtime, deterministic
waitlist fairness. (Targets, to be demonstrated in Part B.)

**Slide 14 — Feasibility & scope.** A realistic 3-week plan and the planned Part B deliverables.
Stress it's self-contained and runs from sample data.

**Slide 15 — Close.** Recap: optimal, fair, constraint-safe, integrated. Invite questions.
