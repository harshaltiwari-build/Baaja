package com.metrolist.music.utils

/** The changelog is shown when the stored "last seen" version differs from the running version. */
fun shouldShowChangelog(lastSeenVersion: String?, currentVersion: String): Boolean =
    (lastSeenVersion ?: "") != currentVersion
