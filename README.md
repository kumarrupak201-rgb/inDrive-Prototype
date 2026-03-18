# inDrive-Prototype

A prototype Android application built for M.Tech thesis research at **IIT Kanpur** (Civil Engineering) to study **driver distraction** caused by bidding-based ride-hailing apps using a driving simulator.

---

## Research Context

This app replicates the **InDrive driver-side interface** — where a driver receives a ride request, sees the passenger's offered fare, and can accept, reject, or place a counter offer. The goal is to measure how the bidding interaction distracts a driver compared to non-bidding apps (Ola/Uber).

Experiments are conducted on a **Tecknotrove driving simulator** at IIT Kanpur.

---

## App Flow

```
Admin sends ride
      ↓
Driver receives notification + 10-second countdown
      ↓
      ├── ACCEPT  → ride logged, back to idle
      ├── REJECT  → ride logged, back to idle
      ├── TIMEOUT → auto-logged after 10 seconds
      └── COUNTER OFFER (select fare)
                ↓
         7-second waiting screen
                ↓
         "Passenger accepted!" banner (2 sec)
                ↓
              Idle
```

---

## Screens

### Driver Mode

| Screen | Description |
|--------|-------------|
| **Idle** | "Waiting for ride requests..." on map background |
| **Ride Incoming** | Ride card with passenger info, fare, route, countdown ring |
| **Waiting Offer** | 7-sec countdown after counter offer sent |
| **Offer Accepted** | Green banner showing accepted fare for 2 seconds |

**Ride Card contains:**
- Passenger name and rating
- Pickup location + address (green dot)
- Drop location + address (red dot)
- Fare (Rs.) and distance (km)
- 3 counter offer buttons (distance-based fares)
- ACCEPT button (green)
- REJECT button (red)
- Countdown ring (top-right) — green → yellow → red

### Admin Mode (Researcher)

- Send pre-made rides (5 fixed Kanpur routes, cycled sequentially)
- Cancel active ride
- Toggle auto-generation of rides
- Export all timestamp data as CSV

---

## Pre-made Routes (Kanpur)

| # | Pickup | Drop | Distance |
|---|--------|------|----------|
| 1 | Z Square Mall | Kanpur Central | 4.2 km |
| 2 | Nana Rao Park | JK Temple | 6.8 km |
| 3 | Green Park Stadium | IIT Kanpur | 8.5 km |
| 4 | Moti Jheel | Allen Forest Zoo | 5.3 km |
| 5 | Ghanta Ghar | Rave 3 Mall | 7.1 km |

---

## Fare Logic

- **Passenger fare:** `Rs. 40 + (distance × 22)`
- **Counter offer options:** 3 distance-based alternatives shown to driver

---

## Timings

| Constant | Value | Purpose |
|----------|-------|---------|
| `RIDE_TIMEOUT_SEC` | 10 sec | Ride card auto-dismiss |
| `WAITING_DURATION_SEC` | 7 sec | Passenger "response" after counter offer |
| `ACCEPTED_SHOW_SEC` | 2 sec | Accepted banner display duration |

---

## Timestamp Logging (CSV)

Every ride interaction is recorded with **IST timestamp (microsecond precision)** using `System.nanoTime()`.

| Column | Description |
|--------|-------------|
| `RideID` | Unique ride number |
| `AdminClickTime` | When researcher triggered the ride |
| `DriverReceiveTime` | When ride appeared on driver screen |
| `DriverResponseTime` | When driver responded |
| `ResponseType` | `ACCEPTED` / `REJECTED` / `OFFER` / `TIMEOUT` |
| `OfferAmount` | Counter offer value in Rs. (blank if not applicable) |

**Export:** Admin Mode → "Export CSV" button → saved to `Downloads/indrive_timestamps_<unix_ms>.csv`

**Reaction time** = `DriverResponseTime − DriverReceiveTime` (calculate in Excel/Python post-export)

---

## Mode Switching

| Action | How |
|--------|-----|
| Driver → Admin | Tap **play icon** (top-left of app bar) |
| Admin → Driver | Tap **"DRIVER"** text button (top-right of app bar) |

---

## Tech Stack

| Item | Detail |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Audio | `ToneGenerator` (TONE_PROP_BEEP) |
| Storage | `Environment.DIRECTORY_DOWNLOADS` |
| Min SDK | 24 |
| Target SDK | 34 |

---

## Project Structure

```
app/src/main/java/com/example/ugp/
└── MainActivity.kt          ← entire app (single file)

app/src/main/res/drawable/
├── map_ride_1.png           ← route map backgrounds
├── map_ride_2.png
├── map_ride_3.png
├── map_ride_4.png
└── map_ride_5.png
```

---

## How to Run

```bash
git clone https://github.com/kumarrupak201-rgb/inDrive-Prototype.git
```

1. Open in **Android Studio**
2. Connect Android device (API 24+)
3. Click **Run**

---

## Research Team

- **Researcher:** Rupak Kumar — M.Tech, Transportation Engineering, IIT Kanpur
- **Advisors:** Prof. Pranamesh Chakraborty & Prof. Aditya Medury
- **App Developer:** Rupak kumar

**Thesis:** CE698/CE699 — Driver distraction comparison between bidding and non-bidding ride-hailing apps using driving simulator.
