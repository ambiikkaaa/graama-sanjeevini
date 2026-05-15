# Grama-Sanjeevini – Rural Pharmacy Network

## Overview
Grama-Sanjeevini is an Android-based rural healthcare application that helps users locate nearby pharmacies with available medicines in real time. The application uses GPS location services, Firebase backend integration, and Google Maps navigation to improve medicine accessibility in rural areas.

---

## Features

- Real-time medicine availability tracking
- GPS-based nearby pharmacy search
- Distance calculation in kilometers
- Google Maps route navigation
- Radius filtering (10KM / 20KM)
- Emergency/Life-saving medicine highlighting
- Pharmacist inventory management
- Expiry alert and discount system
- Firebase real-time database synchronization

---

## Technologies Used

- Kotlin
- XML
- Material Design 3
- Firebase Authentication
- Cloud Firestore
- Google Maps SDK
- Fused Location Provider API
- Android Studio

---

## System Modules

### User Module
Users can search medicines, view nearby pharmacies, filter results by distance, and navigate to pharmacies using Google Maps.

### Pharmacist Module
Pharmacists can register stores, pin pharmacy locations, add medicine stock, update inventory, and manage expiry information.

---

## Workflow

### Pharmacist Workflow
Register → Pin Store Location → Login → Add Medicine Stock → Update Inventory

### User Workflow
Open App → Grant Location Permission → Search Medicine → View Nearby Pharmacies → Navigate Using Google Maps

---

## Firebase Integration

- Firebase Authentication for secure login/signup
- Cloud Firestore for real-time medicine and pharmacy data storage
- Firebase BOM for dependency management

---

## Location & Navigation

The application uses:
- Fused Location Provider API for accurate GPS coordinates
- Google Maps SDK for map display
- Google Navigation Intent for route guidance

---

## Results

### App link: https://drive.google.com/file/d/1B566wI0KrU9aNxg5O3rcl98Gn8j-agep/view?usp=drivesdk

### Authentication Screens
<img src="images/Home page and Authentication.jpg" width="800"/>

The application provides secure pharmacist registration and login using Firebase Authentication with map-based pharmacy location pinning.

---

### Pharmacist Inventory Dashboard
<img src="images/Stock update.jpg" width="800"/>

Pharmacists can manage medicine stock, track near-expiry medicines, add discounts, and highlight life-saving medicines.

---

### User Medicine Search Interface
<img src="images/Medicine search.jpg" width="800"/>

This screen allows users to search medicines, apply distance filters, view nearby pharmacies, and navigate directly using Google Maps.

---

## Conclusion
Grama-Sanjeevini successfully digitizes the rural pharmacy experience. By combining real-time inventory management with precise GPS-based navigation, it creates a reliable healthcare safety net for village communities.
