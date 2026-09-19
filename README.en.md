# Kairos

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room%20DB-2.6.1-orange?style=flat&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20SOLID%20%2B%20MVVM-success)](#architecture--clean-code)
[![API](https://img.shields.io/badge/Min%20SDK-26%2B-brightgreen)](https://android-arsenal.com/api?level=26)
[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial-orange.svg)](LICENSE)

**Languages:** [English](README.en.md) · [Русский](README.md)

> *In ancient Greek thought there are two kinds of time: Chronos — the steady ticking of the clock — and **Kairos** — the right, meaningful moment to act.*

**Kairos** is a modern Android app for strategic and day-to-day life planning, combining **OKR**, the **Eisenhower Matrix**, and **chunking / focus**.

It connects long-term goals with today’s actions:
- **OKR board:** Today → Soon → Ongoing → Main Goal.
- **Eisenhower Matrix board:** four quadrants (Urgent & important, Important not urgent, Urgent not important, Not urgent & not important).
- **One-tap task mirroring:** the same card can live in your plan and in the day’s priority matrix.
- **Fast board switcher:** jump between OKR and the matrix in one tap at the top of the screen.

---

## Screenshots

> _(Add screenshots or a demo GIF here)_
>
| OKR board | Eisenhower Matrix | In progress | Due-date calendar |
|:---:|:---:|:---:|:---:|
| `Screenshot 1` | `Screenshot 2` | `Screenshot 3` | `Screenshot 4` |

---

## Features

### 1. Two-level planning (OKR + Eisenhower Matrix)
- **OKR board (from step to main goal):**
  - *Today* — one concrete step for this day
  - *Soon* — the next few days / week
  - *Ongoing* — habits and practices you keep
  - *Main Goal* — the bigger picture this is all for
- **Eisenhower board (daily priority filter):**
  - *Urgent and important (Q1)* — deadlines and crises
  - *Important, not urgent (Q2)* — growth, health, strategy (key focus)
  - *Urgent, not important (Q3)* — routine and delegation
  - *Not urgent and not important (Q4)* — cut time-wasters
- **One-tap board switcher** under the header, with live task counts.

### 2. Mirror tasks across boards in one tap
- Each card has a one-tap quadrant selector: `[ Q1 ]` `[ Q2 ]` `[ Q3 ]` `[ Q4 ]`.
- Choosing a quadrant places the task on the Eisenhower board while keeping it in the OKR plan.
- Done state, comments, and due dates stay in sync.
- Matrix cards show their home hub (e.g. *“From plan: Today”*).

### 3. Focus mode — “In progress”
- The active task is highlighted with a strong gradient and an “NOW IN PROGRESS” badge.
- Other tasks fade so you can stay in flow.

### 4. Drag & drop and swipe-to-reveal
- **Swipe left on a task:** Comments, Edit, Delete.
- **Swipe left on a hub header:** Rename, Delete.
- **Compact hub navigation** under a task: `[ ◀ ] [ 📁 ] [ ▶ ]`.
- **Drag & drop** within a hub and across neighboring hubs, with visual insert slots.
- **Cross-board drag:** hold a task to open a panel of other boards at the bottom.

### 5. Statuses and categories
- Color-coded categories (Reflection, Learning, Control, Execution).
- Empty status chips stay hidden when unused.
- Full category manager: create, rename, pick colors.

### 6. Task comments
- Notes and discussion on any task.
- Timestamps and a comment count on the card.

### 7. Completed section
- One-tap complete with strike-through.
- Large thumb-friendly Done control.
- Completed tasks move into a collapsible hub section with finish time.

### 8. Calendar and due dates
- Due dates via Material 3 DatePicker.
- Calendar screen: by date, overdue highlight, Eisenhower badges.

### 9. Export, import, and messenger sharing (deep links)
- Full backup/restore of projects, boards, hubs, tasks, and comments as JSON / `.kairos`.
- Share to messengers (Telegram, WhatsApp, Viber, etc.) with a `kairos://import?data=...` preview and backup file.
- One-tap import from a link or `.kairos` file.

---

## Architecture & Clean Code

The project follows **Clean Architecture** and **SOLID**:

```
app/src/main/java/com/example/kaizenkanban/
├── data/                  # Data layer
│   ├── local/             # Room Database, DAO, Entity, Migrations
│   ├── mapper/            # Entity ↔ domain mappers
│   ├── repository/        # Repository implementations (DIP)
│   └── transfer/          # JSON, GZIP, Base64, deep-link helpers
├── domain/                # Business logic (pure Kotlin)
│   ├── model/             # Immutable domain models
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Granular use cases
└── ui/                    # Presentation
    ├── board/             # Kanban board (BoardScreen)
    ├── calendar/          # CalendarScreen
    ├── navigation/        # Navigation graph
    ├── projects/          # ProjectsScreen
    ├── theme/             # Material 3 themes & palettes
    └── viewmodel/         # SharedViewModel, AppState
```

### SOLID in practice
- **S:** each operation is its own use case (`AddTaskUseCase`, `MoveTaskUseCase`, …).
- **O:** `KanbanRepository` abstracts storage and can be extended (e.g. remote API).
- **L:** implementations honor interface contracts.
- **I:** narrow, focused APIs.
- **D:** domain does not depend on Android/Room; dependencies are injected.

### Database & migrations
- Room migrations (`MIGRATION_4_5`, `MIGRATION_5_6`) without wiping user data.
- UUID primary keys for easier future cloud sync.

---

## Tech stack

- **Language:** Kotlin 2.1.0
- **UI:** Jetpack Compose (Material 3, BOM 2024.02.00)
- **Architecture:** Clean Architecture + MVVM
- **Async:** Kotlin Coroutines, `StateFlow`, `Flow`, `combine`
- **Local DB:** Room 2.6.1 + KSP
- **Navigation:** Jetpack Navigation Compose 2.7.7
- **Adaptive UI:** WindowSizeClass
- **Build:** Gradle (Kotlin DSL), AGP 8.3+

---

## Build & run

### Requirements
- Android Studio Ladybug / Jellyfish (or newer)
- JDK 17+ (17 or 21 recommended)
- Device or emulator with Android 8.0+ (API 26+)

### From source

1. Clone the repo:
   ```bash
   git clone https://github.com/Ruslan103/kairos.git
   cd kairos
   ```

2. Open in Android Studio, or build a debug APK:
   - **Windows:**
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   - **macOS / Linux:**
     ```bash
     ./gradlew assembleDebug
     ```

3. APK path:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Roadmap

- [ ] Cloud sync across devices (Firebase Firestore)
- [ ] User sign-in (Google Sign-In)
- [ ] Desktop build with Compose Multiplatform
- [ ] Reminders and push notifications (WorkManager)
- [ ] Productivity analytics

---

## License

Distributed under the **Kairos Source License (Non-Commercial)** — see [LICENSE](LICENSE).

**Copyright (c) 2026 Ruslan (`JIuMaPk@gmail.ru`).**

Allowed: viewing, copying, modifying, and non-commercial redistribution **with attribution preserved**.

Not allowed without the author’s written consent: selling, fee-based licensing, or any commercial use (including shipping APKs or embedding in a commercial product).

For commercial permission: `JIuMaPk@gmail.ru`.
