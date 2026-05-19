# BookShare Project Instructions

## 📚 Project Overview
BookShare is a social community Android application for book lovers to share, exchange, or give away books. This is a 2-person university project designed with a strict zero-blocking architecture.

**AI Assistant Goal:** Your role is to help write course-compliant, highly modular code while respecting the parallel workflow of Member A (UI/Navigation) and Member B (Data/Backend).

## 🛠️ Tech Stack & Architecture
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **UI & Navigation:** Single Activity Architecture, Fragments, Navigation Component (`safeArgs`), XML Layouts (ConstraintLayout), RecyclerViews.
- **Local Persistence:** Room Database (Single Source of Truth).
- **Backend & Sync:** Firebase Authentication, Firebase Realtime Database/Firestore, Firebase Storage.
- **Networking:** Retrofit + Gson (for Google Books API).
- **Image Management:** Picasso (for remote image loading), `ActivityResultContracts` (for local Camera/Gallery).

## 🚨 Strict Architectural Rules (Course Compliance)
1. **Offline-First Synchronization (The Delta Fetch):** - Room is the **absolute single source of truth**.
    - Data flow MUST be: `Firebase/Retrofit ➔ Room ➔ LiveData ➔ ViewModel ➔ UI`.
    - When fetching from Firebase, you MUST use a `lastUpdated` timestamp check. Only fetch records newer than the local database's highest timestamp to minimize bandwidth.
2. **Zero-Blocking Workflow:** - UI code (Member A) must not wait for backend code (Member B).
    - If writing UI code, use mock data objects in the Fragment until the ViewModel is fully wired.
    - If writing Backend code, return `Result` wrappers or `LiveData` that the UI can eventually observe.
3. **Data Encapsulation:**
    - Repositories handle all data logic and external API calls.
    - ViewModels only interact with Repositories and expose `LiveData`.
    - Fragments only observe ViewModels and handle UI events.

## 📝 Coding Standards
- **Asynchronous Work:** Use Kotlin Coroutines (`suspend` functions, `viewModelScope`, `lifecycleScope`) for all network and database operations.
- **Lists:** Always use `RecyclerView` with custom `ViewHolder` and `Adapter`.
- **View Binding:** Prefer ViewBinding over `findViewById`.
- **Navigation:** Use the Navigation component's generated `Directions` classes for navigating between fragments.

## 🔀 Git Workflow
For every task or feature:
1. Create a new branch from `main` named `feat/{name-of-the-feature}` (e.g., `feat/book-listing-screen`).
2. Do all work on that branch.
3. When the task is complete, push the branch with a detailed commit message that describes:
   - **What** was added/changed.
   - **Why** the change was made.
   - **How** it fits into the architecture (e.g., which layer was touched: UI, ViewModel, Repository, Room, Network).

## 💻 Helpful Gradle Commands
- Build Debug APK: `./gradlew assembleDebug`
- Run Unit Tests: `./gradlew test`
- Clean Project: `./gradlew clean`