package com.moovie.tv

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.material3.Player
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TextField
import coil3.compose.AsyncImage

@Composable
fun MoovieRoot(vm: MoovieViewModel) {
    var playingUrl by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = playingUrl != null || vm.selectedMovie != null) {
        if (playingUrl != null) playingUrl = null else vm.back()
    }

    when {
        playingUrl != null -> PlayerScreen(playingUrl!!, onBack = { playingUrl = null })
        vm.selectedMovie != null -> DetailScreen(
            movie = vm.selectedMovie!!,
            loading = vm.loading,
            error = vm.error,
            onBack = vm::back,
            onPlay = { playingUrl = it }
        )
        else -> HomeScreen(vm)
    }
}

@Composable
fun HomeScreen(vm: MoovieViewModel) {
    var query by remember { mutableStateOf(vm.searchText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Moovie", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.width(12.dp))
            Text("影牛 TV", color = Color.LightGray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    vm.searchText = it
                },
                placeholder = { Text("搜索电影、电视剧、动漫…") },
                singleLine = true,
                modifier = Modifier.width(560.dp)
            )
            Button(onClick = vm::search) { Text("搜索") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryButton("电影") { vm.loadCategory("1") }
            CategoryButton("电视剧") { vm.loadCategory("2") }
            CategoryButton("综艺") { vm.loadCategory("3") }
            CategoryButton("动漫") { vm.loadCategory("4") }
        }

        if (vm.loading) Text("加载中…", color = Color.LightGray)
        vm.error?.let { Text("请求失败：$it", color = Color(0xFFFF8080), maxLines = 2) }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(vm.movies, key = { it.id }) { movie ->
                MovieCard(movie) { vm.openMovie(movie) }
            }
        }
    }
}

@Composable
private fun CategoryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(label) }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(180.dp).height(286.dp)) {
        Column {
            AsyncPoster(movie.poster, Modifier.fillMaxWidth().height(220.dp))
            Text(movie.name, modifier = Modifier.padding(10.dp), maxLines = 2)
        }
    }
}

@Composable
private fun AsyncPoster(url: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF202024)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Text("No Image", color = Color.Gray)
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DetailScreen(
    movie: Movie,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPlay: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .padding(48.dp)
    ) {
        Button(onClick = onBack) { Text("返回") }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            AsyncPoster(movie.poster, Modifier.width(260.dp).height(380.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(movie.name, style = MaterialTheme.typography.displaySmall)
                Text(
                    listOf(movie.year, movie.category, movie.remark)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    color = Color.LightGray
                )
                Text(movie.description.ifBlank { "暂无简介" }, maxLines = 7)

                if (loading) Text("正在加载播放源…", color = Color.LightGray)
                error?.let { Text("加载失败：$it", color = Color(0xFFFF8080), maxLines = 2) }

                movie.playSources.forEach { source ->
                    Text(source.source, style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(source.episodes) { _, episode ->
                            Button(onClick = { onPlay(episode.url) }) {
                                Text(episode.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(url: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exo) { onDispose { exo.release() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Player(player = exo, modifier = Modifier.fillMaxSize())
        Button(
            onClick = onBack,
            modifier = Modifier.padding(28.dp).align(Alignment.TopStart)
        ) {
            Text("返回")
        }
    }
}

@Composable
fun MoovieTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
