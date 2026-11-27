package com.knoopbyknoop.web

import com.knoopbyknoop.core.FerryStatus
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.LocalMoment
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.PlaceDetails

/**
 * What the card of a clicked place shows, as the apps' place card, see `web-planner` "Places in the
 * planner": its name (or its kind without one), its kind, whether it is open [now], its opening
 * hours as tagged, its website and phone number, as far as known.
 */
data class PlaceCard(
  val title: String,
  val kind: String?,
  val status: FerryStatus?,
  val hours: String?,
  val website: String?,
  val websiteText: String?,
  val dial: String?,
  val phoneText: String?,
) {
  companion object {
    fun of(
      place: MapPlace,
      details: PlaceDetails?,
      now: LocalMoment,
      language: Language,
      text: (String) -> String,
    ): PlaceCard {
      val kind = text("place.${place.kindKey}")
      return PlaceCard(
        title = place.name ?: kind,
        kind = if (place.name != null) kind else null,
        status = details?.openingStatusAt(now, language),
        hours = details?.openingHours,
        website = details?.websiteUri,
        websiteText = details?.displayWebsite,
        dial = details?.dialUri,
        phoneText = details?.displayPhone,
      )
    }
  }
}
