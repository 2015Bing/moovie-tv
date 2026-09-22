package com.moovie.tv

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.material3.Player

@Composable
fun MoovieRoot(vm: MoovieViewModel) {
    when {
        vm.selectedMovie != null -> DetailScreen(vm.selectedMovie!!, vm::back)
        else -> HomeScreen(vm)
    }
}

@Composable
fun HomeScreen(vm: MoovieViewModel) {
    var query by remember { mutableStateOf("") }
    var playingUrl by remember { mutableStateOf<String?>(null) }

    if (playingUrl != null) {
        PlayerScreen(playingUrl!!, onBack = { playingUrl = null })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Moovie", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.width(12.dp))
            Text("影牛 TV", color = Color.LightGray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it; vm.searchText = it },
                placeholder = { Text("搜索电影、电视剧、动漫…") },
                modifier = Modifier.width(520.dp)
            )
            Button(onClick = vm::search) { Text("搜索") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.loadCategory("1") }) { Text("电影") }
            Button(onClick = { vm.loadCategory("2") }) { Text("电视剧") }
            Button(onClick = { vm.loadCategory("4") }) { Text("动漫") }
            Button(onClick = { vm.loadCategory("3") }) { Text("综艺") }
        }

        if (vm.loading) Text("加载中…")
        vm.error?.let { Text("请求失败：$it", color = Color(0xFFFF8080)) }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(vm.movies) { movie ->
                MovieCard(movie) { vm.openMovie(movie) }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(180.dp).height(280.dp)) {
        Column {
            AsyncPoster(movie.poster, Modifier.fillMaxWidth().height(220.dp))
            Text(movie.name, modifier = Modifier.padding(10.dp), maxLines = 2)
        }
    }
}

@Composable
private fun AsyncPoster(url: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFF202024)), contentAlignment = Alignment.Center) {
        Text(if (url.isBlank()) "No Image" else "海报")
    }
}

@Composable
private fun DetailScreen(movie: Movie, onBack: () -> Unit) {
    var playingUrl by remember { mutableStateOf<String?>(null) }
    if (playingUrl != null) {
        PlayerScreen(playingUrl!!, onBack = { playingUrl = null })
        return
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).padding(48.dp)) {
        Button(onClick = onBack) { Text("返回") }
        Spacer(Modifier.height(24.dp))
        Text(movie.name, style = MaterialTheme.typography.displaySmall)
        Text(listOf(movie.year, movie.category, movie.remark).filter { it.isNotBlank() }.joinToString(" · "), color = Color.LightGray)
        Spacer(Modifier.height(20.dp))
        Text(movie.description.ifBlank { "暂无简介" }, maxLines = 6)
        Spacer(Modifier.height(28.dp))
        movie.playSources.forEach { source ->
            Text(source.source, style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(source.episodes) { _, ep ->
                    Button(onClick = { playingUrl = ep.url }) { Text(ep.name) }
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
        Button(onClick = onBack, modifier = Modifier.padding(32.dp).align(Alignment.TopStart)) { Text("返回") }
    }
}

@Composable
fun MoovieTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
