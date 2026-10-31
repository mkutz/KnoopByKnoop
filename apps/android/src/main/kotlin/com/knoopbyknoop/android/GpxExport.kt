package com.knoopbyknoop.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes [content] to the cache as [fileName] and offers it in the share sheet, see `map-planning`
 * "Export the route as GPX".
 */
fun shareGpx(context: Context, fileName: String, content: String) {
  val directory = File(context.cacheDir, "export").apply { mkdirs() }
  // Only the latest export is kept.
  directory.listFiles()?.forEach { it.delete() }
  val file = File(directory, fileName).apply { writeText(content) }
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
  val send =
    Intent(Intent.ACTION_SEND).apply {
      type = "application/gpx+xml"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_TITLE, fileName)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  context.startActivity(Intent.createChooser(send, "Export GPX"))
}
