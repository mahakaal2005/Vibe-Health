# Firebase Setup Instructions

## Prerequisites
1. Create a Firebase project at https://console.firebase.google.com/
2. Enable Authentication with Email/Password provider
3. Set up Firestore Database
4. Configure project for asia-south1 (Mumbai) region

## Setup Steps

### 1. Firebase Project Configuration
1. Go to Firebase Console
2. Create new project named "Vibe Health"
3. Select asia-south1 (Mumbai) as the default region
4. Enable Google Analytics (optional)

### 2. Add Android App
1. Click "Add app" and select Android
2. Package name: `com.vibehealth.android`
3. App nickname: "Vibe Health Android"
4. Download the `google-services.json` file
5. Replace the placeholder `app/google-services.json` with the downloaded file

### 3. Enable Authentication
1. Go to Authentication > Sign-in method
2. Enable Email/Password provider
3. Configure authorized domains if needed

### 4. Set up Firestore
1. Go to Firestore Database
2. Create database in production mode
3. Set location to asia-south1 (Mumbai)
4. Configure security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read and write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Users can read and write their own prescriptions
    match /prescriptions/{prescriptionId} {
      allow read, write: if request.auth != null && 
        resource.data.uid == request.auth.uid;
    }
  }
}
```

### 5. Enable Crashlytics (Optional)
1. Go to Crashlytics in Firebase Console
2. Follow setup instructions
3. Enable crash reporting

## Important Notes
- The current `google-services.json` is a placeholder
- Replace it with your actual Firebase configuration
- Ensure the package name matches: `com.vibehealth.android`
- Configure Firebase Authentication for asia-south1 region
- Set up proper security rules for production use

## Testing
After setup, you can test the Firebase connection by running the app and checking the Firebase Console for connection logs.