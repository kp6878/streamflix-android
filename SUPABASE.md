# Supabase in StreamFlix

## 1. Authentication (`/auth/v1/`)

- **Sign Up** — Creates a new account with email + password. If email confirmation is required, notifies the user to check their inbox before signing in.
- **Sign In** — Authenticates with email + password, receives a JWT access token + refresh token.
- **Sign Out** — Calls the Supabase logout endpoint to invalidate the server-side session.
- **Token Refresh** — Automatically refreshes the JWT before it expires (within 60 seconds of expiry). Called both at app launch and lazily before any API call.
- **Session Persistence** — The access token, refresh token, expiry date, user ID, and email are all stored in `UserDefaults` so the user stays logged in across launches.
- **Bootstrap** — On app launch, restores the persisted session and refreshes the token if needed. Controls whether the login screen or main app is shown (`isSignedIn`, `isBootstrapping` published state).

---

## 2. Watch History (`/rest/v1/watch_history`)

This is the most data-rich table. Each row is scoped to both a `user_id` (account) and a `profile_id` (sub-profile).

**Stored fields per entry:**
- `unique_id` — `"tt1234567"` for movies, `"tt1234567:S01E05"` for episodes
- `content_id`, `content_type`, `content_name`
- `last_position` (seconds), `duration` (seconds), `is_completed`
- `last_watched_at` (ISO8601 timestamp)
- `season`, `episode`, `episode_id`, `episode_title`
- `poster`, `background`, `thumbnail` (image URLs)
- `last_stream_url` — the direct URL that was played
- `last_stream_info_hash` — the torrent hash used (if any)
- `last_stream_file_idx` — the file index within the torrent

**Operations:**
- **Upsert** — Fire-and-forget write on every progress update (conflict resolution on `profile_id + unique_id`)
- **Fetch** — Loads up to 200 items ordered by most recently watched, used when switching profiles
- **Delete single item** — When user removes one item from history
- **Delete all for a profile** — When user clears entire watch history

---

## 3. User Preferences (`/rest/v1/user_preferences`)

Synced per-account (not per-profile). Stores:
- `real_debrid_api_key`
- `preferred_audio_language`, `secondary_audio_language`
- `subtitles_enabled`, `preferred_subtitle_language`
- `opensubtitles_api_key`, `opensubtitles_username`, `opensubtitles_password`
- `max_cache_size_gb`
- `updated_at`

**Operations:**
- **Upsert** — Synced whenever any setting changes (conflict on `user_id`)
- **Fetch** — Pulled on sign-in and applied to local `AppState`, so settings roam across devices

---

## 4. Watchlist / Favorites (`/rest/v1/watchlist`)

Scoped to both `user_id` and `profile_id`. Stores:
- `content_id`, `content_type`, `content_name`
- `poster`, `background`, `year`, `imdb_rating`
- `genres` (array)
- `added_at`

**Operations:**
- **Upsert** — Written immediately when user adds a favorite (conflict on `user_id + profile_id + content_id`)
- **Delete** — Written immediately when user removes a favorite
- **Fetch** — Loads up to 500 items ordered by most recently added, used when switching profiles

---

## 5. Profiles (`/rest/v1/profiles`)

Each user can have up to 5 profiles, all synced to Supabase. Stores:
- `profile_id` (UUID), `user_id`
- `name`, `avatar_name` (SF Symbol)
- `is_kids_profile`
- `preferred_audio_language`, `preferred_subtitle_language`
- `auto_play_next_episode`
- `created_at`, `last_used_at`

**Operations:**
- **Upsert** — Synced whenever a profile is created or updated (conflict on `user_id + profile_id`)
- **Fetch** — Pulled on sign-in, ordered by most recently used, merges remote profiles into local state
- **Delete** — Removes a profile and all its associated data when the user deletes it

---

## 6. Row-Level Security (RLS)

`auth.uid() = user_id` RLS is enforced on every table. Users can only read/write their own data. The JWT from authentication is passed as a `Bearer` token on every REST call to satisfy this policy.

---

## Summary Table

| Feature | Table | Scoped To |
|---|---|---|
| Auth / Session | `auth.users` (built-in) | Account |
| Watch Progress | `watch_history` | Account + Profile |
| Favorites | `watchlist` | Account + Profile |
| App Settings | `user_preferences` | Account only |
| User Profiles | `profiles` | Account |

The overall design is **local-first**: all data lives on device and Supabase writes are fire-and-forget. Supabase is used for cross-device sync and persistence, not as the primary data source during normal use.
