# HabitLoop Monetization Readiness

## Implemented

- Rewarded ad preload with visible loading, ready, playing, unavailable and retry states.
- One completed rewarded ad grants exactly one freeze to the selected habit.
- Reward persistence uses Room first and mirrors to Firebase when connected.
- Visible success confirmation names the habit that received the freeze.
- Display failure surfaces a message and triggers the next preload.
- One labeled banner placement exists after Challenge discovery content.
- No banner appears on Today, onboarding, habit creation, completion controls or account/security screens.

## Required before production release

1. Replace Google’s test AdMob application ID in `AndroidManifest.xml`.
2. Replace the rewarded and banner test-unit IDs.
3. Add Google’s User Messaging Platform consent flow for applicable regions before requesting ads.
4. Configure child-directed-treatment and age controls according to the intended audience.
5. Test reward callbacks, dismissal, offline behavior and rotation on a physical Google Play-enabled device.
6. Add server-side verification if freeze tokens later gain monetary or transferable value.
7. Add ad-frequency analytics without collecting habit names or sensitive routine content.

## Product rules

- Never gate basic tracking, editing, history or reminders behind an ad.
- Never display a banner beside a health, sobriety or prayer check-in.
- Rewarded ads must always state the reward before playback.
- A failed or skipped reward must never silently grant or remove a token.
- Paid ad removal can be added later, but should not reduce tracking functionality.
