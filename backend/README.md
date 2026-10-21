# Backend

This folder is a boundary for a future Sahakar Bharati server. Nothing here is implemented yet.

Backend framework, database, authentication, APIs, deployment, and migrations will be decided in a separate architecture phase.

The Android app in `../frontend/app` is the current product. Registration, sign-in, session, tracking, and QR attendance are local/demo behaviour on the device. The Android frontend must never connect directly to the production database. When a backend is chosen, the server will own database access and the shared responsibilities below.

## Intended responsibilities

- Citizen registration and authentication
- Profile and session data that must survive across devices
- Chariot tracking and attendance records
- Notifications and benefits data
- Admin or operations APIs, if required later

Do not add placeholder APIs, fake databases, unused dependency manifests, or empty implementation classes until the stack is selected.

## Proposed future layout

```
backend/
  README.md                 # this file
  src/                      # server source, once a language/framework is chosen
  contracts/                # API contracts shared with the Android client
  migrations/               # database schema migrations
  config/                   # environment and deployment configuration
  tests/                    # backend unit and integration tests
```

Technology, database, authentication, and hosting will be chosen in a later task.
