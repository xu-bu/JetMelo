package com.rcmiku.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.rcmiku.music.constants.newAlbumFeedKey
import com.rcmiku.music.constants.newAlbumFeedLastScanKey
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.Album
import com.rcmiku.ncmapi.model.SearchArtist
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.PriorityQueue

/** One unheard album of a followed artist, as stored in the local feed. */
@Serializable
data class FeedAlbum(
    val id: Long,
    val name: String,
    val picUrl: String,
    val artistId: Long,
    val artistNames: String,
    val publishTime: Long,
    val size: Int
)

/**
 * "Latest releases" feed: for every followed artist, walk their albums from newest to oldest and
 * keep each album until the first one that is already in the user's collected albums
 * (everything older than that is assumed to be heard). The result is stored locally and re-scanned
 * at most once every 24 hours.
 */
object AlbumFeed {
    private const val SCAN_INTERVAL_MS = 24L * 60 * 60 * 1000
    private const val PAGE_SIZE = 20
    private const val FEED_SIZE = 20
    private const val MAX_ALBUMS_PER_ARTIST = 300
    private const val PARALLEL_ARTISTS = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listSerializer = ListSerializer(FeedAlbum.serializer())

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    fun feed(context: Context): Flow<List<FeedAlbum>> =
        context.applicationContext.dataStore.data
            .map { prefs -> decode(prefs[newAlbumFeedKey]) }
            .distinctUntilChanged()

    /** Time of the last fully successful scan in epoch millis, 0 if there has been none. */
    fun lastScan(context: Context): Flow<Long> =
        context.applicationContext.dataStore.data
            .map { prefs -> prefs[newAlbumFeedLastScanKey] ?: 0L }
            .distinctUntilChanged()

    /** Starts a scan in the background if the last successful one is more than 24 hours old. */
    fun scanIfDue(context: Context) = startScan(context, force = false)

    /** Starts a scan right now, regardless of the 24 hour limit. */
    fun scanNow(context: Context) = startScan(context, force = true)

    private fun startScan(context: Context, force: Boolean) {
        val appContext = context.applicationContext
        scope.launch {
            if (!force) {
                val last = appContext.dataStore.data.first()[newAlbumFeedLastScanKey] ?: 0L
                if (System.currentTimeMillis() - last < SCAN_INTERVAL_MS) return@launch
            }
            if (!_scanning.compareAndSet(expect = false, update = true)) return@launch
            try {
                scan(appContext)
            } finally {
                _scanning.value = false
            }
        }
    }

    /** Forget the feed, e.g. after logging out. */
    fun clear(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            appContext.dataStore.edit {
                it.remove(newAlbumFeedKey)
                it.remove(newAlbumFeedLastScanKey)
            }
        }
    }

    private suspend fun scan(context: Context) {
        val artists = loadFollowedArtists() ?: return
        val collected = loadCollectedAlbumIds() ?: return
        val previous = decode(context.dataStore.data.first()[newAlbumFeedKey])

        val newest = NewestTimes(FEED_SIZE)
        val semaphore = Semaphore(PARALLEL_ARTISTS)
        val failedArtists = mutableSetOf<Long>()
        val found = coroutineScope {
            artists.map { artist ->
                async {
                    semaphore.withPermit {
                        scanArtist(artist, collected, newest) ?: run {
                            synchronized(failedArtists) { failedArtists.add(artist.id) }
                            emptyList()
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        // Keep the old entries of artists whose request failed so a bad network does not wipe them.
        val merged = (found + previous.filter { it.artistId in failedArtists })
            .distinctBy { it.id }
            .sortedByDescending { it.publishTime }
            .take(FEED_SIZE)

        context.dataStore.edit { prefs ->
            prefs[newAlbumFeedKey] = json.encodeToString(listSerializer, merged)
            // Only mark the scan as done when nothing failed, otherwise try again at the next launch.
            if (failedArtists.isEmpty()) prefs[newAlbumFeedLastScanKey] = System.currentTimeMillis()
        }
    }

    /**
     * Albums of [artist] newer than the first collected one; null if a request failed.
     * Stops early once an album is not newer than the [FEED_SIZE]-th newest album found so far
     * (it, and everything older, cannot make it into the feed).
     */
    private suspend fun scanArtist(
        artist: SearchArtist,
        collected: Set<Long>,
        newest: NewestTimes
    ): List<FeedAlbum>? {
        val result = mutableListOf<FeedAlbum>()
        var offset = 0
        while (offset < MAX_ALBUMS_PER_ARTIST) {
            val page = ArtistApi.artistAlbum(artist.id, PAGE_SIZE, offset).getOrNull() ?: return null
            for (album in page.hotAlbums.sortedByDescending { it.publishTime }) {
                if (album.id in collected) return result
                if (!newest.offer(album.publishTime)) return result
                result.add(album.toFeedAlbum(artist))
            }
            if (!page.more || page.hotAlbums.isEmpty()) break
            offset += PAGE_SIZE
            delay(100)
        }
        return result
    }

    /** Thread-safe record of the [size] newest publish times seen so far. */
    private class NewestTimes(private val size: Int) {
        private val times = PriorityQueue<Long>()

        /** Returns false if [time] can no longer be among the newest [size]; otherwise records it. */
        @Synchronized
        fun offer(time: Long): Boolean {
            if (times.size >= size && time <= times.peek()!!) return false
            times.add(time)
            if (times.size > size) times.poll()
            return true
        }
    }

    private suspend fun loadFollowedArtists(): List<SearchArtist>? {
        val all = mutableListOf<SearchArtist>()
        var offset = 0
        while (true) {
            val page = AccountApi.artistSublist(offset, 100).getOrNull() ?: return null
            all += page.data
            if (!page.hasMore || page.data.isEmpty()) return all
            offset += 100
        }
    }

    /** IDs of all albums the user has collected, or null if a request failed. */
    suspend fun loadCollectedAlbumIds(): Set<Long>? {
        val ids = mutableSetOf<Long>()
        var offset = 0
        while (true) {
            val page = AccountApi.albumSublist(offset, 100).getOrNull() ?: return null
            page.data.forEach { ids.add(it.id) }
            if (!page.hasMore || page.data.isEmpty()) return ids
            offset += 100
        }
    }

    private fun Album.toFeedAlbum(fallback: SearchArtist) = FeedAlbum(
        id = id,
        name = name,
        picUrl = picUrl,
        artistId = fallback.id,
        artistNames = artists.ifEmpty { listOf(artist) }.joinToString(" / ") { it.name }
            .ifBlank { fallback.name },
        publishTime = publishTime,
        size = size
    )

    private fun decode(text: String?): List<FeedAlbum> =
        if (text.isNullOrEmpty()) emptyList()
        else runCatching { json.decodeFromString(listSerializer, text) }.getOrDefault(emptyList())
}
