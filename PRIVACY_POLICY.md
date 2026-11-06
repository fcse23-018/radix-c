# TerroChroma Privacy Policy

> **Status: historical template — do not publish as-is.** This policy describes an older
> TerroChroma design with Firebase analytics, billing, iOS and cloud sync, none of which exist in
> SoilCarbon Engine. The implemented network behaviour is limited to optional point covariate
> lookups: with network lookup enabled, sample coordinates are sent to SoilGrids, Open-Meteo and
> Sentinel. No image or sample record is uploaded. See `README.md` for current data handling.

**Last Updated**: October 18, 2026  
**Effective Date**: October 18, 2026

## Table of Contents

1. [Introduction](#introduction)
2. [Data We Collect](#data-we-collect)
3. [How We Use Your Data](#how-we-use-your-data)
4. [Data Security](#data-security)
5. [Data Retention](#data-retention)
6. [Your Rights](#your-rights)
7. [Third-Party Services](#third-party-services)
8. [Children's Privacy](#childrens-privacy)
9. [International Data Transfers](#international-data-transfers)
10. [Changes to This Policy](#changes-to-this-policy)
11. [Contact Us](#contact-us)

---

## Introduction

TerroChroma ("we," "our," or "us") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, store, and protect your personal information when you use our mobile application ("the App").

This policy applies to all users of the App and is compliant with the General Data Protection Regulation (GDPR) and other applicable privacy laws.

---

## Data We Collect

### 1. Location Data

**What we collect:**
- GPS coordinates (latitude, longitude, altitude) when you capture soil samples
- Optional: Location data from device GPS when enabled

**Purpose:**
- To tag soil samples with geographic location for analysis
- To enable map visualization of your samples
- To provide location-based soil reference data (SoilGrids)

**Legal Basis:**
- Explicit consent (you can choose to capture without location)
- Legitimate interest for soil analysis functionality

**Storage:**
- Encrypted in local database (AES-256)
- Stored only on your device unless you explicitly export

### 2. Camera Images

**What we collect:**
- RAW camera images (DNG on Android, Bayer on iOS)
- Processed image data for color and texture analysis
- Image hash for data integrity

**Purpose:**
- To analyze soil color and texture characteristics
- To estimate Soil Organic Carbon (SOC) and Soil Organic Matter (SOM)
- To generate scientific-grade soil measurements

**Legal Basis:**
- Explicit consent (you actively capture images)
- Contract necessity (to provide the core service)

**Storage:**
- Encrypted in local database (AES-256)
- Processed data (features, not raw images) stored for analysis
- Raw images may be deleted after processing (configurable in settings)

### 3. Device Information

**What we collect:**
- Device ID (for encryption key generation)
- Device model and OS version
- Camera parameters (ISO, exposure time, white balance)
- App version and build number

**Purpose:**
- To generate secure encryption keys
- To optimize camera settings for your device
- To troubleshoot and improve the app
- To track provenance of soil samples

**Legal Basis:**
- Legitimate interest (security and service improvement)
- Contract necessity (to provide the service)

**Storage:**
- Encrypted in local database
- Not shared with third parties

### 4. Usage Analytics

**What we collect:**
- Anonymous app usage statistics
- Feature usage frequency
- Crash reports and error logs
- Performance metrics

**Purpose:**
- To understand how the app is used
- To identify and fix bugs
- To improve app performance
- To guide feature development

**Legal Basis:**
- Legitimate interest (service improvement)
- Explicit consent (you can opt out in settings)

**Storage:**
- Anonymous, aggregated data
- No personally identifiable information
- Processed by Firebase Analytics (with consent)

### 5. Subscription and Billing Data

**What we collect:**
- Subscription status and tier
- Purchase history
- Billing information (processed by Google Play/App Store)

**Purpose:**
- To manage your subscription
- To provide premium features
- To process payments

**Legal Basis:**
- Contract necessity (subscription service)
- Legal obligation (payment processing)

**Storage:**
- Encrypted in local database
- Billing data processed by Google Play/App Store (not stored by us)

---

## How We Use Your Data

We use your personal data for the following purposes:

1. **Core Service Delivery**
   - Analyze soil samples to estimate SOC and SOM
   - Provide location-based soil reference data
   - Generate sample reports and visualizations

2. **Service Improvement**
   - Optimize camera settings for your device
   - Improve ML model accuracy
   - Fix bugs and crashes
   - Enhance user experience

3. **Account Management**
   - Manage your subscription
   - Restore purchases
   - Sync data across devices (optional)

4. **Compliance and Security**
   - Generate cryptographic hashes for data integrity
   - Encrypt and protect your data
   - Comply with legal obligations

**We will never:**
- Sell your personal data to third parties
- Use your data for advertising
- Share your data without your explicit consent
- Process your data beyond the purposes stated above

---

## Data Security

We implement appropriate technical and organizational measures to protect your personal data:

### Encryption
- **At Rest**: All data encrypted with AES-256 (SQLCipher)
- **In Transit**: HTTPS/TLS 1.3 for all network communications
- **Key Management**: Encryption keys stored in hardware-backed secure storage (Android Keystore / iOS Keychain)

### Access Control
- Biometric authentication (optional) for app access
- No remote access to your local data
- Data processed only on your device (offline-first)

### Secure Development
- Regular security audits
- Vulnerability scanning
- Dependency updates
- Secure coding practices

### Data Minimization
- We collect only the data necessary for the service
- Raw images can be automatically deleted after processing
- Location data is optional
- Analytics are anonymous and aggregated

---

## Data Retention

We retain your personal data only as long as necessary for the purposes outlined in this policy:

### Soil Sample Data
- **Retention Period**: Until you delete the data or uninstall the app
- **Automatic Deletion**: You can configure automatic deletion in settings (e.g., after 1 year)
- **Raw Images**: Can be configured to delete immediately after processing

### Analytics Data
- **Retention Period**: 26 months (Firebase Analytics default)
- **Anonymous Data**: Aggregated and anonymized after 6 months

### Crash Reports
- **Retention Period**: 90 days
- **Purpose**: Debugging and improvement

### Subscription Data
- **Retention Period**: As long as your subscription is active + 2 years (for legal compliance)
- **Deletion**: Upon account deletion or subscription cancellation

### Device Information
- **Retention Period**: Until you uninstall the app
- **Purpose**: Service provision and security

**Your Right to Deletion**: You can delete all your data at any time from the Settings screen. This will permanently delete all soil samples, images, and local data.

---

## Your Rights

Under GDPR and applicable privacy laws, you have the following rights:

### 1. Right to Access
You can request a copy of all personal data we hold about you. Use the "Export Data" feature in Settings to download your data in CSV, GeoJSON, or KML format.

### 2. Right to Rectification
You can correct inaccurate or incomplete data by editing sample notes or using the edit feature in the Library screen.

### 3. Right to Erasure (Right to be Forgotten)
You can request deletion of all your personal data by:
- Using the "Delete All Data" button in Settings
- Contacting us at privacy@terrochroma.com

This will permanently delete:
- All soil samples
- All images
- All analytics data linked to your device
- All local app data

### 4. Right to Restrict Processing
You can limit data collection by:
- Disabling location permissions
- Disabling analytics in Settings
- Configuring automatic image deletion

### 5. Right to Data Portability
You can export your data in machine-readable formats (CSV, GeoJSON, KML) using the Export feature in the Library screen.

### 6. Right to Object
You can object to data processing by:
- Disabling analytics in Settings
- Contacting us at privacy@terrochroma.com

### 7. Right to Withdraw Consent
You can withdraw consent at any time by:
- Revoking permissions in device settings
- Disabling features in app settings
- Deleting your data

### 8. Right to Lodge a Complaint
You have the right to lodge a complaint with a supervisory authority (data protection authority) in your jurisdiction if you believe our processing of your personal data infringes GDPR.

---

## Third-Party Services

We use the following third-party services to provide the TerroChroma app:

### Google Play Billing (Android)
- **Purpose**: Process subscription payments
- **Data Collected**: Subscription status, purchase history
- **Data Processing**: Processed by Google under their privacy policy
- **Your Rights**: Managed through Google Play account

### Apple App Store (iOS)
- **Purpose**: Process subscription payments
- **Data Collected**: Subscription status, purchase history
- **Data Processing**: Processed by Apple under their privacy policy
- **Your Rights**: Managed through Apple ID account

### Firebase Analytics (with consent)
- **Purpose**: Anonymous usage analytics
- **Data Collected**: Anonymous app usage statistics
- **Data Processing**: Processed by Google under their privacy policy
- **Your Rights**: Can be disabled in Settings

### Firebase Crashlytics
- **Purpose**: Crash reporting and debugging
- **Data Collected**: Crash logs, error reports
- **Data Processing**: Processed by Google under their privacy policy
- **Your Rights**: No personal data in crash reports

### ISRIC SoilGrids (optional)
- **Purpose**: Provide global soil reference data
- **Data Collected**: None (read-only API)
- **Data Processing**: No personal data sent

### Google Earth Engine (optional)
- **Purpose**: Provide environmental covariates
- **Data Collected**: None (read-only API)
- **Data Processing**: No personal data sent

**We do not sell your data to any third party.**

---

## Children's Privacy

TerroChroma is not intended for children under the age of 16. We do not knowingly collect personal data from children under 16. If we discover that we have collected personal data from a child under 16 without parental consent, we will take steps to delete that data immediately.

If you are a parent or guardian and believe your child has provided us with personal data, please contact us at privacy@terrochroma.com.

---

## International Data Transfers

TerroChroma is designed to work offline-first. Most of your data is stored locally on your device and is not transferred outside your jurisdiction.

When you enable optional features (analytics, cloud sync), data may be transferred to:

- **Google Firebase** (United States) - For analytics and crash reporting
- **ISRIC SoilGrids** (Netherlands) - For soil reference data
- **Google Earth Engine** (United States) - For environmental covariates

All transfers are protected by:
- HTTPS/TLS encryption
- Standard contractual clauses (where applicable)
- GDPR-compliant data processing agreements

You can disable these features in Settings to prevent international data transfers.

---

## Changes to This Policy

We may update this Privacy Policy from time to time. We will notify you of any changes by:

- Posting the new policy in the app
- Sending a notification when you open the app
- Updating the "Last Updated" date

Your continued use of the app after changes constitutes acceptance of the updated policy.

---

## Contact Us

If you have any questions about this Privacy Policy or our data practices, please contact us:

**Email**: privacy@terrochroma.com  
**Address**: [Your Company Address]  
**Website**: https://terrochroma.com

**Data Protection Officer**:  
If you have specific GDPR-related inquiries, you can contact our Data Protection Officer at dpo@terrochroma.com

---

## Cookie Policy

TerroChroma does not use cookies or similar tracking technologies.

---

## License and Third-Party Licenses

This app uses open-source libraries. See the About section in Settings for a complete list of licenses.

---

**GDPR Compliance Statement**

TerroChroma is committed to GDPR compliance. We have implemented:
- Data protection by design and by default
- Clear consent mechanisms
- Data subject rights implementation
- Data breach notification procedures
- Data protection impact assessments (where required)

Last reviewed: October 18, 2026
