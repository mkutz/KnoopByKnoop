package com.knoopbyknoop.core

/**
 * What OpenStreetMap tells about a highlighted place beyond its kind and name, as tagged, see
 * `network-data` "Published place details". Each is `null` when it isn't tagged.
 *
 * @property website as tagged, possibly several separated by `;`
 * @property phone as tagged, possibly several separated by `;`
 * @property openingHours `opening_hours` as tagged
 */
data class PlaceDetails(val website: String?, val phone: String?, val openingHours: String?) {

  /** The first website, as the card links it: with `https://` where the scheme is missing. */
  val websiteUri: String?
    get() = first(website)?.let { if ("://" in it) it else "https://$it" }

  /** The website as the card shows it: its host, without scheme, e.g. `www.example.nl`. */
  val displayWebsite: String?
    get() = first(website)?.substringAfter("://")?.substringBefore('/')?.ifBlank { null }

  /** The first phone number as tagged, as the card shows it, see "Several numbers". */
  val displayPhone: String?
    get() = first(phone)

  /**
   * The first phone number as a `tel:` URI for the dialler: digits and a leading `+` only, without
   * an extension, and without the `(0)` some write after the country code.
   */
  val dialUri: String?
    get() {
      val number = first(phone) ?: return null
      val withoutExtension = number.split(EXTENSION, limit = 2).first()
      val withoutTrunk =
        if (withoutExtension.trimStart().startsWith("+")) withoutExtension.replace("(0)", "")
        else withoutExtension
      val digits = withoutTrunk.filter { it.isDigit() }
      if (digits.isEmpty()) return null
      return "tel:" + (if (withoutTrunk.trimStart().startsWith("+")) "+" else "") + digits
    }

  private val parsedOpeningHours: OpeningHours? by lazy { openingHours?.let(OpeningHours::parse) }

  /**
   * Whether the place is open at [now], in the local time of the covered area, and when that
   * changes, worded as for ferries, e.g. `Open until 18:00` or `Closed until tomorrow 17:00`;
   * `null` when the opening hours don't tell for sure, see `map-planning` "Opening hours on the
   * place card".
   */
  fun openingStatusAt(now: LocalMoment, language: Language = Language.EN): FerryStatus? {
    val status = parsedOpeningHours?.statusAt(now) ?: return null
    return FerryStatus(status.open, RouteFerry.statusText(status, now, language))
  }

  /** Whether there is anything to show on the card. */
  val isEmpty: Boolean
    get() = website == null && phone == null && openingHours == null

  companion object {

    private val EXTENSION = Regex("""(?i)\s*(x|ext\.?|extension|tst\.?|dw\.?)\s*\d""")

    /**
     * The details in OpenStreetMap [tags], `null` without any, see `network-data` "Contact keys".
     */
    fun ofTags(tags: Map<String, String>): PlaceDetails? {
      fun tag(vararg keys: String) = keys.firstNotNullOfOrNull { key ->
        tags[key]?.trim()?.ifEmpty { null }
      }
      return PlaceDetails(
          tag("website", "contact:website"),
          tag("phone", "contact:phone", "contact:mobile"),
          tag("opening_hours"),
        )
        .takeUnless { it.isEmpty }
    }

    private fun first(values: String?) =
      values?.split(';')?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
  }
}
