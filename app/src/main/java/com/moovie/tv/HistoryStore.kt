package com.moovie.tv

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.historyDataStore by preferencesDataStore("moovie_history")

data class HistoryEntry(
    val movieId: String,
    val movieName: String,
    val poster: String,
    val episodeName: String,
    val url: String
)

class HistoryStore(private val context: Context) {
    private val key = stringPreferencesKey("entries")

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

    suspend fun clear() {
        context.historyDataStore.edit { it.remove(key) }
    }

    private fun encode(items: List<HistoryEntry>): String =
        items.joinToString("\n") {
            listOf(it.movieId, it.movieName, it.poster, it.episodeName, it.url)
                .joinToString("\t") { field -> field.replace("\t", " ").replace("\n", " ") }
        }

    private fun decode(value: String): List<HistoryEntry> =
        value.lineSequence().mapNotNull { line ->
            val p = line.split("\t", limit = 5)
            if (p.size == 5) HistoryEntry(p[0], p[1], p[2], p[3], p[4]) else null
        }.toList()
}
