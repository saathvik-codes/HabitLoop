# HabitLoop community setup

The Android client contains no fallback challenge catalog. Every circle shown in
Discover comes from the Firestore `circles` collection.

## Official starter challenges

Create a few starter documents in Firebase Console using auto-generated IDs.
Set `featured` to `true` only from the trusted console/admin environment. Suggested
launch set:

- Gentle Morning — habit: Complete a 10-minute morning reset
- Focus Together — habit: Complete one distraction-free focus block
- Move Your Way — habit: Move intentionally for 20 minutes

Required document fields:

| Field | Type | Example |
|---|---|---|
| `id` | string | Same as the document ID |
| `title` | string | Gentle Morning |
| `description` | string | Build a realistic morning rhythm with a supportive group. |
| `habitName` | string | Complete a 10-minute morning reset |
| `category` | string | Wellbeing |
| `emoji` | string | 🌤️ |
| `cadence` | string | Daily |
| `durationDays` | number | 21 |
| `leaderName` | string | HabitLoop |
| `ownerId` | string | Trusted admin Firebase UID |
| `memberCount` | number | 0 |
| `checkInCount` | number | 0 |
| `featured` | boolean | true |
| `createdAt` | timestamp | Current server time |

Deploy `firestore.rules` before enabling the module. Normal clients can create
community circles only with `featured = false`; they cannot impersonate an
official challenge.
