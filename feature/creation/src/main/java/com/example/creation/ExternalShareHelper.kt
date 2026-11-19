package com.example.creation

import android.content.Context
import android.content.Intent
import android.net.Uri

object ExternalShareHelper {

    /**
     * Fluxo da Week 9:
     * File privado -> FileProvider (content:// URI) -> ACTION_SEND + FLAG_GRANT_READ_URI_PERMISSION -> createChooser
     */
    fun shareRecipeExternally(
        context: Context,
        title: String,
        description: String,
        locationTag: String,
        mediaUriString: String?,
        mediaType: MediaType?
    ) {
        val shareText = buildString {
            append("Experimenta esta receita no CookTok: $title")
            if (locationTag.isNotBlank()) append(" ($locationTag)")
            if (description.isNotBlank()) append("\n\n$description")
        }

        val uri = mediaUriString?.let { Uri.parse(it) }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "CookTok: $title")
            putExtra(Intent.EXTRA_TEXT, shareText)

            if (uri != null && uri.scheme == "content") {
                type = if (mediaType == MediaType.VIDEO) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        val chooser = Intent.createChooser(sendIntent, "Partilhar receita com...")
        context.startActivity(chooser)
    }
}