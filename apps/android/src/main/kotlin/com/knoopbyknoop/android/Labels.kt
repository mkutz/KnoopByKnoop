package com.knoopbyknoop.android

import androidx.annotation.StringRes
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.KnotRole

/** The network's name in the app's language, see `app-languages` "Language of the texts". */
@get:StringRes
val IndexedNetworkType.label: Int
  get() =
    when (this) {
      IndexedNetworkType.CYCLING -> R.string.network_cycling
      IndexedNetworkType.HIKING -> R.string.network_hiking
    }

@get:StringRes
val Appearance.label: Int
  get() =
    when (this) {
      Appearance.SYSTEM -> R.string.appearance_system
      Appearance.LIGHT -> R.string.appearance_light
      Appearance.DARK -> R.string.appearance_dark
    }

/** A waypoint's role in the waypoint list. */
@get:StringRes
val KnotRole.label: Int
  get() =
    when (this) {
      KnotRole.START,
      KnotRole.ROUND_TRIP -> R.string.role_start
      KnotRole.END -> R.string.role_end
      else -> R.string.role_stop
    }
