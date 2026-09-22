package com.moovie.tv

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoovieViewModel : ViewModel() {
    private val api = MoovieApi()

    var searchText by mutableStateOf("")
    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var selectedMovie by mutableStateOf<Movie?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
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
            viewModelScope.launch {
                loading = true
                runCatching { withContext(Dispatchers.IO) { api.detail(movie.id) } }
                    .onSuccess { detail -> if (detail != null) selectedMovie = detail }
                    .onFailure { error = it.message ?: "详情加载失败" }
                loading = false
            }
        }
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
