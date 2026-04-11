
<a id="readme-top"></a>

<h1 align="center">
  <a href="https://github.com/Royzalah/android-stage-mate"><img src="https://github.com/user-attachments/assets/22b24069-e2d2-40b4-9501-ad5602386358" alt="StageMate Logo" width="200"></a>
  <br>
  StageMate
  <br>
</h1>


<p align="center">
	<b>Discover live events, pick your seats, and buy tickets — all in one Android app built for the Israeli market.</b>
</p>
<p align="center">
	<a href="https://github.com/Royzalah/android-stage-mate/issues">Report Bug</a>
	•
	<a href="https://github.com/Royzalah/android-stage-mate/issues">Request Feature</a>
	•
	<a href="#5-demonstration">View Demo</a>
</p>

---

<p align="center">
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Firebase-FFCA28?style=flat&logo=firebase&logoColor=black" alt="Firebase" />
    <img src="https://img.shields.io/badge/Material%203-757575?style=flat&logo=materialdesign&logoColor=white" alt="Material 3" />
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
</p>

## 1 About The Project
StageMate is a comprehensive event ticketing and discovery platform for Android. It brings the full event experience to your pocket — discover events around you, pick seats on an interactive venue map, purchase tickets via a multi-step checkout, and manage everything from trending concerts to QR-coded tickets in one place.

The project was developed as part of the 'User Interface Development' course in my Computer Science BSc at Afeka College of Engineering.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 2 Key Features
- **Event Discovery**: Hot, recommended, and trending events with GPS-aware "Near Me" filtering (Haversine, 30km radius).
- **Smart Search & Filtering**: Filter by category, city, or free text with adaptive debouncing.
- **Interactive Seat Selection**: Venue-specific maps for Bloomfield Stadium, Menora Arena, and theaters with real-time availability.
- **End-to-End Ticketing**: Multi-step payment wizard, QR-coded tickets, and booking references.
- **Favorites & History**: Save events and revisit recently viewed ones.
- **Push Notifications**: Trending alerts and event reminders via Firebase Cloud Messaging.
- **Calendar Sync**: Add purchased events directly to Google Calendar.
- **Deep Linking**: Share events via `stagemate://` links.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 3 Architecture
Callback-based layered architecture (no ViewModel/LiveData) following course requirements. Fragments route through `DataRepository`, never calling `FirebaseManager` directly. Fragment-to-Activity communication is via Interface Callbacks.

```
app/src/main/java/com/roei/stagemate/
├── data/
│   ├── models/            # Event, User, Ticket, Seat, Venue, Category, PricingTier
│   ├── interfaces/        # Callback interfaces (Event, Ticket, Category, Notification)
│   └── repository/        # DataRepository — single source of truth
├── ui/
│   ├── activities/        # Splash, Login, SignUp, Main, EventDetail, SeatSelection, Payment, Receipt
│   ├── fragments/         # Home, Search, Tickets, Profile, Notifications, Payment wizard
│   ├── adapters/          # RecyclerView adapters
│   ├── dialogs/           # Rating, Trending Event, QR Code
│   └── views/             # BaseVenueMapView, StadiumMapView, TheaterMapView, ArenaMapView
├── services/              # FCM messaging, event reminder receiver
├── utilities/             # FirebaseManager, LocationManager, ImageLoader, QRCodeManager
└── MyApp.kt
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 4 Classes Overview

| Class Name          | Layer/Type      | Description                                                               |
| :------------------ | :-------------- | :------------------------------------------------------------------------ |
| `DataRepository`    | **Repository**  | Single entry point for data ops; fragments never call Firebase directly.  |
| `FirebaseManager`   | **Manager**     | Wraps Firestore/Auth; UUIDs generated locally before `.document().set()`. |
| `MainActivity`      | **Activity**    | Hosts the 4 bottom-nav tabs (Home, Search, Tickets, Profile).             |
| `BaseVenueMapView`  | **Custom View** | Base for interactive venue maps; subclassed per venue type.               |
| `PaymentActivity`   | **Activity**    | Multi-step checkout via Navigation Component internal to the activity.    |
| `LocationManager`   | **Manager**     | `FusedLocationProviderClient` wrapper for Near Me filtering.              |
| `ImageLoader`       | **Wrapper**     | Glide singleton wrapper for consistent image loading across the app.      |
| `QRCodeManager`     | **Manager**     | ZXing wrapper generating QR codes for ticket validation.                  |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 5 Demonstration
> Screenshots and demo video coming soon.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 6 Tech Stack

| Layer         | Technology                                                              |
| :------------ | :---------------------------------------------------------------------- |
| Language      | Kotlin                                                                  |
| UI            | Material Design 3, ConstraintLayout, ViewBinding, Lottie                |
| Navigation    | Jetpack Navigation Component                                            |
| Backend       | Firebase (Firestore, Storage, FCM, Analytics, Crashlytics)              |
| Auth          | Firebase Auth (Email + Google SSO via FirebaseUI)                       |
| Location      | Google Play Services — FusedLocationProviderClient                      |
| Images        | Glide (via `ImageLoader` wrapper)                                       |
| QR Codes      | ZXing                                                                   |
| Serialization | Gson                                                                    |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 7 Getting Started

### 7.1 Prerequisites
*   **Android Studio**: Hedgehog or newer (https://developer.android.com/studio)
*   **Kotlin**: Version 1.9+
*   **Min SDK**: 26 (Android 8.0)
*   **Firebase project** with Auth, Firestore, Storage, FCM, Analytics, and Crashlytics enabled

### 7.2 Installation
1. Clone the repo
```
git clone https://github.com/Royzalah/android-stage-mate.git
```
2. Set up Firebase — create a project, enable Auth (Email + Google) and Firestore, then drop `google-services.json` into `app/`.
3. Open the project in Android Studio, sync Gradle, and run on a physical device or emulator (API 26+).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 8 Usage

*   **Sign up or log in** with email or Google SSO.
*   **Choose preferred categories** during onboarding.
*   **Discover events** — browse hot, recommended, and trending sections.
*   **Search & filter** by name, category, or city.
*   **Pick seats** on the interactive venue map (or General Admission for festivals).
*   **Purchase** through the multi-step checkout wizard.
*   **Manage tickets** with QR codes in the Tickets tab.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 9 Roadmap

- [ ] Real-time Firestore snapshot listeners for live seat availability
- [ ] In-app event reviews and ratings
- [ ] Dark theme polish pass
- [ ] Google Wallet pass export

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 10 Contributors

<div align="center">
 <a href="https://github.com/Royzalah/android-stage-mate/graphs/contributors">
 <img src="https://contrib.rocks/image?repo=Royzalah/android-stage-mate" alt="contrib.rocks image" />
 </a>
 </br>
 Roei Zalah
</div>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## 11 License

Built as a final project for the **User Interface Development** course at Afeka College of Engineering. Distributed under the MIT License.

<p align="right">(<a href="#readme-top">back to top</a>)</p>
