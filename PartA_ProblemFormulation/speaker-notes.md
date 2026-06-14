# Speaker notes — Part A presentation (≤10 slides)

One short paragraph per slide; ~45–60 seconds each keeps the deck under the time limit. Each
group member can take 2–3 slides so everyone speaks (per the submission rules).

---

**Slide 1 — Title.** Introduce the team and the one-line pitch: an engine that optimally and
fairly assigns outstation participants to hostel rooms, chosen under Track 1 because it is a true
mathematical-optimisation problem (the assignment problem) with a classic exact algorithm.

**Slide 2 — The problem.** Today the warden does this by hand on a spreadsheet. With thousands of
participants and limited beds, manual allocation produces gender/accessibility mistakes, ignores
stated preferences, leaves beds empty while others are oversubscribed, and the waitlist is ad hoc.
We frame rooms as a scarce constrained resource and target a provably optimal, constraint-safe
assignment.

**Slide 3 — Inputs & outputs.** Walk through the two input files (participants, rooms) and the
three outputs. Emphasise the **wallet charge** field — our output feeds the Wallet module directly
(charge = price/night × nights) — and that a participant who can't be placed lands on a fair,
prioritised waitlist.

**Slide 4 — Mathematical model.** This is the heart. State the objective (minimise total
dissatisfaction) and the one-bed-per-participant / one-participant-per-bed constraints. Explain the
two cost regimes: hard constraints become *forbidden* edges (∞), soft preferences become finite
penalties. Then the three modelling moves: expand rooms into beds, pad to a square matrix, solve
with Hungarian O(K³); a bipartite matching pass recovers feasible placements the pure cost-optimum
might strand.

**Slide 5 — Innovation.** Call out what's clever beyond a textbook Hungarian: bed-expansion +
dummy padding to handle capacity and unequal sizes; **priority injected as a matrix-only bias** so
scarce beds go to performers/VIPs first *without* distorting room choice or the reported cost; the
optimise-then-repair two-phase design; and the real-time arrival stream.

**Slide 6 — Success metrics.** These are measurable and we hit them: zero hard-constraint
violations by construction, 100 % placement when there's capacity, globally minimal dissatisfaction,
and sub-0.3-second runtime at 500 participants (we measured it). Waitlist fairness is deterministic.

**Slide 7 — Integration.** Show the data-flow diagram. We integrate via the platform's existing
**file-exchange** channel — Django can produce our inputs and consume our outputs unchanged — and we
additionally simulate the **WebSocket** real-time feed with a BlockingQueue and provide a serialized
snapshot for offline/resume. Map each output to its consumer (Wallet, Mobile, Admin).

**Slide 8 — Advanced Java features.** This satisfies the rubric's "≥2 advanced features": we use
many, each for a real reason — generics for the type-safe DAO, an ExecutorService to build the cost
matrix in parallel, a PriorityQueue for the waitlist, serialization for the offline snapshot, and
seven design patterns. Point to the table.

**Slide 9 — Architecture & scope / feasibility.** The engine is pure and unit-tested, with thin CLI
and Swing layers on top; the GUI has two distinct role views as the brief requires. Lay out the
realistic 3-week plan and stress it's self-contained and runs from sample data immediately.

**Slide 10 — Close.** Recap the value: optimal, fair, constraint-safe, integrated. Invite questions
and tee up the Part B demo video.
