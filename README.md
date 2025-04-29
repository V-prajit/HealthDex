# HealthDex

**Personal Health Management System**

A full-stack application composed of an Android client and a Ktor backend server to help users track vital signs, manage medications, diet, appointments, and receive real-time alerts.

---

## Table of Contents

1. [Overview](#overview)  
2. [Project Structure](#project-structure)  
3. [Prerequisites](#prerequisites)  
4. [Setup Instructions](#setup-instructions)  
   - [1. Clone the Repository](#1-clone-the-repository)  
   - [2. Firebase Setup](#2-firebase-setup)  
     - [Android App Setup](#android-app-setup)  
     - [Backend Service Account](#backend-service-account)  
   - [3. Backend Setup (PHMS-Backend)](#3-backend-setup-phms-backend)  
     - [Environment Variables (`.env`)](#environment-variables-env)  
     - [Generating `GMAIL_APP_PASSWORD`](#generating-gmail_app_password)  
     - [Build and Run](#build-and-run)  
   - [4. Android Setup (PHMS-Android)](#4-android-setup-phms-android)  
     - [Configure API Keys (`local.properties`)](#configure-api-keys-localproperties)  
     - [Build and Run](#build-and-run-1)  
   - [5. Language Translator (LibreTranslate) Setup](#5-language-translator-libretranslate-setup)  
   - [6. Emulator Setup for Biometrics](#6-emulator-setup-for-biometrics)  
5. [Running the Application](#running-the-application)  
6. [License](#license)  

---

## Overview

HealthDex is a robust Personal Health Management System (PHMS) that enables users to:

- **Track vital signs**: Heart rate, blood pressure, glucose, cholesterol, etc.  
- **Receive alerts**: Email and in-app notifications for abnormal readings.  
- **Manage health data**: Medications, diet logs, appointments, and custom notes.  
- **Secure authentication**: Firebase Authentication with biometric (fingerprint) login support.  
- **Multi-language support**: Powered by LibreTranslate for on-device translation.

---

## Project Structure

```
HealthDex/
├── PHMS-Android/             # Android application code (Jetpack Compose)
├── PHMS-Backend/             # Ktor backend server code
├── Supporting/  
│   └── Language Translator/  # LibreTranslate integration scripts and docs
├── LICENSE.md                # License information
└── README.md                 # Project documentation (this file)
```

---

## Prerequisites

Before you begin, make sure you have the following installed:

- **Git**: Version control system  
- **Java Development Kit (JDK) 11+**: Verify by running:
  ```bash
  java -version
  ```
- **Android Studio**: Official IDE for Android development  
- **Docker** (optional): For setting up LibreTranslate locally

---

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/V-prajit/HealthDex
cd HealthDex
```



### 2. Firebase Setup

This project uses Firebase for authentication and backend services.

#### Android App Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.  
2. Add an Android app with package name `com.example.phms`.  
3. Download the generated `google-services.json` file.  
4. Place it in:
   ```
   HealthDex/PHMS-Android/app/google-services.json
   ```

#### Backend Service Account

1. In Firebase Console > Project Settings > Service Accounts, generate a new private key.  
2. Download and rename it to `serviceAccountKey.json`.  
3. Place it in:
   ```
   HealthDex/PHMS-Backend/src/main/resources/serviceAccountKey.json
   ```

### 3. Backend Setup (PHMS-Backend)

Navigate to the backend directory:

```bash
cd HealthDex/PHMS-Backend
```

#### Environment Variables (`.env`)

Create a file named `.env` in the `PHMS-Backend` directory with:

```env
GMAIL_EMAIL=your_gmail_address@gmail.com
GMAIL_APP_PASSWORD=your_16_digit_app_password
```

#### Generating `GMAIL_APP_PASSWORD`

1. Go to [Google Account Security](https://myaccount.google.com/security).  
2. Enable **2-Step Verification**.  
3. Under **Signing in to Google**, click **App passwords**.  
4. Select **Mail** as the app.  
5. Choose **Other (Custom name)** and enter `PHMS Backend`.  
6. Click **Generate** and copy the 16-character password (no spaces) into your `.env`.

#### Build and Run

```bash
# macOS/Linux
./gradlew run
# Windows
gradlew.bat run
```

The server starts on `http://0.0.0.0:8085` by default.

### 4. Android Setup (PHMS-Android)

1. Open Android Studio and select **Open an existing project**.  
2. Navigate to `HealthDex/PHMS-Android` and open it.

#### Configure API Keys (`local.properties`)

In the `PHMS-Android` root (alongside `build.gradle.kts`), create `local.properties`:

```properties
# API Keys
openai.api.key=YOUR_OPENAI_API_KEY
FDC_API_KEY=YOUR_FDC_API_KEY

# SDK location (auto-added by Android Studio)
sdk.dir=/path/to/android/sdk
```

> **Note:** Features depending on OpenAI or FDC keys will not work without valid entries.

#### Build and Run

- Sync Gradle.  
- Select an emulator or connected device.  
- Click **Run 'app'** (green ▶️) in the toolbar.

### 5. Language Translator (LibreTranslate) Setup

HealthDex includes multi-language support via LibreTranslate. Follow these steps to run it locally:

1. **Clone LibreTranslate**:
   ```bash
   git clone https://github.com/LibreTranslate/LibreTranslate.git
   cd LibreTranslate
   ```
2. **Run via Docker** (recommended):
   ```bash
   docker-compose up -d
   ```
3. **API Endpoint**  
   By default, LibreTranslate runs at `http://localhost:5000`.  
   HealthDex will call this endpoint for translation services.

For more details, visit the [LibreTranslate GitHub repo](https://github.com/LibreTranslate/LibreTranslate).

### 6. Emulator Setup for Biometrics

To test fingerprint login on an Android Virtual Device (AVD):

1. Launch your AVD.  
2. In the emulator, go to **Settings > Security > Fingerprint**, and add a fingerprint.  
3. When the app prompts for a fingerprint, open **Extended Controls** (`⋯`), select **Fingerprint**, and tap **Touch sensor** to simulate.

---

## Running the Application

```bash
# Start the backend server
cd HealthDex/PHMS-Backend
./gradlew run

# In a new terminal, start LibreTranslate (if using locally)
cd HealthDex/Supporting/Language Translator/LibreTranslate
docker-compose up -d

# In Android Studio, open and run the Android app
```

1. Register/log in via Firebase; biometric login and multi-language translation will now work.  
2. Begin tracking vital signs and managing your health data in your preferred language.

---

## License

This project is licensed under the terms in `LICENSE.md`. Please review for details.
