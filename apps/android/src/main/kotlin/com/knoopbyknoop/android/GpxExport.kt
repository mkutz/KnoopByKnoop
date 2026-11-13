package com.knoopbyknoop.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** The type of shared route files, see `route-sharing` "Share the route". */
const val ROUTE_FILE_TYPE = "text/markdown"

/** The type of exported GPX files, see `map-planning` "Export the route as GPX". */
const val GPX_TYPE = "application/gpx+xml"

/**
 * Writes [content] to the cache as [fileName] and offers it in the share sheet as [type], with
 * [title] above the choices, see `map-planning` "Export the route as GPX" and `route-sharing`
 * "Share the route".
 */
fun shareFile(context: Context, fileName: String, content: String, type: String, title: String) {
  val directory = File(context.cacheDir, "export").apply { mkdirs() }
  // Only the latest shared file is kept.
  directory.listFiles()?.forEach { it.delete() }
  val file = File(directory, fileName).apply { writeText(content) }
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
  val send =
    Intent(Intent.ACTION_SEND).apply {
      this.type = type
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_TITLE, fileName)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  context.startActivity(Intent.createChooser(send, title))
}
