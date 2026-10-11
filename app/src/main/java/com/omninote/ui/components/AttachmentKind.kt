package com.omninote.ui.components

import android.net.Uri
import java.util.Locale

enum class AttachmentKind { IMAGE, AUDIO, FILE }

/** Prefer a provider's specific MIME type, then the real URI suffix, then its display name. */
fun attachmentKind(uri: String, name: String, mime: String? = null): AttachmentKind {
    val type = mime?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
    if (type?.startsWith("image/") == true) return AttachmentKind.IMAGE
    if (type?.startsWith("audio/") == true || type == "application/ogg") return AttachmentKind.AUDIO
    if (!type.isNullOrBlank() && type !in setOf("application/octet-stream", "binary/octet-stream", "*/*")) return AttachmentKind.FILE
    val image = setOf("jpg", "jpeg", "jpe", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
    val audio = setOf("mp3", "m4a", "aac", "wav", "wave", "ogg", "oga", "opus", "flac", "amr", "3ga")
    fun suffix(value: String) = value.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val actual = runCatching { Uri.parse(uri).lastPathSegment }.getOrNull().orEmpty()
    val extension = suffix(actual).takeIf { it.isNotEmpty() } ?: suffix(name)
    return when (extension) { in image -> AttachmentKind.IMAGE; in audio -> AttachmentKind.AUDIO; else -> AttachmentKind.FILE }
}

fun attachmentMarkdown(uri: String, name: String, mime: String? = null): String {
    val label = name.ifBlank { "Attachment" }.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]").replace('\n', ' ').replace('\r', ' ')
    val destination = uri.replace("<", "%3C").replace(">", "%3E").replace(" ", "%20").replace("\n", "%0A").replace("\r", "%0D")
    return when (attachmentKind(uri, name, mime)) {
        AttachmentKind.IMAGE -> "![$label](<$destination>)"
        AttachmentKind.AUDIO -> "[audio:$label](<$destination>)"
        AttachmentKind.FILE -> "[file:$label](<$destination>)"
    }
}
