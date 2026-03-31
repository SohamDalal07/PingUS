# UniFind Login/Registration UI - Quick Reference

## Color Palette
| Color | Hex Code | Usage |
|-------|----------|-------|
| Navy Blue | #1B4D8E | Primary buttons, active tabs, app header |
| Dark Navy | #0F2B4D | Darker shade for depth |
| Light Gray | #F5F5F5 | Tab background, field backgrounds |
| Text Gray | #666666 | Secondary text, inactive tabs |
| Error Red | #D32F2F | Error messages, weak password |
| Success Green | #4CAF50 | Success states, strong password |

## Button Styles

### Primary Action Buttons
- **Sign In / Create Account Buttons**
  - Background: Navy Blue (#1B4D8E)
  - Text Color: White
  - Elevation: 4dp
  - Corner Radius: 12dp
  - Height: 48dp

### Secondary Action Buttons
- **SSO Button**
  - Background: White
  - Text Color: Navy Blue
  - Border: 1dp Navy Blue
  - Elevation: 2dp
  - Corner Radius: 12dp
  - Height: 48dp

## Text Field Components
- **All Input Fields**: TextInputLayout with Material Design 3
  - Corner Radius: 12dp
  - Helper Text: For additional info
  - Error Messages: Dynamic error display
  - Password Toggle: Integrated eye icon

## Form Fields Overview

### Sign In Form
```
┌─ Header ───────────────────┐
│  🔍 UniFind                │
│  University Lost & Found   │
└────────────────────────────┘
┌─ Tabs ─────────────────────┐
│ [Sign In] [Create Account] │
└────────────────────────────┘
┌─ Form ─────────────────────┐
│ Email: [______________]    │
│ Password: [__________] 👁  │
│ ☑ Keep me signed in        │
│              [Forgot pwd?] │
│ [    SIGN IN    ]          │
│ ────── or continue with ── │
│ [ Continue with SSO ]      │
│ New here? Create account   │
└────────────────────────────┘
```

### Create Account Form
```
┌─ Personal Information ─────┐
│ Full Name: [___________]   │
│ University Email: [____]   │
│ (Must be official email)   │
│ ID: [____] Role: [___]     │
│ Department: [__________]   │
│ Phone: [___] (optional)    │
└────────────────────────────┘
┌─ Set Password ─────────────┐
│ Password: [__________] 👁   │
│ ▮▮▮ ▮▮▮ ▮▮▮  (strength)    │
│ Confirm: [__________] 👁    │
│ ☑ I agree to terms         │
│ [ CREATE ACCOUNT ]         │
│ Already have account?      │
└────────────────────────────┘
```

## Interactive Elements

### Tab Switching
- Click "Sign in" → Switch to Sign In form
- Click "Create account" → Switch to Create Account form
- Active tab shows navy blue background + white text
- Inactive tab shows transparent background + gray text

### Password Strength Indicator
```
Weak:    ▮ (Red) ▪ ▪
Medium:  ▮ ▮ (Gray) ▪
Strong:  ▮ ▮ ▮ (Green)
```
- Evaluates: Length (8+), Uppercase, Numbers
- Updates in real-time

### Validation Feedback
- ✓ Red error text below field on validation failure
- ✓ Error clears when field is corrected
- ✓ Toast notifications for form-level errors

## Spacing Guidelines

| Element | Value |
|---------|-------|
| Card Margin (H) | 16dp |
| Card Padding | 24dp |
| Field Margin (bottom) | 12-16dp |
| Section Divider | 20dp |
| Button Height | 48dp |
| Icon Size | 60dp |
| Corner Radius (Cards) | 16dp |
| Corner Radius (Fields) | 12dp |
| Elevation (Primary) | 4dp |
| Elevation (Secondary) | 2dp |

## Font Styles

| Element | Size | Style | Color |
|---------|------|-------|-------|
| App Name | 28sp | Bold | White |
| Tagline | 14sp | Normal | White |
| Section Header | 16sp | Bold | Navy |
| Field Label | 14sp | Normal | Navy |
| Button Text | 16sp | Bold | White/Navy |
| Helper Text | 12sp | Normal | Gray |
| Error Text | 12sp | Normal | Red |
| Link Text | 14sp | Bold | Navy |

## Validation Messages

| Scenario | Message |
|----------|---------|
| Empty Email | "Email is required" |
| Invalid Email | "Must be a valid university email" |
| Empty Password | "Password is required" |
| Short Password | "Password must be at least 8 characters" |
| Empty Name | "Full name is required" |
| Invalid ID | "Student/Staff ID is required" |
| No Department | "Department is required" |
| Weak Password | "Password must be 8+ chars with uppercase and number" |
| Password Mismatch | "Passwords do not match" |
| Terms Not Accepted | "Please agree to terms and conditions" |

## Dynamic Behaviors

### Form Submission
1. User clicks button
2. All fields validated
3. If valid → Show success toast
4. If invalid → Show field errors + button disables briefly
5. On success → Clear form and switch tabs (for registration)

### Email Verification
- After successful registration
- Toast: "Account created! Please verify your email."
- Auto-switch to Sign In form after confirmation
- User should check university inbox

## Accessibility Features
- ✓ Content descriptions on images
- ✓ Proper label associations
- ✓ Touch target minimum 48dp
- ✓ Clear error messaging
- ✓ High contrast ratios

## Notes for Developers

### Key Methods in Login_page.java:
- `switchToSignIn()` - Switch to sign in form
- `switchToCreateAccount()` - Switch to register form
- `handleSignIn()` - Validate and process sign in
- `handleCreateAccount()` - Validate and process registration
- `isPasswordStrong()` - Check password strength
- `updatePasswordStrength()` - Update strength indicator
- `showEmailVerificationDialog()` - Show verification prompt

### Resource Files:
- `activity_login_page.xml` - Main layout
- `colors.xml` - Color definitions
- `strings.xml` - All text content
- `drawable/*` - Custom shapes and icons

---

**Last Updated**: March 2026
**Status**: ✅ Production Ready

