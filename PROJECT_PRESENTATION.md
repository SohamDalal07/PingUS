# BackToYou Project Presentation

## 1) Project Overview

**BackToYou** is an Android app for reporting lost/found items and securely claiming ownership.

Core goals:
- Allow users to post lost/found items quickly.
- Let claimers answer security questions to prove ownership.
- Let posters review claims and approve/reject safely.
- Enable direct contact (call/email/WhatsApp) between matched users.

Tech stack:
- **Android (Java)**
- **Firebase Auth**
- **Cloud Firestore** (named instance: `lf26`)
- **Firebase Storage** (item image uploads)
- **Glide** for image loading

---

## 2) High-Level Architecture

Architecture style is Activity + Fragment + Adapter driven:
- UI and data fetching are mostly in Activities/Fragment.
- Firestore is read/written directly from screen classes.
- Navigation is intent-based (no navigation graph).

Main modules:
- **Authentication**: login/sign-up and user profile creation.
- **Home Feed**: browse/search/filter posted items.
- **Post Item**: create LOST/FOUND posts with security answers.
- **Item Detail + Claim**: submit claim answers and track status.
- **Alerts**: approve/reject pending claims and view history.
- **Profile**: view/edit account details and sign out.

---

## 3) End-to-End User Flow

1. User signs in / signs up.
2. User posts a lost/found item (with security answers; optional image for lost).
3. Another user opens item details and submits a claim (answers 3 questions).
4. Poster sees pending claim in Alerts > Current Processing.
5. Poster approves/rejects.
6. If approved:
   - Item type is marked as `CLAIMED`.
   - Users can contact each other through call/email/WhatsApp.

---

## 4) Main Java Files (Your Core Logic Files)

### `app/src/main/java/com/example/backtoyou/Login_page.java`
- Handles both **sign-in** and **sign-up**.
- Creates Firebase Auth account.
- Writes initial user profile to Firestore `users`.

### `app/src/main/java/com/example/backtoyou/Home.java`
- Main host screen.
- Handles toolbar menu and bottom navigation.
- Loads `HomeFragment`.
- Alerts bell state updates based on pending claims.
- `Settings` menu now routes to `ProfileActivity`.

### `app/src/main/java/com/example/backtoyou/HomeFragment.java`
- Fetches and renders item feed from Firestore `items`.
- Supports search/filter chips.
- Displays list using `FeedCardAdapter`.
- Opens item details on selection.

### `app/src/main/java/com/example/backtoyou/PostActivity.java`
- Creates LOST/FOUND posts.
- Collects security question answers.
- Uploads image to Firebase Storage (for lost items).
- Saves item to Firestore `items`.

### `app/src/main/java/com/example/backtoyou/ItemDetailActivity.java`
- Shows full item details.
- Lets non-poster users submit claims.
- Saves claim answers to Firestore `claims`.
- Shows claim status (`PENDING`/`APPROVED`/`REJECTED`).

### `app/src/main/java/com/example/backtoyou/AlertsActivity.java`
- Listens to claims in real-time.
- Shows **Current Processing** pending request.
- Displays claimer security answers.
- Prioritizes uploaded claim image over pre-registered image.
- Approve/reject flow updates claim and item state.
- Contact buttons (call/mail/WhatsApp) fetch and use other user contact details.

### `app/src/main/java/com/example/backtoyou/ProfileActivity.java`
- Loads profile from Firestore `users`.
- Handles fallback from auth if profile is missing.
- Supports phone number update.
- Supports sign out.

### `app/src/main/java/com/example/backtoyou/AlertAdapter.java`
- Binds claim history list rows.
- Shows claim answer summary and status text.
- Loads item images/category art.

### `app/src/main/java/com/example/backtoyou/FeedCardAdapter.java`
- Binds home feed cards.
- Renders item image/title/status/meta/action states.

### `app/src/main/java/com/example/backtoyou/UserProfile.java`
- Data model for profile information.

### `app/src/main/java/com/example/backtoyou/CategoryDrawableHelper.java`
- Utility to map category names to drawable resources.

---

## 5) Main XML Files (Your Main UI Files)

### Screen Layout XML
- `app/src/main/res/layout/activity_login_page.xml`  
  Login/sign-up UI.

- `app/src/main/res/layout/activity_home.xml`  
  Home shell (toolbar + fragment container + bottom nav).

- `app/src/main/res/layout/fragment_home.xml`  
  Feed UI (stats, search, chips, list).

- `app/src/main/res/layout/activity_post.xml`  
  Post form and upload section.

- `app/src/main/res/layout/activity_item_detail.xml`  
  Item detail screen and claim/contact action area.

- `app/src/main/res/layout/activity_alerts.xml`  
  Alerts dashboard, current processing card, history list.

- `app/src/main/res/layout/activity_profile.xml`  
  Profile page and account details.

### Dialog XML
- `app/src/main/res/layout/dialog_security_questions.xml`  
  Security questions during posting.

- `app/src/main/res/layout/dialog_claim.xml`  
  Security questions during claim submission.

### Row/Card XML
- `app/src/main/res/layout/item_home_feed_card.xml`  
  Feed item card.

- `app/src/main/res/layout/item_alert_card.xml`  
  Alert/claim history card.

### Menu XML
- `app/src/main/res/menu/home.xml`  
  Bottom navigation items.

- `app/src/main/res/menu/top_menu.xml`  
  Toolbar actions (`Alerts`, `Settings`).

---

## 6) Firebase Data Model (Practical View)

### Collection: `users`
Common fields:
- `uid`
- `fullName`
- `email`
- `studentId`
- `role`
- `department`
- `phone`

### Collection: `items`
Common fields:
- `title`
- `type` (`LOST` / `FOUND` / `CLAIMED`)
- `category`
- `location`
- `description`
- `postedByUid`
- `postedByName`
- `postedAt`
- `status`
- `securityColor`
- `securityBrand`
- `securityMark`
- `photoUrl`

### Collection: `claims`
Common fields:
- `itemId`
- `itemTitle`
- `claimerUid`
- `claimerName`
- `posterUid`
- `colorAns`
- `brandAns`
- `markAns`
- `status` (`PENDING` / `APPROVED` / `REJECTED`)
- `timestamp`
- optional image URL fields for claim upload

---

## 7) Strengths of Current Project

- Clear user journey from post to claim to approval.
- Real-time Firestore listeners for alerts and status.
- Practical security-question based claim verification.
- Simple and understandable codebase for iterative growth.

---

## 8) Improvement Opportunities (Recommended Roadmap)

1. Move to MVVM + Repository pattern for cleaner separation.
2. Centralize Firestore field constants to avoid typo bugs.
3. Add Firestore rules hardening (`firestore.rules` currently permissive).
4. Add automated tests for claim approval and contact flows.
5. Add loading/error states consistently for all async operations.
6. Introduce dependency injection (e.g., Hilt) for scalability.

---

## 9) Suggested Presentation Talk Track (5–7 Minutes)

Slide flow suggestion:
1. Problem statement + app purpose
2. Architecture diagram (Activity/Fragment + Firebase)
3. Main user journeys (Post -> Claim -> Approve)
4. Core Java files and responsibilities
5. Core XML/UI structure
6. Data model (`users`, `items`, `claims`)
7. Current achievements + next roadmap

---

## 10) Download / Share

This presentation doc is saved at:

- `D:/BackToYou/PROJECT_PRESENTATION.md`

You can:
- Open directly in Cursor/VS Code
- Export to PDF
- Convert to slides (Google Slides / PowerPoint) using this as script content

