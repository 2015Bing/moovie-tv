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
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set
    var favorites by mutableStateOf<List<FavoriteEntry>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            historyStore.entries.collectLatest { history = it }
        }
        loadCategory("1")
        viewModelScope.launch { historyStore.favoritesFlow().collectLatest { favorites = it } }
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
            viewModelScope.launch {
                loading = true
                runCatching { withContext(Dispatchers.IO) { api.detail(movie.id) } }
                    .onSuccess { detail -> if (detail != null) selectedMovie = detail }
                    .onFailure { error = it.message ?: "详情加载失败" }
                loading = false
            }
        }
    }

    fun recordPlayback(movie: Movie, episode: Episode) {
        viewModelScope.launch { historyStore.save(movie, episode) }
    }

    fun toggleFavorite(movie: Movie) { viewModelScope.launch { historyStore.toggleFavorite(movie) } }\n\n    fun clearHistory() {
        viewModelScope.launch { historyStore.clear() }
    }

    fun back() {
        selectedMovie = null
        error = null
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
