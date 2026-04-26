📚 BookShare: AI-Optimized Mobile Development Project Plan
🏗️ Architectural Core (Course Compliance)
Architecture: Strict MVVM (Model-View-ViewModel) + Repository Pattern.

Navigation: Single Activity Architecture using Fragments, Navigation Component, and SafeArgs.

Data Strategy (Offline-First): Room Database is the Single Source of Truth. Data flows: Firebase/Retrofit ➔ Room ➔ LiveData ➔ ViewModel ➔ UI.

Sync Mechanism: Firebase Realtime Database/Firestore queried using a lastUpdated timestamp delta fetch (only fetching records newer than the local database timestamp).

Networking & Media: Retrofit + Gson for Google Books API, Firebase Storage for images, Picasso for UI image loading.

🚀 Phase 0: Project Initialization & Git (Parallel Setup)
Member A (UI/Shell Leader): Initialize the Android Studio project (Kotlin, Empty Activity). Create the basic package structure (ui, model, repository, network, local). Initialize the Git repository and push the initial commit to GitHub.

Member B (Data/Backend Leader): Clone the repository. Create a Firebase Project in the Firebase Console. Enable Authentication (Email/Password), Database, and Storage. Generate the google-services.json file and push the Gradle dependency updates to Git.

🏃 Sprint 1: Foundation & Authentication (Zero-Blocking)
🧑‍💻 Member A: UI Shell & Navigation (UPDATED)
Task 1A: Navigation Graph & Fragments Shell

Action: Create empty Fragments for Login, Register, Feed, MyPosts, Profile, and AddEditBook. Map them in nav_graph.xml with appropriate actions.

AI Prompt Concept: Generate a Navigation Component XML graph containing 6 fragments [List Names]. Include actions to navigate from Login to Feed, Login to Register, Feed to Profile, and Feed to AddEditBook.

Task 2A: Authentication UI (Vision-Assisted)

Action: Build XML layouts for Login and Register screens using ConstraintLayout.

AI Prompt Concept: AI AGENT INSTRUCTION: Please review the local image file "login_mockup.png" (or ask the user to provide it). Based strictly on the visual hierarchy in this image, generate a ConstraintLayout XML for the login screen adhering to Material Design guidelines. Do not include Java/Kotlin logic.

Task 3A: Profile Layout & Image Capture UI (Vision-Assisted)

Action: Build the Profile layout matching the mockup. Implement the Course's exact Camera/Gallery intent logic to select a profile picture. Use Picasso to preview the selected local URI.

AI Prompt Concept: AI AGENT INSTRUCTION: Review the local image file "profile_mockup.png". Generate the XML layout for this screen. Then, write a Kotlin Fragment snippet using ActivityResultContracts to launch the device camera or pick an image from the gallery, returning a Bitmap or URI to populate the profile image view.

🧑‍💻 Member B: Data Layer Shell & Firebase Auth
Task 1B: Room Database Foundation

Action: Setup Room dependencies. Create the base AppDatabase class and the User Entity (id, name, email, avatarUrl). Create UserDao.

AI Prompt Concept: Generate a Kotlin Room Database setup including an Entity named 'User', a DAO with insert and query methods, and an abstract RoomDatabase class utilizing the Singleton pattern via Companion Object.

Task 2B: Firebase Authentication Repository

Action: Create AuthRepository. Implement signInWithEmail, registerWithEmail, and a check for auth.currentUser (auto-login state).

AI Prompt Concept: Create a Kotlin AuthRepository class using FirebaseAuth. Implement suspend functions for login and registration that utilize Kotlin Coroutines and return a custom Result wrapper.

Task 3B: Firebase Storage Shell

Action: Create a utility class or function in the Repository to upload a byte array/URI to Firebase Storage and return the download URL.

AI Prompt Concept: Write a Kotlin function that takes a Bitmap, compresses it to JPEG, uploads it to Firebase Storage, and returns the download URL via a callback or Coroutine.

🏃 Sprint 2: The "BookShare" Core (Zero-Blocking)
🧑‍💻 Member A: Feed UI & RecyclerViews (UPDATED)
Task 4A: Feed RecyclerView Setup (Vision-Assisted)

Action: Create the XML layout for a single Book Post item.

AI Prompt Concept: AI AGENT INSTRUCTION: Review the local image file "feed_mockup.png" and isolate the design of a single list item card. Generate an XML layout for a RecyclerView item representing this book post. Ensure the book cover has a fixed ratio and the layout uses ConstraintLayout.

Task 5A: Adapters and ViewHolders

Action: Implement the BookAdapter and BookViewHolder. Use Picasso inside the ViewHolder to load image URLs. Mock a dummy list of books in the Fragment to test the UI immediately.

AI Prompt Concept: Generate a RecyclerView Adapter and ViewHolder in Kotlin for a Book data class. Inside the bind method, use Picasso to load an image URL into the ImageView. Include a lambda function for item clicks.

Task 6A: "My Posts" UI (Vision-Assisted)

Action: Re-use the BookAdapter in the MyPostsFragment. Add swipe-to-delete UI logic (ItemTouchHelper) or an "Edit/Delete" button overlay based on the mockup.

AI Prompt Concept: AI AGENT INSTRUCTION: Review the local image file "myposts_mockup.png" to see how edit/delete actions are presented. Provide the Kotlin code to attach an ItemTouchHelper to a RecyclerView to detect swipe-to-delete gestures, or generate the XML overlay for the buttons shown in the mockup.

🧑‍💻 Member B: External APIs & Sync Architecture
Task 4B: Google Books API (Retrofit)

Action: Set up Retrofit Client, Gson converter, and the API Interface to search for books by ISBN or title.

AI Prompt Concept: Generate a Retrofit interface and NetworkClient object in Kotlin to query the Google Books API. Provide the data classes matching the Google Books JSON response to extract the book title, author, description, and thumbnail URL.

Task 5B: Room 'Book' Entity & DAO

Action: Create Book Entity (id, title, author, description, coverUrl, ownerId, lastUpdated). Create BookDao with INSERT, UPDATE, DELETE, and SELECT operations returning LiveData<List<Book>>.

AI Prompt Concept: Create a Room Entity for a Book. Include a Long field for 'lastUpdated'. Create a Room DAO returning LiveData for fetching all books, and fetching books by a specific ownerId.

Task 6B: Firebase Sync Engine (The Delta Fetch)

Action: Implement the core course requirement in BookRepository: Query Firebase for books where lastUpdated > local database timestamp. Insert fetched items into Room.

AI Prompt Concept: Write a Kotlin repository function that queries Firebase Realtime Database for records where 'lastUpdated' is greater than a provided timestamp. Parse the results and insert them into a Room DAO.

🏃 Sprint 3: Integration & ViewModels (Collaborative & Final Wiring)
At this stage, both members merge their tracks. The ViewModels will act as the bridge between Member A's UI and Member B's Repositories.

🧑‍💻 Member A: UI Logic & Data Binding
Task 7A: Add/Edit Book Screen Logic

Action: Wire the Add Book UI. When the user types an ISBN/Title, trigger the ViewModel to call the Google Books API. Observe the result and auto-fill the Title, Author, and Cover ImageView (via Picasso).

AI Prompt Concept: Write a Kotlin Fragment snippet that observes a LiveData<BookInfo> from a ViewModel. When the LiveData updates, populate two TextViews and load an image URL into an ImageView using Picasso.

Task 8A: Connecting Feed LiveData

Action: In FeedFragment and MyPostsFragment, observe the LiveData<List<Book>> from the ViewModel and submit it to the RecyclerView Adapter.

🧑‍💻 Member B: ViewModels & CRUD Finalization
Task 7B: MainViewModel & AuthViewModel

Action: Build the ViewModels that Member A is observing. Expose LiveData from the Repositories.

AI Prompt Concept: Create a ViewModel in Kotlin that takes a Repository in its constructor. Expose a LiveData list of items from the repository, and create a function to trigger a network refresh using Coroutines.

Task 8B: Post Creation/Editing Flow

Action: Finalize the saveBook() flow in the Repository: Upload local image to Firebase Storage (if custom image selected) -> Save Book object to Room (generates local ID & timestamp) -> Push to Firebase Database -> Update UI state.

AI Prompt Concept: Write a suspend function that first uploads a Bitmap to Firebase Storage, retrieves the URL, attaches it to a Data Class, inserts it into Room, and finally pushes it to Firebase Realtime Database.

📝 Grading Checklist & Final Polish (Both Members)
Offline Check: Turn off device Wi-Fi. Verify the Feed still loads from the Room Database cache.

Sync Check: Turn on Wi-Fi. Add a post from Device 1. Verify Device 2 fetches it using the lastUpdated timestamp logic.

UI/UX: Ensure all image loading utilizes Picasso for memory efficiency and caching.

Navigation: Verify the Back button behaves correctly (e.g., backing out of the Feed closes the app, not returns to the Login screen). Use popUpTo in the NavGraph.