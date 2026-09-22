package com.moovie.tv

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoovieViewModel(app: Application) : AndroidViewModel(app) {
    private val api = MoovieApi()
    private val historyStore = HistoryStore(app)

    var searchText by mutableStateOf("")
    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var selectedMovie by mutableStateOf<Movie?>(null)
        private set
    var resumePlayback by mutableStateOf<PlaybackItem?>(null)
        private set
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set
    var favorites by mutableStateOf<List<FavoriteEntry>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { historyStore.entries.collectLatest { history = it } }
        viewModelScope.launch { historyStore.favoritesFlow().collectLatest { favorites = it } }
        loadCategory("1")
    }

    fun search() {
        if (searchText.isBlank()) return
        load { api.search(searchText.trim()) }
    }

    fun loadCategory(type: String) {
        load { api.category(type) }
    }

    fun openMovie(movie: Movie) {
        selectedMovie = movie
        error = null
        if (movie.playSources.isEmpty()) {
            viewModelScope.launch { loadDetail(movie.id) }
        }
    }

    fun openHistory(entry: HistoryEntry) {
        error = null
        viewModelScope.launch {
            loading = true
            runCatching { withContext(Dispatchers.IO) { api.detail(entry.movieId) } }
                .onSuccess { detail ->
                    if (detail == null) {
                        error = "影片详情不存在"
                        return@onSuccess
                    }
                    selectedMovie = detail
                    val match = detail.playSources.asSequence()
                        .mapNotNull { source ->
                            val index = source.episodes.indexOfFirst {
                                it.url == entry.url || it.name == entry.episodeName
                            }
                            if (index >= 0) source to index else null
                        }
                        .firstOrNull()
                    if (match != null) {
                        resumePlayback = PlaybackItem(detail, match.first, match.second)
                    } else {
                        error = "未找到原播放集数，请从详情页重新选择"
                    }
                }
                .onFailure { error = it.message ?: "详情加载失败" }
            loading = false
        }
    }

    fun consumeResumePlayback() {
        resumePlayback = null
    }

    fun recordPlayback(movie: Movie, episode: Episode) {
        viewModelScope.launch { historyStore.save(movie, episode) }
    }

    fun savePlaybackPosition(url: String, positionMs: Long) {
        viewModelScope.launch { historyStore.savePosition(url, positionMs) }
    }

    suspend fun getPlaybackPosition(url: String): Long = historyStore.getPosition(url)

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { historyStore.toggleFavorite(movie) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyStore.clear() }
    }

    fun back() {
        selectedMovie = null
        error = null
    }

    private suspend fun loadDetail(id: String) {
        loading = true
        runCatching { withContext(Dispatchers.IO) { api.detail(id) } }
            .onSuccess { detail -> if (detail != null) selectedMovie = detail }
            .onFailure { error = it.message ?: "详情加载失败" }
        loading = false
    }

    private fun load(block: suspend () -> List<Movie>) {
        viewModelScope.launch {
            loading = true
            error = null
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onSuccess { movies = it }
                .onFailure { error = it.message ?: "加载失败" }
            loading = false
        }
    }
}
