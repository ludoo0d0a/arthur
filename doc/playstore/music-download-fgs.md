# Royalty-free music download FGS (`dataSync`) — deferred

**Status:** removed from shipping builds (2026-10-29) so Play API uploads are not blocked by an undeclared Foreground Service permission.

**Why:** Commit `272d2e7` added `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` and `MusicDownloadService` with `foregroundServiceType="dataSync"`. CI built the AAB, then Play rejected the edit with:

> You must let us know whether your app uses any Foreground Service permissions.

That declaration lives in Play Console (**Policy → App content → Foreground service permissions**) and cannot be completed via the upload API alone. `dataSync` needs a use-case description and a short demo video.

**Current behavior (without FGS):** `MusicDownloadService` still queues downloads with HTTP Range resume and progress notifications, started via `startService` from the royalty-free music sandbox UI while the app is foreground. The OS may stop the process if the user leaves the app mid-download; partial files under `filesDir/downloaded_music` allow resume on the next enqueue.

## Restore checklist

1. **Play Console first**
   - Upload once manually to Internal testing if the App content form is missing, then complete **Foreground service permissions**.
   - Type: **Data sync** (`FOREGROUND_SERVICE_DATA_SYNC`).
   - Describe: user starts a royalty-free track download from Developer / music sandbox; a low-priority notification shows progress until the file is stored locally.
   - Attach a short video: open music sandbox → download a track → ongoing notification → file appears in downloaded list.
   - Wait until the declaration is accepted before relying on CI uploads.

2. **Re-apply FGS in code**

   | Piece | Role |
   |-------|------|
   | Manifest `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Required permissions |
   | `android:foregroundServiceType="dataSync"` on `MusicDownloadService` | Manifest type |
   | `context.startForegroundService(...)` in `enqueue` | Start path |
   | `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_DATA_SYNC)` promptly in `onStartCommand` | Satisfy FGS timeout |
   | `stopForeground(STOP_FOREGROUND_REMOVE)` when the queue drains | Tear-down |

3. **Verify**

   ```bash
   ./gradlew :androidApp:assemblePlaystoreRelease
   # Merged playstore manifest must contain FOREGROUND_SERVICE_DATA_SYNC only after Console declaration is done.
   ```

## Related

- [Understanding foreground service and full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- `doc/playstore/foreground-service-media.md` — mediaPlayback (also deferred)
- Gaston: `docs/park-stop-fgs.md`, `docs/map-download-fgs.md`
