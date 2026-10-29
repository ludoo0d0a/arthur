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
| — | Archive.org | **No key** (open collections only) |
| — | Incompetech | **No key** |

**Temporarily removed:**
- Free Music Archive (legacy API returns HTTP 404).
- **Pixabay Music** — `/api/audio/` returns `[ERROR 403] Access denied.` for standard keys; Pixabay’s public API covers images/videos only. `PIXABAY_API_KEY` remains for Pixabay **Videos**.

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

## 3. Pixabay Music — paused

Pixabay’s documented public API is images + videos only. Calling `https://pixabay.com/api/audio/` with a normal key returns plain text `[ERROR 403] Access denied.` (not JSON), which previously surfaced as “unexpected JSON token”. The music provider stays in code for a possible future API but is **not** listed in the royalty-free music screen.

Keep `PIXABAY_API_KEY` for Pixabay Videos.

---

## 4. Internet Archive (Archive.org) — no key

| | Link |
|---|------|
| **Advanced search (JSON)** | https://archive.org/advancedsearch.php |
| **Metadata API** | https://archive.org/metadata/{identifier} |
| **Developer / usage tips** | https://archive.org/developers/ |

No signup. Arthur searches `collection:(opensource_audio OR netlabels)` + `format:MP3` (many other audio items 401 on CDN download), uses a descriptive `User-Agent`, and resolves real `.mp3` files (+ size) via item metadata before download.

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
- `PIXABAY_API_KEY` (videos; not required for music search)

Add them under the repo **Settings → Secrets and variables → Actions** so CI builds match local.

Live music e2e (opt-in):

```bash
./gradlew :androidApp:testFullDebugUnitTest -Pe2eMusicSources=true --tests '*MusicSourceE2eTest'
```

---

## Quick checklist

1. [ ] Jamendo Client ID → `JAMENDO_CLIENT_ID`  
2. [ ] Freesound API token → `FREESOUND_API_KEY`  
3. [ ] Rebuild app  
4. [ ] Open Developer → Recherche musiques libres de droits — missing-key banner should clear for configured providers  
5. [ ] Use network debug bar to inspect search / download traffic  

Related: [roadmap-royalty-free-music-sources.md](./roadmap-royalty-free-music-sources.md)
