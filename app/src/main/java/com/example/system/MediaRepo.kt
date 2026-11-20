package com.example.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import com.example.model.MediaStreamItem
import com.example.service.MediaListenerService

data class MediaSnapshot(
    val streams: List<MediaStreamItem>,
    val isPlaying: Boolean,
    val title: String,
    val artist: String,
    val totalSeconds: Int,
    val currentSeconds: Int
)

/** Controls whatever is really playing (YouTube, Spotify...). Needs Notification access for this app. */
class MediaRepo(private val ctx: Context) {
    private val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(ctx, MediaListenerService::class.java)
    private var selectedPkg: String? = null

    private fun sessions(): List<MediaController> = try {
        msm.getActiveSessions(component)
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun current(list: List<MediaController>): MediaController? =
        list.firstOrNull { it.packageName == selectedPkg }
            ?: list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull()

    private fun label(pkg: String): String = try {
        ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    private fun positionMs(ps: PlaybackState?): Long {
        if (ps == null) return 0L
        var p = ps.position
        if (ps.state == PlaybackState.STATE_PLAYING) {
            p += ((SystemClock.elapsedRealtime() - ps.lastPositionUpdateTime) * ps.playbackSpeed).toLong()
        }
        return p.coerceAtLeast(0L)
    }

    private fun fmt(ms: Long): String {
        val s = (ms / 1000).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun snapshot(): MediaSnapshot {
        val list = sessions()
        val cur = current(list) ?: return MediaSnapshot(
            emptyList(), false, "Nothing playing", "Turn on Media access and start a player", 0, 0
        )
        val md = cur.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "Unknown title"
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: label(cur.packageName)
        val durMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val posMs = positionMs(cur.playbackState)
        val streams = list.map { c ->
            val m = c.metadata
            val d = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val p = positionMs(c.playbackState)
            MediaStreamItem(
                c.packageName, label(c.packageName),
                m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown title",
                m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                if (d > 0) fmt(d) else "--:--",
                c.packageName == cur.packageName, "music",
                if (d > 0) (p.toFloat() / d).coerceIn(0f, 1f) else 0f,
                isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING
            )
        }
        return MediaSnapshot(
            streams, cur.playbackState?.state == PlaybackState.STATE_PLAYING,
            title, artist, (durMs / 1000).toInt(), (posMs / 1000).toInt()
        )
    }

    fun playPause(): Boolean {
        val c = current(sessions()) ?: return false
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
        return true
    }

    fun next(): Boolean { val c = current(sessions()) ?: return false; c.transportControls.skipToNext(); return true }
    fun previous(): Boolean { val c = current(sessions()) ?: return false; c.transportControls.skipToPrevious(); return true }

    fun seekTo(seconds: Int): Boolean {
        val c = current(sessions()) ?: return false
        c.transportControls.seekTo(seconds * 1000L)
        return true
    }

    fun skipBy(deltaSeconds: Int): Boolean {
        val c = current(sessions()) ?: return false
        val dur = c.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val target = (positionMs(c.playbackState) + deltaSeconds * 1000L)
            .coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
        c.transportControls.seekTo(target)
        return true
    }

    fun select(pkg: String) { selectedPkg = pkg }
}
