---v1.1.0
# Baaja 1.1.0
Every screen now follows Baaja's own look, with new features for listening and your library.

## Design
- All screens restyled: Home, Explore, Search, Library, Album, Artist, Playlists, Player, Lyrics, Queue, Profile and Settings
- Dialogs, menus, sheets and notices follow the same outlined look
- New Welcome screen and a new app logo
- Smoother animations: press effects, screen transitions, a sliding bottom bar and a springier player
- Explore shows real cover art on Charts and New releases, with a sticker on every mood
- Always four tabs at the bottom (Home, Search, Together, Library); portrait only

## New features
- Lyric preview under the artwork in the player, and an optional immersive player
- Audio output picker, optional 32-bit float output, and Automix transitions
- Smart downloads of your most-played songs
- Play music files stored on your phone
- Import playlists from Spotify (with your own Spotify key) or from a CSV file
- Edit your profile: name, tagline and photo, stored on your device
- Behind the Song: facts and your listening history for any track

## Changes
- Removed AI features, Integrations (Discord, Last.fm, ListenBrainz), Wrapped and the theme customiser
- Release notes no longer appear in Settings
- English only for now
- Listen Together shares a plain room code, and asks before first using its community server
- Spotify keys are left out of backups

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
