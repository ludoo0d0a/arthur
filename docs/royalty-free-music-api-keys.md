# Royalty-free music providers — API key registration

Guide to obtain the credentials used by Arthur’s **Recherche musiques libres de droits** screen
(`RoyaltyFreeMusicScreen` / `MusicProviderFactory`).

Wire secrets in **`local.properties`** (gitignored) or CI secrets. They are injected into
`BuildConfig` by `androidApp/build.gradle.kts`.

```properties
# local.properties (project root)
JAMENDO_CLIENT_ID=...
FREESOUND_API_KEY=...
PIXABAY_API_KEY=...
```

| BuildConfig field | Provider | Required? |
|-------------------|----------|-----------|
| `JAMENDO_CLIENT_ID` | Jamendo | Yes for Jamendo |
| `FREESOUND_API_KEY` | Freesound | Yes for Freesound |
| `PIXABAY_API_KEY` | Pixabay Music | Yes for Pixabay (same key as photos/videos) |
| — | Archive.org | **No key** |
| — | Incompetech | **No key** |

**Temporarily removed:** Free Music Archive (legacy API returns HTTP 404).

After changing `local.properties`, sync / rebuild the app so `BuildConfig` updates.
Missing keys are shown as an error banner on the royalty-free music screen.

---

## 1. Jamendo — `JAMENDO_CLIENT_ID`

Full tracks (ambient, chill, meditation, zen).

| | Link |
|---|------|
| **Sign up (developer portal)** | https://devportal.jamendo.com/signup |
| **Login / apps** | https://devportal.jamendo.com/ |
| **API docs (tracks)** | https://developer.jamendo.com/v3.0/tracks |
| **Auth docs** | https://developer.jamendo.com/v3.0/authentication |

**Steps**

1. Create an account on the [Jamendo developer portal](https://devportal.jamendo.com/signup).
2. Confirm the email if asked.
3. Create an **application** (any project name / website is fine for local dev).
4. Open the app → copy the **Client ID** (not the Client Secret).
5. Set `JAMENDO_CLIENT_ID=<client_id>` in `local.properties`.

Read-only plan is enough for search + stream URLs.

---

## 2. Freesound — `FREESOUND_API_KEY`

Nature / ambient soundscapes (bowls, rain, drones, etc.). Prefers preview MP3 URLs.

| | Link |
|---|------|
| **Create account** | https://freesound.org/home/register/ |
| **Apply for API key** | https://freesound.org/apiv2/apply/ |
| **API docs** | https://freesound.org/docs/api/ |
| **My API credentials** (after login) | https://freesound.org/apiv2/apply/ (shows token once approved) |

**Steps**

1. [Register](https://freesound.org/home/register/) on Freesound and verify email.
2. Sign in, open [Apply for API credentials](https://freesound.org/apiv2/apply/).
3. Describe the app (e.g. “Arthur ambient Android app — search & offline cache of CC previews”).
4. Copy the **API token** when issued.
5. Set `FREESOUND_API_KEY=<token>` in `local.properties`.

---

## 3. Pixabay Music — `PIXABAY_API_KEY`

Studio royalty-free music. **Same API key** as Pixabay Photo / Video (already used elsewhere in Arthur if configured).

| | Link |
|---|------|
| **Create account** | https://pixabay.com/accounts/register/ |
| **API key (account)** | https://pixabay.com/api/docs/ |
| **Audio API section** | https://pixabay.com/api/docs/#api_audio |

**Steps**

1. [Register](https://pixabay.com/accounts/register/) / log in on Pixabay.
2. Open [API documentation](https://pixabay.com/api/docs/) while logged in — your **key** is shown on that page.
3. Set `PIXABAY_API_KEY=<key>` in `local.properties` (reuse the existing value if photos/videos already work).

Rate limit: ~100 requests / minute.

---

## 4. Internet Archive (Archive.org) — no key

| | Link |
|---|------|
| **Advanced search (JSON)** | https://archive.org/advancedsearch.php |
| **Metadata API** | https://archive.org/metadata/{identifier} |
| **Developer / usage tips** | https://archive.org/developers/ |

No signup. Arthur uses a descriptive `User-Agent` (`Arthur-Ambient/1.0`) and resolves real `.mp3` files via item metadata before download.

---

## 5. Incompetech (Kevin MacLeod) — no key

| | Link |
|---|------|
| **Music catalog** | https://incompetech.com/music/royalty-free/ |
| **License (CC BY)** | https://creativecommons.org/licenses/by/4.0/ |
| **Artist site** | https://incompetech.com/ |

No API key. Arthur ships a curated list of **verified** direct MP3 URLs (CC BY 4.0 — attribute “Kevin MacLeod (incompetech.com)” in UI when playing).

Useful for testing downloads without any secrets: filter **Incompetech** on the royalty-free music screen.

---

## CI secrets

GitHub Actions already map the same names (see `.github/workflows/android-ci.yml` / `release-play.yml`):

- `JAMENDO_CLIENT_ID`
- `FREESOUND_API_KEY`
- `PIXABAY_API_KEY`

Add them under the repo **Settings → Secrets and variables → Actions** so CI builds match local.

---

## Quick checklist

1. [ ] Jamendo Client ID → `JAMENDO_CLIENT_ID`  
2. [ ] Freesound API token → `FREESOUND_API_KEY`  
3. [ ] Pixabay key → `PIXABAY_API_KEY`  
4. [ ] Rebuild app  
5. [ ] Open Developer → Recherche musiques libres de droits — missing-key banner should clear for configured providers  
6. [ ] Use network debug bar to inspect search / download traffic  

Related: [roadmap-royalty-free-music-sources.md](./roadmap-royalty-free-music-sources.md)
