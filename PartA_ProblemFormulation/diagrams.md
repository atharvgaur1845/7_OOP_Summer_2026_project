# Diagrams — Accommodation Allocation Engine

Render these with any Mermaid-capable viewer (GitHub, VS Code Markdown Preview Mermaid
extension, or <https://mermaid.live>). Export to PNG/SVG for the report/slides.

---

## A. Data-flow / integration diagram

```mermaid
flowchart LR
    subgraph Backend["Festival backend (Django monolith)"]
        ACC["Accommodation module"]
        WAL["Wallet module"]
        MOB["Mobile app"]
    end

    ACC -- "participants.csv/json\nrooms.csv/json" --> ENG

    subgraph ENG["Accommodation Allocation Engine (Java)"]
        IO1["Repository&lt;T&gt; (DAO)\nCSV / JSON loaders"]
        SVC["AccommodationAllocator"]
        ALG["HungarianAlgorithm\n+ BipartiteMatcher"]
        OUT["AllocationWriter\nAllocationStore (.ser)"]
        IO1 --> SVC --> ALG --> SVC --> OUT
    end

    OUT -- "allocations.csv/json\n(charge = price x nights)" --> WAL
    OUT -- "allocations.csv/json" --> MOB
    OUT -- "waitlist.csv/json" --> ADMIN["Admin / Warden dashboard"]
    STREAM["Live arrivals\n(WebSocket ~ BlockingQueue)"] -- "ArrivalStream" --> SVC
```

---

## B. Allocation pipeline (sequence)

```mermaid
sequenceDiagram
    participant U as Admin/CLI
    participant S as AccommodationAllocator
    participant B as CostMatrixBuilder
    participant H as HungarianAlgorithm
    participant M as BipartiteMatcher

    U->>S: allocate(participants, rooms)
    S->>S: validate (ids, capacity)
    S->>B: build cost matrix (parallel, ExecutorService)
    B-->>S: square KxK CostMatrix (+ priority bias)
    S->>H: solve(matrix)
    H-->>S: optimal assignment
    S->>S: interpret -> assigned / stranded
    S->>M: match stranded to free beds (feasible only)
    M-->>S: extra placements
    S->>S: roommate co-location nudge (cost-neutral)
    S->>S: build waitlist (PriorityQueue)
    S-->>U: AllocationResult (allocations, waitlist, metrics)
```

---

## C. UML class diagram (core)

```mermaid
classDiagram
    class Participant {
      +String id
      +Gender gender
      +double budgetPerNight
      +int nights
      +boolean needsAccessibleRoom
      +Category category
      +Preference preference
    }
    class Room {
      +String id
      +int capacity
      +Gender genderPolicy
      +double pricePerNight
      +boolean accessible
    }
    class RoomSlot {
      +Room room
      +int index
    }
    class Allocation {
      +double cost
      +double charge()
    }
    class AllocationResult {
      +List~Allocation~ allocations
      +List~Participant~ waitlist
      +Metrics metrics
      +roommatesOf(id)
    }
    class CostStrategy {
      <<interface>>
      +cost(Participant, Room) double
    }
    class DefaultCostStrategy
    class HungarianAlgorithm {
      +solve(double[][]) int[]$
    }
    class BipartiteMatcher {
      +match(adj, rightSize) int[]$
    }
    class CostMatrixBuilder {
      +build(participants, rooms) CostMatrix
    }
    class AccommodationAllocator {
      +allocate(participants, rooms) AllocationResult
    }
    class Repository~T~ {
      <<interface>>
      +loadAll() List~T~
    }

    CostStrategy <|.. DefaultCostStrategy
    AccommodationAllocator --> CostStrategy
    AccommodationAllocator --> CostMatrixBuilder
    AccommodationAllocator --> HungarianAlgorithm
    AccommodationAllocator --> BipartiteMatcher
    AccommodationAllocator --> AllocationResult
    AllocationResult --> Allocation
    Allocation --> Participant
    Allocation --> Room
    CostMatrixBuilder --> RoomSlot
    RoomSlot --> Room
    Repository~T~ ..> Participant
    Repository~T~ ..> Room
```

---

## D. GUI roles (Observer / MVC)

```mermaid
flowchart TB
    M["AllocationModel\n(Subject / Model)"]
    A["AdminDashboard\n(load, run, simulate arrivals, export)"]
    P["ParticipantView\n(my room / waitlist position)"]
    M -- "onModelChanged()" --> A
    M -- "onModelChanged()" --> P
    A -- "runAllocation() via SwingWorker" --> M
    A -- "onArrival() via ArrivalStream" --> M
```
