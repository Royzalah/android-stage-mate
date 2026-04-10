<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="StageMate Logo" width="120"/>

# StageMate

**A comprehensive event ticketing and discovery platform for Android, built for the Israeli market.**

Discover live events · Pick your perfect seats · Purchase tickets — all in one app.

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Firebase-Backend-orange?logo=firebase&logoColor=white)](https://firebase.google.com)
[![Material 3](https://img.shields.io/badge/Material%20Design-3-purple?logo=material-design&logoColor=white)](https://m3.material.io)

</div>

---

## About The Project

Developed as a final project for the **"User Interface Development"** course, as part of a Computer Science BSc.

StageMate brings the full event experience to your pocket. Discover events happening around you, pick your perfect seats on an interactive venue map, purchase tickets, and manage everything in one place. From finding a trending concert to getting a QR-coded ticket — StageMate handles it all.

## Demo

<!-- Add a demo video or GIF here -->
<!-- Example: -->
<!-- https://github.com/user-attachments/assets/your-video-id -->

> Demo video coming soon.

## Key Features

- **Event Discovery** — Browse hot, recommended, and trending events with location-aware filtering
- **Smart Search** — Filter by category, city, date, and free text
- **Interactive Seat Selection** — Choose seats on venue-specific maps (Stadium, Theater, Arena) with real-time availability
- **Ticket Management** — View tickets with QR codes, booking references, and status tracking
- **Favorites & History** — Save events and revisit recently viewed ones
- **Push Notifications** — Trending event alerts and reminders via Firebase Cloud Messaging
- **Calendar Sync** — Add events directly to your Google Calendar
- **Wide Event Variety** — Concerts, stand-up comedy, theater, kids shows, sports, festivals, and more
- **Email Receipts** — Receive a purchase receipt directly to your email after buying tickets
- **Deep Linking** — Share and open events via `stagemate://` links

## Architecture

```
app/src/main/java/com/roei/stagemate/
├── data/
│   ├── models/          # Event, User, Ticket, Seat, Venue, Category...
│   ├── interfaces/      # Callback interfaces for adapters
│   └── repository/      # DataRepository (Firebase wrapper with local fallback)
├── ui/
│   ├── activities/      # 16 screens (Login, Payment, Seat Selection...)
│   ├── fragments/       # 7 main fragments (Home, Search, Tickets, Profile...)
│   ├── adapters/        # 10 RecyclerView adapters
│   ├── dialogs/         # Rating, Trending Event, QR Code dialogs
│   └── views/           # Custom venue map views (Stadium, Theater, Arena)
├── services/            # FCM messaging, event reminder receiver
├── utilities/           # Firebase, Location, QR, image loading, formatting
└── MyApp.kt             # Application entry point
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Material Design 3, ConstraintLayout, Lottie Animations |
| Navigation | Jetpack Navigation Component |
| Backend | Firebase (Firestore, Realtime DB, Storage, FCM, Analytics, Crashlytics) |
| Auth | Firebase Auth (Email + Google SSO via FirebaseUI) |
| Maps | Google Maps SDK |
| Images | Glide |
| QR Codes | ZXing |
| Serialization | Gson |

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android 8.0+ (API 26)
- Google Play Services

### Installation

1. Clone the repo
   ```bash
   git clone https://github.com/Royzalah/android-stage-mate.git
   ```
2. Set up Firebase
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Email and Google sign-in) and Firestore
   - Download `google-services.json` and place it in the `app/` folder
3. Open the project in Android Studio
4. Run on an emulator or physical device (API 26+)

## Usage

1. **Sign up or log in** with email or your Google account
2. **Choose your preferences** — pick favorite categories during onboarding
3. **Discover events** — browse the home screen for hot, recommended, and trending events
4. **Search & filter** — find events by name, category, or city
5. **Pick your seats** — tap an event, view the venue map, and select your preferred seats
6. **Purchase tickets** — complete the checkout flow with order summary and payment
7. **Manage your tickets** — view all tickets with QR codes in the Tickets tab
8. **Stay updated** — receive push notifications about trending events and reminders

## License

This project was built as a final project for the **User Interface Development** course.
