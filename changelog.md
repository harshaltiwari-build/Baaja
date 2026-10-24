---v1.1.2
# Baaja 1.1.2
Updates now install from inside the app, plus a round of fixes.

## New features
- In-app updater: download the new version and install it without leaving Baaja. The file is checked to be Baaja's own before the installer opens

## Changes
- Update checks now read the release version, so new versions are found reliably
- The settings button is now an outlined circle that matches the profile picture, and Explore has it too
- The sign-in page no longer has its top covered by the title bar
- Charts fill the screen with trending songs when charts are not available in your region
- Listen Together: a short "How it works" card sits at the top and connecting happens when you start or join a room
- An empty Library now explains how to add playlists
- Lyrics options have their own page in Appearance
- People updating Baaja skip the Welcome screen
- The profile dot only shows when a newer version exists
- The launch screen shows the whole logo tile
- Your sign-in is no longer included in cloud backups
- The artist page has a larger photo with the name on it, and Play, Subscribe and radio on one row
- Plain-HTTP traffic is now blocked except to this device, and one background receiver is no longer open to other apps
- A new launch animation: the logo stays exactly where the system splash leaves it, rises as the scene pops in, then a circle of purple closes onto it and the logo flies up into the Home header
- No white flash behind screens while they change, in dark mode too
- The volume slider is a bold outlined bar with a speaker button and the level shown, and the Identify song and Behind the song pages no longer start with a big gap
- Behind the song has a Go to artist button
- About says Baaja is designed and built by Harshal Tiwari and credits Metrolist, InnerTune and OuterTune
- Hide explicit content, hide video songs and hide YouTube Shorts are on by default (change them any time in Settings > Content)
- Removed Import from Spotify, since it could not be made reliable

---v1.1.1
# Baaja 1.1.1
A small clean-up after 1.1.0.

## Changes
- Queue settings: "Keep playing similar music" now also covers similar content, so there is one switch instead of two
- Removed the "App language" row from Settings, since Baaja is English only

---v1.1.0
# Baaja 1.1.0
Every screen now follows Baaja's own look, with new features for listening and your library.

## Design
- All screens restyled: Home, Explore, Search, Library, Album, Artist, Playlists, Player, Lyrics, Queue, Profile and Settings
- Dialogs, menus, sheets and notices follow the same outlined look
- A new animated Splash, Welcome and Login flow, and a new app logo
- Speed dial on Home restyled, with a "Surprise me" card
- Smoother animations: press effects, screen transitions, a sliding bottom bar and a springier player
- Explore shows real cover art on Charts and New releases, with a sticker on every mood
- Always four tabs at the bottom (Home, Search, Together, Library); portrait only
- The app's colours are fixed to Baaja's own and no longer change with the music you play
- Mini player: one design with two background styles, follow theme or album gradient
- Player: one play button colour, and two seek bar styles (wavy and squiggly)

## New features
- Lyric preview under the artwork in the player, and an optional immersive player
- Audio output picker, optional 32-bit float output, and Automix transitions
- Smart downloads of your most-played songs
- Play music files stored on your phone
- Import playlists from Spotify (with your own Spotify key) or from a CSV file
- Import playlists from YouTube Music by link, or pick from your own playlists when signed in
- Charts and New releases now show a loading state, a fallback and a Try again button
- Edit your profile: name, tagline and photo, stored on your device
- Behind the Song: facts and your listening history for any track

## Changes
- Removed AI features, Integrations (Discord, Last.fm, ListenBrainz), Wrapped and the theme customiser
- Release notes no longer appear in Settings
- Settings simplified: the Player page is split into Playback, Player, Queue and Timers & alarms; Sound shaping moved into the Equalizer; one Transition style picker; one "Keep playing similar music" switch; proxy, lyrics provider choices and several technical switches removed
- English only for now
- Listen Together shares a plain room code, and asks before first using its community server
- Spotify keys are left out of backups

---v1.0.0
# Baaja 1.0.0
The first release of Baaja.

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

## Changes
- Installs as its own app (`com.baaja.music`)
- Update checks now come from this repository's releases
- Removed the old upgrade prompt and external community links
