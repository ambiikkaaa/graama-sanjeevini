# Grama-Sanjeevini

A rural healthcare pharmacy network app for Android.

## Features

- Medicine search for villagers
- Location-based filtering (within 10-20 km)
- Pharmacist login and inventory management
- Shared inventory using Firebase Firestore
- Expiry alerts
- Emergency stock view for life-saving drugs

## Setup Instructions

1. **Firebase Setup:**
   - Create a Firebase project at https://console.firebase.google.com/
   - Enable Firestore and Authentication
   - Download `google-services.json` and place it in `app/` directory
   - Update the API key in `AndroidManifest.xml`

2. **Google Maps API:**
   - Enable Google Maps API in Google Cloud Console
   - Get API key and replace `YOUR_API_KEY_HERE` in `AndroidManifest.xml`

3. **Build the App:**
   - Open the project in Android Studio
   - Sync Gradle files
   - Build and run on device/emulator

## Usage

- **Villagers:** Search for medicines, view nearby availability
- **Pharmacists:** Login to manage inventory, add medicines, update stock

## Tech Stack

- Kotlin
- Firebase Firestore
- Firebase Auth
- Google Maps/Location API