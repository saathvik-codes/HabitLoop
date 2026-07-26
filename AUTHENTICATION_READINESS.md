# HabitLoop Authentication Readiness

## Implemented Android flows

- Anonymous Firebase session on first launch.
- Anonymous-to-permanent credential linking to preserve the Firebase UID when possible.
- Email/password account creation and sign-in.
- Password-reset email.
- Google ID-token sign-in using `default_web_client_id` from `google-services.json`.
- Phone/SMS verification, including automatic verification where supported.
- Apple, GitHub and Microsoft Firebase OAuth provider entry points.
- Collision fallback for credentials that already belong to an existing account.
- Permanent-account logout while preserving the local Room database.
- Profile display name and local avatar style/color customization.

## Firebase Console requirements

Each provider button requires the matching provider to be enabled and configured in Firebase Authentication:

- Email/Password
- Phone
- Google
- Apple
- GitHub
- Microsoft

Apple, GitHub and Microsoft also require their provider-specific client IDs, secrets, redirect URIs and platform configuration. Phone authentication requires valid SHA fingerprints, SMS configuration and device/app verification. Google requires the Android SHA-1/SHA-256 fingerprints to be registered and an updated `google-services.json` after configuration changes.

## Release validation

Test every provider on a physical release-signed Android device. Debug-build success confirms code integration but cannot prove that provider-console credentials, redirect URIs, SMS quotas or release certificate fingerprints are correct.

## Account/data rules

- Local Room data is not deleted by logout.
- Linking an anonymous session preserves its Firebase UID when the credential is new.
- Signing into an existing credential switches to that account; local data remains on the device.
- Cross-account merge conflict resolution is not automatic. Before public release, add an explicit merge/replace choice if the signed-in cloud account and local device both contain habits.
