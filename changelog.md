---v1.0.0
# Baaja 1.0.0
The first release of Baaja, built on Metrolist 13.7.0. Earlier history lives in the [Metrolist changelog](https://github.com/MetrolistGroup/Metrolist/blob/main/changelog.md).

## Highlights
- New name and identity: Baaja (बाजा), with its own app icon and themed icon
- New light design: white surfaces, bold ink outlines with offset shadows, and a purple accent, plus a matching dark mode
- Floating, outlined navigation bar and an outlined mini player
- Album-art colours still tint the app while music plays

## New features
- Sound shaping: stereo width, left/right balance, and bass and treble controls (Settings → Player)
- Separate audio quality for Wi-Fi, mobile data and downloads
- Show the codec and bitrate under the seek bar
- Skip non-music segments with SponsorBlock (off by default; only a short hash of the song ID is sent)
- Automatic daily or weekly backups to a folder you choose, keeping the 5 newest
- Heart-burst animation when you like a song
- Animated glow for the gradient player background, and optional spinning album art

## Changes from Metrolist
- Installs as its own app (`com.baaja.music`), so it can sit alongside Metrolist
- Update checks now come from this repository's releases
- Removed the Metrolist KMP upgrade prompt and Metrolist's community links
