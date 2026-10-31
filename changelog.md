---v1.3.3
# Baaja 1.3.3
Better sound, a Home like YouTube Music's, and a calmer look.

## Sound
- "High" quality now really plays the best stream on mobile data too (before, it quietly used a 48 kbps stream)
- Songs played once at low quality are fetched again in better quality when you want it
- The player shows what is playing, like "OPUS · 154 kbps", and files on your phone show their quality too
- "Reset sound to default" in the Equalizer now also turns the equalizer off

## Home and recommendations
- Signed in, Home shows your YouTube Music rows first (Quick picks, Listen again, Mixed for you), just like YouTube Music
- YouTube Music's own Speed dial joins Baaja's, so it no longer shows up twice
- Quick picks mixes in new songs, and skips the ones already on Speed dial
- New "Not interested" in song menus: YouTube Music suggests less like it
- Home refreshes when you come back after a while, and after signing in or out
- Search shows trending searches before you type
- Charts work again, music only: Top 100 Songs India, Trending 20, daily and weekly song charts by language, and a ranked Top artists list (before, India got "Charts aren't available in your region")
- Opening a chart shows its songs as audio songs in chart order, with Play all and Shuffle; no music videos
- New releases matches YouTube Music: the featured playlist and "Albums & singles" in YouTube's order

## Design
- Speed dial titles sit on the artwork and fit on two lines
- Share and Like on the player are round buttons, so long song titles fit
- The mini player has a Next button
- Lighter chips, and the bar at the bottom no longer lets songs peek through
- Page titles line up with the content below them
- Empty Liked and Downloaded tiles tell you how to fill them
- The Immersive player is gone

## Fixes
- Fixed a crash on Home when two artists you listen to share an album, and the same risk in search suggestions
- A shared song link opened while Baaja is closed now plays that song instead of your old queue
- Tapping two songs quickly plays the one you tapped last
- Songs whose best quality is already low are no longer downloaded again on every play
- Signing out and back in quickly no longer leaves Home on the old account
- "Not interested" on a liked song removes the like here too
- Switching from Home to Search no longer jumps
- The crash screen can open a ready-to-send GitHub report, with account cookies, tokens, emails and file names blanked out first

---v1.3.2
# Baaja 1.3.2
Downloads you can trust, and menus that open every time.

## Downloads
- The Downloaded bar now shows your download as it happens: "Downloading 61 of 202 songs", and the size goes up live
- Stopping a playlist download only stops the songs still waiting; songs already downloaded stay
- Big playlists queue up much faster, so they no longer stall partway
- Downloads left waiting when the app closed carry on by themselves when you open it again
- The Downloaded count on Library now matches the number inside Downloaded

## Fixes
- The ⋮ menu on Liked and Downloaded opens again (it only dimmed the screen before)
- Song, playlist and selection menus open at the right height instead of jumping up and down
- "Remove from library" now says "Take it out of your library" underneath
- Home: the Your mix card lines up properly and the whole card plays your mix when tapped
- Artist mix tiles on Home are all the same height
- Speed dial no longer repeats the artists already shown at the top of Home (artists you pin yourself stay)

---v1.3.1
# Baaja 1.3.1
Home, Explore and Library follow your YouTube Music account, plus a round of fixes.

## Changes
- Signed in with Google, Home is built from your YouTube Music listening history, the artists you follow and your liked songs, from the very first launch
- New "Because you liked…" shelves on Home, built from your most recent likes
- Home starts with Speed dial, like YouTube Music, followed by your mix, artist mixes and "Jump back in"
- The Podcasts chip is gone from Home; the mood chips come from your account
- A new look for the Surprise me tile
- Artist photos now show in Your mix, artist mixes and your top artists on Home and Profile
- Removed the home screen widgets
- The Baaja GitHub page moved to github.com/harshaltiwari-build/Baaja and the website to harshaltiwari-build.github.io/Baaja

## Fixes
- Explore: New releases no longer comes up empty, and Charts keeps trying until it loads
- Explore scrolls all the way down past the mini player
- Mood and genre pages: song covers no longer cover the titles or overlap each other
- Menus (like the one on Liked songs) are only as tall as their options, with no empty white area

---v1.3.0
# Baaja 1.3.0
Widgets that do real jobs, in Baaja's own look.

## New features
- Eight home screen widgets: Now playing, Resume, Sleep timer, Music alarm, Up next, Search and Identify, Quick play and Listen together
- Now playing shows the song, how far you are, a heart, previous, play, next and a sleep button, in light and dark
- Resume picks up the last song with one tap
- Sleep timer: tap 15, 30 or 60 minutes or the end of the song, see what is left, add 15 minutes or cancel
- Music alarm: see the next alarm and switch it on or off without opening the app
- Up next lists the next songs and a tap jumps to one
- Search and Identify opens search, or starts naming a song playing around you
- Quick play starts your liked songs, your downloads or your top songs in one tap
- Listen together shows your room code and lets you share it

## Changes
- The older player widgets now use the Baaja look too
- The artist picker and mixes fit small phones and large text sizes

---v1.2.0
# Baaja 1.2.0
Home now learns your taste, starting from the moment you open the app.

## New features
- Tell Baaja what you like: after "Continue as guest" you pick your music languages and then at least 5 artists. Every pick brings in more artists like it
- A Home made for you: a "Your mix" card, an artist mix for each artist you picked, and "Because you like…" rows you can scroll sideways
- Home keeps learning by itself. At first it follows your picks, then it adds what you listen to ("Listen again", "Because you listened to…"), and after a while your listening leads, with new releases from artists you play
- Settings > Content has Music languages and Favourite artists, so you can change your picks or clear them all at any time
- After you sign in with Google, a Welcome back screen shows your account and what is being brought over from YouTube Music
- About has Website and GitHub buttons

## Changes
- Mixes are longer: each artist now brings up to 40 songs
- The Google sign-in button shows the real Google logo
- Lyrics you are not on are easier to read
- Listen Together fills in your name from your profile and the Start a room button is always ready
- The Explore charts tile tries again if the first load fails
- Artist names under avatars can wrap to two lines

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
- Every screen stays upright, whatever your phone's auto-rotate is set to
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
