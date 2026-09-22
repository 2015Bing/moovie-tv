package com.moovie.tv

import android.net.Uri
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MoovieApi(
    private val baseUrl: String = "https://moovie.c2v2.com"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun search(keyword: String): List<Movie> {
        require(keyword.isNotBlank()) { "请输入搜索关键词" }
        return request("/api/vod", mapOf("wd" to keyword.trim(), "pg" to "1")).let(::parseMovies)
    }

    fun category(type: String, page: Int = 1): List<Movie> =
        request("/api/vod", mapOf("t" to type, "pg" to page.coerceAtLeast(1).toString()))
            .let(::parseMovies)

    fun detail(id: String): Movie? =
        request("/api/vod", mapOf("ac" to "detail", "ids" to id))
            .let(::parseMovies)
            .firstOrNull()

    fun imageUrl(url: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith(baseUrl)) return url
        return baseUrl.trimEnd('/') + "/api/proxy/image/" + Uri.encode(url)
    }

    private fun request(path: String, params: Map<String, String>): JsonObject {
        val builder = (baseUrl.trimEnd('/') + path).toHttpUrl().newBuilder()
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val request = Request.Builder()
            .url(builder.build())
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) error("服务返回空数据")
            return JsonParser.parseString(body).asJsonObject
        }
    }

    private fun parseMovies(root: JsonObject): List<Movie> {
        val list = root.getAsJsonArray("list")
            ?: root.getAsJsonArray("data")
            ?: root.getAsJsonArray("vod")
            ?: return emptyList()

        return list.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val o = element.asJsonObject
            val id = first(o, "vod_id", "id") ?: return@mapNotNull null
            Movie(
                id = id,
                name = first(o, "vod_name", "name", "title").orEmpty(),
                poster = first(o, "vod_pic", "pic", "poster").orEmpty(),
                year = first(o, "vod_year", "year").orEmpty(),
                remark = first(o, "vod_remarks", "remarks").orEmpty(),
                category = first(o, "vod_class", "class", "type_name").orEmpty(),
                description = first(o, "vod_content", "content", "desc").orEmpty(),
                playSources = parseSources(o)
            )
        }
    }

    private fun parseSources(o: JsonObject): List<PlaySource> {
        val from = first(o, "vod_play_from").orEmpty()
        val urls = first(o, "vod_play_url").orEmpty()
        if (urls.isBlank()) return emptyList()

        val sourceNames = from.split("\$\$").map { it.trim() }
        val sourceBlocks = urls.split("\$\$")

        return sourceBlocks.mapIndexed { index, block ->
            val source = sourceNames.getOrNull(index).orEmpty().ifBlank { "线路 ${index + 1}" }
            val episodes = block.split("#").mapNotNull { item ->
                val pair = item.split("$", limit = 2)
                if (pair.size == 2 && pair[1].isNotBlank()) {
                    Episode(pair[0].ifBlank { "播放" }, pair[1])
                } else null
            }
            PlaySource(source, episodes)
        }.filter { it.episodes.isNotEmpty() }
    }

    private fun first(o: JsonObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            o.get(key)?.takeUnless { it.isJsonNull }?.let { value ->
                if (value.isJsonPrimitive) value.asString else null
            }
        }
}
