# FLUX BANK - ANDROID NATIVE PROJECT
- Package: com.flux.bank
- Language: Kotlin
- Architecture: MVVM (ViewBinding, ViewModel, Repository, Coroutines)
- Backend: Firebase Authentication + Cloud Firestore

## UI / UX Design System (Option 2: Apple Bento Grid & Ultra-Minimalism)
- Dominant Canvas: Pure White (`#FFFFFF`) & Apple Signature Canvas (`#F5F5F7`). Strictly NO generic top-heavy dark banners.
- Layout Architecture: 100% `ConstraintLayout` for all screens, modular Bento cards, and component containers.
- Bento Card Principles:
  - Surface: Pure White (`#FFFFFF`) with 1dp hairline border (`#E5E7EB`).
  - Corner Radius: 20dp to 24dp for major Bento tiles, 12dp to 16dp for inner chips and action buttons.
  - Elevation: Soft, subtle Apple-style shadow (`elevation="2dp"` to `"3dp"`).
  - Primary Action / Text: Obsidian Black (`#0A0D12` / `#111827`) for high-contrast, modern authority.
  - Interactive Accents: Apple Blue (`#0071E3`), Emerald Green (`#10B981`) for monetary gains & security, Refined Gold (`#D97706`) for card chips.
  - Debit Card Aesthetic: Apple Titanium White Card with frosted metallic sheen.
- Touch Targets: Minimum 48dp x 48dp on all interactive elements with appropriate IME keyboard actions (`actionNext`, `actionDone`).
- Image Handling: Compress Bitmap to Base64 String to store directly in Firestore (NO Firebase Storage).

## User Roles:
1. CUSTOMER: Cá nhân mở tài khoản, chuyển tiền, xem số dư, nộp hồ sơ eKYC, xác thực sinh trắc học.
2. BANK_OFFICER: Nhân viên ngân hàng tạo/sửa thông tin khách hàng, chỉnh lãi suất, kiểm duyệt hàng đợi eKYC.

## Implemented Milestones
### Feature 1: User Profiles & Role-Based Authentication (Completed & Tested)
- Models: `User.kt`, `UserRole.kt`
- Repositories & State: `AuthRepository.kt`, `AuthViewModel.kt`, `AuthUiState`
- UI: `LoginActivity`, `CustomerMainActivity`, `OfficerMainActivity` with 100% `ConstraintLayout` and Apple Bento styling.

### Feature 2: Security Requirements - eKYC, Biometrics & 2FA (Completed & Tested)
- eKYC Capture & Review:
  - `EkycActivity`: Camera photo capture for National ID and Face Selfie, client-side downsampling $\le 480$px, Base64 compression, Firestore submission.
  - `OfficerEkycReviewActivity`: Review queue viewer, Base64 bitmap rendering, one-touch Approval/Rejection status transition.
  - Live status indicator capsule & banner in `CustomerMainActivity`.
- Biometrics & Session Security:
  - `BiometricHelper`: AndroidX BiometricPrompt wrapper for Touch ID / Face ID hardware validation.
  - `SessionManager`: Local encrypted/safe session caching for seamless biometric login in `LoginActivity`.
  - Biometric quick sign-in button in `LoginActivity`.
- Two-Factor Authentication (2FA):
  - `OtpManager`: Cryptographic 6-digit OTP generation, expiration timer, single-use validation.
- Unit Testing:
  - `UserUnitTest.kt`: 100% pass (User model, default values, roles, UI states).
  - `SecurityUnitTest.kt`: 100% pass (OTP lifecycle, single-use security, KYC constants, User security fields).