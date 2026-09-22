package com.moovie.tv

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.historyDataStore by preferencesDataStore("moovie_history")

data class FavoriteEntry(val movieId: String, val movieName: String, val poster: String)

data class HistoryEntry(
    val movieId: String,
    val movieName: String,
    val poster: String,
    val episodeName: String,
    val url: String
)

class HistoryStore(private val context: Context) {
    private val key = stringPreferencesKey("entries")
    private val favoriteKey = stringPreferencesKey("favorites")
    private val positionKey = stringPreferencesKey("positions")

    val entries: Flow<List<HistoryEntry>> =
        context.historyDataStore.data.map { prefs -> decode(prefs[key].orEmpty()) }

    suspend fun save(movie: Movie, episode: Episode) {
        context.historyDataStore.edit { prefs ->
            val old = decode(prefs[key].orEmpty())
                .filterNot { it.movieId == movie.id && it.episodeName == episode.name }
            val updated = (listOf(
                HistoryEntry(movie.id, movie.name, movie.poster, episode.name, episode.url)
            ) + old).take(30)
            prefs[key] = encode(updated)
        }
    }

    suspend fun savePosition(url: String, positionMs: Long) {
        if (url.isBlank()) return
        context.historyDataStore.edit { prefs ->
            val positions = decodePositions(prefs[positionKey].orEmpty()).toMutableMap()
            if (positionMs <= 10_000L) positions.remove(url) else positions[url] = positionMs
            prefs[positionKey] = encodePositions(positions)
        }
    }

    suspend fun getPosition(url: String): Long =
        context.historyDataStore.data.map { prefs ->
            decodePositions(prefs[positionKey].orEmpty())[url] ?: 0L
        }.first()

    suspend fun clear() {
        context.historyDataStore.edit { it.remove(key).remove(positionKey) }
    }

    suspend fun toggleFavorite(movie: Movie) {
        context.historyDataStore.edit { prefs ->
            val current = decodeFavorites(prefs[favoriteKey].orEmpty()).toMutableList()
            val existing = current.indexOfFirst { it.movieId == movie.id }
            if (existing >= 0) current.removeAt(existing)
            else current.add(0, FavoriteEntry(movie.id, movie.name, movie.poster))
            prefs[favoriteKey] = encodeFavorites(current.take(100))
        }
    }

    fun favoritesFlow(): Flow<List<FavoriteEntry>> =
        context.historyDataStore.data.map { decodeFavorites(it[favoriteKey].orEmpty()) }

    private fun encode(items: List<HistoryEntry>): String =
        items.joinToString("\n") {
            listOf(it.movieId, it.movieName, it.poster, it.episodeName, it.url)
                .joinToString("\t") { field -> field.replace("\t", " ").replace("\n", " ") }
        }

    private fun encodeFavorites(items: List<FavoriteEntry>): String =
        items.joinToString("\n") {
            listOf(it.movieId, it.movieName, it.poster).joinToString("\t")
        }

    private fun encodePositions(items: Map<String, Long>): String =
        items.entries.joinToString("\n") { (url, position) ->
            listOf(url, position.toString()).joinToString("\t")
        }

    private fun decodeFavorites(value: String): List<FavoriteEntry> =
        value.lineSequence().mapNotNull { line ->
            val p = line.split("\t", limit = 3)
            if (p.size == 3) FavoriteEntry(p[0], p[1], p[2]) else null
        }.toList()

    private fun decode(value: String): List<HistoryEntry> =
        value.lineSequence().mapNotNull { line ->
            val p = line.split("\t", limit = 5)
            if (p.size == 5) HistoryEntry(p[0], p[1], p[2], p[3], p[4]) else null
        }.toList()

    private fun decodePositions(value: String): Map<String, Long> =
        value.lineSequence().mapNotNull { line ->
            val p = line.split("\t", limit = 2)
            val position = p.getOrNull(1)?.toLongOrNull()
            if (p.size == 2 && p[0].isNotBlank() && position != null && position > 0) p[0] to position else null
        }.toMap()
}
