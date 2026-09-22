package com.moovie.tv

data class Movie(
    val id: String,
    val name: String,
    val poster: String = "",
    val year: String = "",
    val remark: String = "",
    val category: String = "",
    val description: String = "",
    val playSources: List<PlaySource> = emptyList()
)

data class PlaySource(
    val source: String,
    val episodes: List<Episode>
)

data class Episode(val name: String, val url: String)
