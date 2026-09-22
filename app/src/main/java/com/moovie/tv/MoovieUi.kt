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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.material3.Player as MediaPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TextField
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun MoovieRoot(vm: MoovieViewModel) {
    var playing by remember { mutableStateOf<PlaybackItem?>(null) }

    LaunchedEffect(vm.resumePlayback) {
        vm.resumePlayback?.let {
            playing = it
            vm.consumeResumePlayback()
        }
    }

    BackHandler(enabled = playing != null || vm.selectedMovie != null) {
        if (playing != null) playing = null else vm.back()
    }

    when {
        playing != null -> PlayerScreen(
            item = playing!!,
            onBack = { playing = null },
            onNext = { next -> playing = next },
            onPlayed = { vm.recordPlayback(it.movie, it.episode) },
            onSavePosition = vm::savePlaybackPosition,
            onLoadPosition = vm::getPlaybackPosition
        )
        vm.selectedMovie != null -> {
            val movie = vm.selectedMovie!!
            DetailScreen(
                movie = movie,
                loading = vm.loading,
                error = vm.error,
                onBack = vm::back,
                onPlay = { playing = it },
                isFavorite = vm.favorites.any { it.movieId == movie.id },
                onToggleFavorite = { vm.toggleFavorite(movie) }
            )
        }
        else -> HomeScreen(vm)
    }
}

@Composable
fun HomeScreen(vm: MoovieViewModel) {
    var query by remember { mutableStateOf(vm.searchText) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0D))
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
                onValueChange = { query = it; vm.searchText = it },
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

        if (vm.history.isNotEmpty()) {
            Text("继续观看", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(vm.history, key = { it.movieId + it.episodeName }) { entry ->
                    HistoryCard(entry) { vm.openHistory(entry) }
                }
            }
        }

        if (vm.favorites.isNotEmpty()) {
            Text("我的收藏", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                items(vm.favorites, key = { it.movieId }) { entry ->
                    MovieCard(Movie(entry.movieId, entry.movieName, entry.poster)) {
                        vm.openMovie(Movie(entry.movieId, entry.movieName, entry.poster))
                    }
                }
            }
        }

        Text("影片", style = MaterialTheme.typography.titleLarge)
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
private fun HistoryCard(entry: HistoryEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(180.dp).height(250.dp)) {
        Column {
            AsyncPoster(entry.poster, Modifier.fillMaxWidth().height(190.dp))
            Text(entry.movieName, modifier = Modifier.padding(8.dp), maxLines = 1)
            Text(entry.episodeName, modifier = Modifier.padding(horizontal = 8.dp), color = Color.LightGray, maxLines = 1)
        }
    }
}

@Composable
private fun AsyncPoster(url: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFF202024)), contentAlignment = Alignment.Center) {
        if (url.isBlank()) Text("No Image", color = Color.Gray)
        else AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DetailScreen(
    movie: Movie,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPlay: (PlaybackItem) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).padding(48.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) { Text("返回") }
            Button(onClick = onToggleFavorite) { Text(if (isFavorite) "取消收藏" else "收藏") }
        }
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
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.LightGray
                )
                Text(movie.description.ifBlank { "暂无简介" }, maxLines = 7)
                if (loading) Text("正在加载播放源…", color = Color.LightGray)
                error?.let { Text("加载失败：$it", color = Color(0xFFFF8080), maxLines = 2) }

                movie.playSources.forEach { source ->
                    Text(source.source, style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(source.episodes) { index, episode ->
                            Button(onClick = { onPlay(PlaybackItem(movie, source, index)) }) {
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
private fun PlayerScreen(
    item: PlaybackItem,
    onBack: () -> Unit,
    onNext: (PlaybackItem) -> Unit,
    onPlayed: (PlaybackItem) -> Unit,
    onSavePosition: (String, Long) -> Unit,
    onLoadPosition: suspend (String) -> Long
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ended by remember(item.episode.url) { mutableStateOf(false) }
    var countdown by remember(item.episode.url) { mutableIntStateOf(5) }
    var resumePosition by remember(item.episode.url) { mutableLongStateOf(0L) }

    val exo = remember(item.episode.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(item.episode.url)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exo, item.episode.url) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    ended = true
                    onSavePosition(item.episode.url, 0L)
                }
            }
        }
        exo.addListener(listener)
        onPlayed(item)
        onDispose {
            onSavePosition(item.episode.url, exo.currentPosition)
            exo.removeListener(listener)
            exo.release()
        }
    }

    LaunchedEffect(item.episode.url) {
        resumePosition = onLoadPosition(item.episode.url)
        while (true) {
            delay(5_000)
            if (exo.isPlaying) onSavePosition(item.episode.url, exo.currentPosition)
        }
    }

    LaunchedEffect(exo, resumePosition) {
        if (resumePosition > 10_000L) {
            while (exo.playbackState == Player.STATE_BUFFERING || exo.playbackState == Player.STATE_IDLE) {
                delay(200)
            }
            if (exo.duration <= 0L || resumePosition < exo.duration - 10_000L) {
                exo.seekTo(resumePosition)
            }
        }
    }

    LaunchedEffect(ended, item.episode.url) {
        if (ended && item.nextEpisode != null) {
            countdown = 5
            while (countdown > 0 && ended) {
                delay(1000)
                countdown--
            }
            if (ended) onNext(PlaybackItem(item.movie, item.source, item.episodeIndex + 1))
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MediaPlayer(player = exo, modifier = Modifier.fillMaxSize())

        Button(onClick = onBack, modifier = Modifier.padding(28.dp).align(Alignment.TopStart)) {
            Text("返回")
        }

        if (resumePosition > 10_000L && exo.currentPosition < 10_000L) {
            Text(
                "继续播放 ${resumePosition / 60_000} 分钟处",
                modifier = Modifier.align(Alignment.TopEnd).padding(32.dp),
                color = Color.White
            )
        }

        if (ended && item.nextEpisode != null) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(40.dp).background(Color(0xDD151519)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("即将播放：${item.nextEpisode.name}")
                Text("${countdown} 秒后自动播放")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        onNext(PlaybackItem(item.movie, item.source, item.episodeIndex + 1))
                    }) { Text("立即播放") }
                    Button(onClick = { ended = false }) { Text("取消") }
                }
            }
        } else if (ended) {
            Text(
                "本线路已播放完毕",
                modifier = Modifier.align(Alignment.BottomCenter).padding(40.dp),
                color = Color.White
            )
        }
    }
}

@Composable
fun MoovieTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
