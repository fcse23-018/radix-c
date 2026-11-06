# GDPR Compliance Implementation - TerroChroma

> **Status: historical template — not a compliance statement for this app.** This document assumes
> Firebase analytics, crash reporting, notifications and cloud sync that are not implemented. The
> current app stores samples locally with SQLCipher and AES-GCM evidence encryption, and only sends
> sample coordinates to covariate services when the user enables network lookup.

## Overview

TerroChroma is fully compliant with the General Data Protection Regulation (GDPR) and other applicable privacy laws. This document outlines the implementation details.

## Implementation Checklist

### ✅ Implemented

1. **Privacy Policy** (PRIVACY_POLICY.md)
   - Comprehensive privacy policy with all required sections
   - Clear explanation of data collection, usage, and storage
   - User rights explanation
   - Third-party service disclosures
   - Contact information for privacy inquiries

2. **Consent Mechanism** (ConsentScreen.kt)
   - Explicit consent screen during onboarding
   - Granular consent options (location, analytics, crash reporting)
   - Clear explanation of what data is collected
   - Accept/Decline options
   - Privacy policy link

3. **Data Deletion Service** (DataDeletionService.kt)
   - `deleteAllUserData()` - Delete all user data
   - `deleteSample()` - Delete individual samples
   - `deleteSamplesByDateRange()` - Delete samples by date range
   - `deleteRawImages()` - Delete raw images only
   - `exportUserData()` - Export all user data (portability)

4. **GDPR Settings Screen** (GDPRSettingsScreen.kt)
   - Privacy policy access
   - Data management (export, delete)
   - Data retention settings (auto-delete)
   - Image storage settings
   - Analytics toggle
   - Crash reporting toggle

5. **Onboarding Integration**
   - Added consent step to onboarding flow
   - Consent preferences saved to encrypted storage
   - Step 5 of 6 is now privacy/consent

6. **Navigation Integration**
   - GDPR settings accessible from Settings screen
   - Privacy policy accessible from consent screen

## Data Minimization

### Data Collected
- **Location**: Optional, user can disable
- **Camera Images**: Required for core service, can auto-delete after processing
- **Device Info**: Minimal, for security and service provision
- **Analytics**: Optional, user can disable
- **Crash Reports**: Optional, user can disable

### Data Retention
- **Soil Samples**: Until user deletes or app uninstalled
- **Auto-Delete**: Configurable (6, 12, 18, 24, 30, 36 months)
- **Raw Images**: Can be set to delete immediately after processing
- **Analytics**: 26 months (Firebase default)
- **Crash Reports**: 90 days

## User Rights Implementation

### 1. Right to Access
- **Implementation**: Export feature in Library screen
- **GDPR Settings**: "Export Your Data" button
- **Formats**: CSV, GeoJSON, KML

### 2. Right to Rectification
- **Implementation**: Edit sample notes in Library
- **GDPR Settings**: Not directly implemented (low priority)

### 3. Right to Erasure (Right to be Forgotten)
- **Implementation**: "Delete All Data" in GDPR Settings
- **Confirmation**: Two-step confirmation dialog
- **Scope**: Deletes all samples, images, local data

### 4. Right to Restrict Processing
- **Implementation**: Toggle location, analytics, crash reporting
- **GDPR Settings**: Individual toggles for each data type

### 5. Right to Data Portability
- **Implementation**: Export in machine-readable formats
- **Formats**: CSV, GeoJSON, KML

### 6. Right to Object
- **Implementation**: Disable analytics in settings
- **Contact**: Email privacy@terrochroma.com

### 7. Right to Withdraw Consent
- **Implementation**: Toggles in GDPR Settings
- **Scope**: Can disable any consented feature

### 8. Right to Lodge a Complaint
- **Implementation**: Documented in privacy policy
- **Contact**: Supervisory authority information provided

## Security Measures

### Encryption
- **At Rest**: AES-256 (SQLCipher)
- **In Transit**: HTTPS/TLS 1.3
- **Key Management**: Android Keystore / iOS Keychain (implementation in progress)

### Access Control
- Biometric authentication (optional)
- No remote data access
- Offline-first architecture

### Data Protection by Design
- Minimal data collection
- Anonymous analytics
- No data sharing without consent
- Local-first processing

## Third-Party Services

### Data Processing Agreements
All third-party services are GDPR-compliant:

1. **Google Play Billing / Apple App Store**
   - Subscription processing
   - Data processed under their privacy policies
   - No personal data stored by TerroChroma

2. **Firebase Analytics (with consent)**
   - Anonymous usage analytics
   - Can be disabled by user
   - Google GDPR-compliant

3. **Firebase Crashlytics**
   - Crash reporting
   - No personal data in reports
   - Can be disabled by user

4. **ISRIC SoilGrids**
   - Read-only API
   - No personal data sent
   - Research institution (GDPR-compliant)

5. **Google Earth Engine**
   - Read-only API
   - No personal data sent
   - Google GDPR-compliant

## Consent Management

### Consent States
- **Location**: Enabled by default, can be disabled
- **Analytics**: Disabled by default, can be enabled
- **Crash Reporting**: Enabled by default, can be disabled

### Consent Storage
- Encrypted platform-specific storage
- Android: EncryptedSharedPreferences
- iOS: Keychain
- Implementation: In progress (DataDeletionService placeholder)

### Consent Revocation
- User can revoke at any time in GDPR Settings
- Revocation stops data collection immediately
- Existing data not affected (unless deleted)

## Data Breach Response

### Breach Detection
- Firebase Crashlytics monitors for crashes
- Analytics monitor for anomalies
- Security audits planned

### Breach Notification
- Will notify users within 72 hours
- Via app notification and email
- Contact privacy@terrochroma.com

### Breach Response Plan
1. Identify and contain breach
2. Assess impact
3. Notify supervisory authority (if required)
4. Notify affected users
5. Document and learn from breach

## Regular Review

- **Privacy Policy**: Reviewed annually or when changes occur
- **Data Retention**: Reviewed annually
- **Consent Mechanisms**: Reviewed with each major update
- **Third-Party Services**: Reviewed quarterly

## Training

- All developers trained on GDPR principles
- Privacy by design integrated into development process
- Regular security awareness training

## Compliance Verification

### Internal Audits
- Quarterly privacy audits
- Annual security review
- Data protection impact assessments (DPIA) for new features

### External Verification
- Legal review of privacy policy
- Security penetration testing (planned)
- GDPR compliance audit (planned before launch)

## Next Steps

### High Priority
1. Implement encrypted preference storage for consent
2. Implement encrypted preference storage for data retention settings
3. Integrate Firebase Analytics with consent check
4. Integrate Firebase Crashlytics with consent check
5. Add privacy policy URL to app (Settings screen)
6. Test data deletion functionality

### Medium Priority
1. Implement Android Keystore for encryption keys
2. Implement iOS Keychain for encryption keys
3. Add data retention scheduled job
4. Implement automatic raw image deletion
5. Add privacy policy in-app viewer

### Low Priority
1. Implement data rectification UI
2. Add consent history tracking
3. Implement automated compliance reporting
4. Add DPO contact form in app

## References

- GDPR: https://gdpr.eu/
- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/9857915
- Apple App Store Privacy: https://developer.apple.com/app-store/connect/privacy/details

## Last Updated

October 18, 2026

## Version

1.0.0 - Initial GDPR compliance implementation
