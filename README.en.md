# Kairos

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room%20DB-2.6.1-orange?style=flat&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20SOLID%20%2B%20MVVM-success)](#architecture)
[![API](https://img.shields.io/badge/Min%20SDK-26%2B-brightgreen)](https://android-arsenal.com/api?level=26)
[![Version](https://img.shields.io/badge/Version-1.1.7-blue)](app/build.gradle.kts)
[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial-orange.svg)](LICENSE)

**Languages:** [English](README.en.md) · [Русский](README.md)

> *In ancient Greek thought there are two kinds of time: Chronos — the steady ticking of the clock — and **Kairos** — the right, meaningful moment to act.*

**Kairos** is an Android app for strategic and day-to-day planning: **OKR**, the **Eisenhower Matrix**, and **single-task focus**.

It connects goals with today’s actions:
- **OKR board:** Today → Soon → Ongoing → Main Goal.
- **Eisenhower board:** four daily priority quadrants.
- **Task mirroring:** the same card can live in the plan and in the matrix.
- **UI languages:** English and Russian.

---

## Screenshots

> _(Add screenshots or a demo GIF)_

| OKR board | Eisenhower Matrix | Focus / widget | Calendar |
|:---:|:---:|:---:|:---:|
| `Screenshot 1` | `Screenshot 2` | `Screenshot 3` | `Screenshot 4` |

---

## Features

### Planning (OKR + Eisenhower)
- OKR hubs from a daily step to the main goal.
- Matrix board Q1–Q4; card buttons paint the task and mirror it into the matrix.
- Fast board switcher under the header with live counts.
- Projects, multiple boards, archive, board templates.

### Primary board and primary hub
- **Primary board** (`★` in Projects) — default home and quick-add target.
- **Primary hub** — set in the board **More** menu; opening the board lands there.
- **More** also lists all hubs on the board for one-tap navigation.

### Focus — “In progress”
- Bolt (⚡) on a card marks one focus task; others dim.
- Focus shows on home-screen widgets.

### Cards and hubs
- On the settled hub: ⚡ / ✓ and ⋮ (comments, due date, repeat, matrix, move, hide, delete…).
- Neighbor hubs stay lite; full chrome and action buttons appear after the swipe settles (soft snap).
- Drag & drop within and across hubs; long-press opens a cross-board drop panel.
- Double-tap the board title to open Projects.

### Due dates, repeats, reminders
- Due dates via DatePicker; calendar screen (overdue, Q badges).
- Task repeat; local due reminders (time in Settings).
- Android notification channel with Done / Snooze / Undo actions.

### Widgets
- **Kairos** — focus + overdue/today tasks + quick add.
- **Kairos Focus** — focus task only.
- Add via long-press home screen → Widgets → Kairos.

### Statuses, comments, contacts
- Color categories; status manager.
- Comments on tasks and hub rules.
- Project contacts (call / email / Telegram).

### Quick add and settings
- Quick Add from the widget or app; destination (board/hub) in Settings.
- Theme (system / light / dark), Q1–Q4 palette, language, Enter saves a task.
- Onboarding and a short OKR planning guide.

### Export / import
- JSON / `.kairos`; share to messengers; deep link `kairos://import?data=...`.

---

## Quick start

1. Open the OKR or Eisenhower board; swipe hubs horizontally.
2. Add a task; set due date, Q1–Q4, and category as needed.
3. Tap ⚡ to focus (also appears on the widget).
4. Board **More** → primary hub, hub list, sort, overdue filter.
5. In Projects mark the **primary board**; in Settings set quick-add destination and reminder time.
6. Add a home-screen widget for focus and deadlines.

---

## Architecture

Clean Architecture + SOLID + MVVM:

```
app/src/main/java/com/example/kaizenkanban/
├── data/           # Room, DAO, prefs, repository, transfer (.kairos)
├── domain/         # models, interfaces, use cases
├── reminders/      # due alerts, notification quick actions
├── widget/         # App Widgets (list + focus)
└── ui/
    ├── board/      # kanban / hubs
    ├── calendar/
    ├── projects/
    ├── settings/
    ├── onboarding/
    ├── i18n/       # EN / RU strings
    ├── navigation/
    ├── theme/
    └── viewmodel/
```

**Database:** Room schema **v16**, migrations `4→16` without wipe. UUID keys.

---

## Tech stack

- Kotlin 2.1.0, Jetpack Compose Material 3, Room 2.6.1 + KSP
- Coroutines / Flow / StateFlow, Navigation Compose, WindowSizeClass
- Gradle Kotlin DSL, AGP 8.3.2 · Min SDK 26 · Target / Compile SDK 34 · app **1.1.7**

---

## Build & run

**Requirements:** Android Studio (Ladybug+), JDK 17+, device/emulator API 26+.

```bash
git clone https://github.com/Ruslan103/kairos.git
cd kairos
```

Windows: `.\gradlew.bat assembleDebug`  
macOS / Linux: `./gradlew assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`

Release (with `keystore.properties`): `.\gradlew.bat assembleRelease`

---

## Roadmap

- [x] Local due reminders and home-screen widgets
- [ ] Cloud sync
- [ ] Sign-in (Google Sign-In)
- [ ] Compose Multiplatform (desktop)
- [ ] Productivity analytics

---

## License

**Kairos Source License (Non-Commercial)** — see [LICENSE](LICENSE).

**Copyright (c) 2026 Ruslan (`JIuMaPk@gmail.ru`).**

Allowed: viewing, copying, modifying, and non-commercial redistribution **with attribution**.  
Not allowed without consent: selling or commercial use.  
Commercial permission: `JIuMaPk@gmail.ru`.
