package com.avitrisphare.whiteboard.presentation.canvas

private val youtubeId = Regex("""(?:v=|youtu\.be/|embed/|shorts/)([A-Za-z0-9_-]{11})""")

fun youtubeVideoId(raw: String): String? {
    return youtubeId.find(raw.trim())?.groupValues?.getOrNull(1)
}

fun youtubeEmbedUrl(raw: String): String? {
    val id = youtubeVideoId(raw) ?: return null
    return "https://www.youtube.com/embed/$id?autoplay=1&playsinline=1&rel=0"
}

fun normalizeWebUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "https://www.google.com"
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}

fun googleImageSearchUrl(query: String): String {
    val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    return "https://www.google.com/search?tbm=isch&q=$q"
}

fun youtubeSearchUrl(query: String): String {
    val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    return "https://m.youtube.com/results?search_query=$q"
}
