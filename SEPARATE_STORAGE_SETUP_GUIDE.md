# 🔥 Separate Firebase Storage Setup - Images Only (Not Merged)
## Overview
Your app now uses **TWO separate Firebase projects**:
### 1. Main Firebase Project (Current)
- **Firestore Database** - All item data
- **Firebase Auth** - User login & authentication
- **Rules & Settings** - App configuration
### 2. Image Firebase Project (NEW)
- **Cloud Storage** - Images ONLY
- **Completely separate** Firebase account
- **No data mixing**
---
## ✅ STEPS TO SET UP
### Step 1: Create Second Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Create a project"** or **"+"**
3. **Name it**: `BackToYou-Images` (or similar)
4. Complete the setup process
5. Skip Google Analytics (optional)
### Step 2: Enable Cloud Storage in Image Project
1. In your NEW Firebase project
2. Click **"Storage"** in left sidebar
3. Click **"Get Started"**
4. Keep default settings, click **"Done"**
5. Wait for storage to initialize
### Step 3: Get Your Image Project Credentials
In the IMAGE Firebase project:
1. **Click ⚙️ Project Settings** (top right)
2. **Go to "Your apps" section**
3. **Select or create Android app** (if needed)
4. **Download `google-services.json`** for reference
5. **Copy these 4 values:**
| Value | Where to Find | Example |
|-------|---------------|---------|
| **Project ID** | Project Settings → General | `backtoyou-images-12345` |
| **API Key** | google-services.json or Project Settings → API keys | `AIzaSyDc1234567890abcdefg` |
| **App ID** | google-services.json (mobileSdkAppId) | `1:123456789:android:abcdef1234567890` |
| **Storage Bucket** | Project Settings → General or Storage | `backtoyou-images-12345.appspot.com` |
### Step 4: Update PostActivity.java
In your Android project, find this line in `PostActivity.java` (around line 80):
```java
ImageStorageConfig.initializeImageStorage(
    "your-image-project-id",
    "YOUR-API-KEY",
    "1:123456789:android:abc123",
    "your-image-project.appspot.com"
);
```
**Replace with your actual values:**
```java
ImageStorageConfig.initializeImageStorage(
    "backtoyou-images-12345",                      // Your Image Project ID
    "AIzaSyDc1234567890abcdefghijklmnopqrst",     // Your API Key
    "1:123456789012:android:abcdef1234567890",    // Your App ID
    "backtoyou-images-12345.appspot.com"          // Your Storage Bucket
);
```
### Step 5: Set Storage Rules (Image Project)
In your IMAGE Firebase project console:
1. **Click "Storage"**
2. **Go to "Rules" tab**
3. **Delete everything** and paste:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.resource.size < 50 * 1024 * 1024;
      allow delete: if request.auth != null;
    }
  }
}
```
4. **Click "Publish"** (bottom right)
5. **Wait for "✅ Rules updated successfully"**
### Step 6: Update Main Project Storage Rules
Make sure your **MAIN** project also has proper storage rules (if using):
In MAIN Firebase project console:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
### Step 7: Rebuild App
```bash
./gradlew clean
./gradlew build
```
Then **run the app** on emulator/device.
---
## 🧪 Testing
1. **Log into the app**
2. **Go to "Report an Item"**
3. **Click "Select Image"**
4. **Grant storage permission** when prompted
5. **Pick any image** from gallery
6. **Check Logcat** for:
   ```
   ImageStorage: ✅ Image Storage initialized
   ImageStorage: Bucket: backtoyou-images-12345.appspot.com
   Upload: Starting upload for: [uid]_[timestamp].jpg
   Upload: Image URL: gs://backtoyou-images-12345.appspot.com/images/...
   ```
---
## 📊 Architecture
```
┌─────────────────────────────────┐
│   Your Android App              │
│   BackToYou                     │
└────────────┬────────────────────┘
             │
    ┌────────┴───────────┐
    │                    │
    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐
│ MAIN Firebase    │  │ IMAGE Firebase   │
│                  │  │                  │
│ • Firestore DB   │  │ • Cloud Storage  │
│ • Firebase Auth  │  │   (images only)  │
│ • Rules          │  │                  │
│                  │  │                  │
│ Account: main    │  │ Account: images  │
└──────────────────┘  └──────────────────┘
```
---
## ✨ Key Points
✅ **Two separate projects** - No data mixing
✅ **Separate credentials** - Each project independent
✅ **Auth uses main project** - Users login with main project credentials
✅ **Images upload to image project** - Using separate storage
✅ **Download URLs stored in main Firestore** - Reference to image storage
✅ **Easy to manage** - Scale storage independently from data
---
## 🔍 Troubleshooting
### "Image Storage not initialized"
- Make sure you filled in the 4 values in PostActivity.onCreate()
- Check Project ID, API Key, App ID, Storage Bucket are correct
- Rebuild the app
### "Upload fails - object does not exist"
- Verify **Image project Storage Rules are published**
- Check user is logged in
- Grant storage permission to app
- Check network is connected
### "Permission denied" error
- Verify **Image project Storage Rules allow authenticated users**
- Ensure rules have `request.auth != null`
- Republish rules after editing
### Images not uploading
- Check Logcat for "ImageStorage" tag
- Verify all 4 credentials in PostActivity
- Make sure Image project has Storage enabled
- Try with test mode rules (temporary):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write;
    }
  }
}
```
---
## 📁 Files Created/Modified
| File | Status | Purpose |
|------|--------|---------|
| `ImageStorageConfig.java` | ✅ CREATED | Manages separate storage instance |
| `PostActivity.java` | ✅ UPDATED | Uses ImageStorageConfig |
| `storage.rules` | 📝 NEW | Storage rules for image project |
| `firestore.rules` | 📝 EXISTING | Firestore rules (main project) |
---
## ✅ Checklist
- [ ] Created second Firebase project (BackToYou-Images)
- [ ] Enabled Cloud Storage in image project
- [ ] Got all 4 credentials (Project ID, API Key, App ID, Bucket)
- [ ] Updated PostActivity.java with credentials
- [ ] Published Storage Rules in image project
- [ ] Rebuilt app (`./gradlew clean build`)
- [ ] Tested image upload
- [ ] Verified images appear in image project Storage console
---
**All set! Your images are now stored in a completely separate Firebase project!** 🎉