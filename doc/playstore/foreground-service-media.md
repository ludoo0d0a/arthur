# Foreground service / media playback (Arthur)

## Current (v1)

`ArthurMediaService` is a **browse-only** `MediaBrowserServiceCompat` for Android Auto
(catalog + metadata, playback state paused). It does **not** call `startForeground`.

Play Console required an FGS declaration only because older builds declared
`FOREGROUND_SERVICE_MEDIA_PLAYBACK` / `foregroundServiceType="mediaPlayback"` without
using them. Those declarations were removed so store review is not blocked on a
demo video for unused permissions.

## When re-adding mediaPlayback FGS

1. Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and
   `android:foregroundServiceType="mediaPlayback"` on the media service.
2. Call `startForeground` with a media notification while playing.
3. Play Console → App content → Foreground service permissions:
   - Check **Lecture de contenus multimédias**
   - Provide a public **YouTube or Drive** URL (30–60s) showing Auto / media session
     using the permission (not a slideshow of store screenshots).
