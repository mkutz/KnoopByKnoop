package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaceDetailsTest {

  private fun phone(tagged: String) = PlaceDetails(null, tagged, null)

  private fun website(tagged: String) = PlaceDetails(tagged, null, null)

  @Test
  fun `Website without a scheme opens with https`() {
    val details = website("www.example.nl")
    assertEquals("https://www.example.nl", details.websiteUri)
    assertEquals("www.example.nl", details.displayWebsite)
  }

  @Test
  fun `a website with a scheme opens as tagged and shows its host`() {
    val details = website("https://manegegrol.nl/voorbeeld-pagina/")
    assertEquals("https://manegegrol.nl/voorbeeld-pagina/", details.websiteUri)
    assertEquals("manegegrol.nl", details.displayWebsite)
    assertEquals("http://www.b.de", website("http://www.b.de").websiteUri)
  }

  // Scenario: Several numbers
  @Test
  fun `Several numbers show and dial the first`() {
    val details = phone("+32 475 26 46 61;+32 471 27 49 11")
    assertEquals("+32 475 26 46 61", details.displayPhone)
    assertEquals("tel:+32475264661", details.dialUri)
  }

  @Test
  fun `phone numbers as tagged in Zeeland dial as digits`() {
    assertEquals("tel:+31111461623", phone("+31 111 461 623").dialUri)
    assertEquals("tel:+31111461210", phone("+31 1 11 46 12 10").dialUri)
    assertEquals("tel:+31111462510", phone("+31111462510").dialUri)
  }

  @Test
  fun `the trunk zero after a country code is left out`() {
    assertEquals("tel:+31113123456", phone("+31 (0)113 123456").dialUri)
  }

  @Test
  fun `a national number keeps its leading zero`() {
    assertEquals("tel:0113123456", phone("0113-123456").dialUri)
    assertEquals("tel:022411234", phone("(02241) 1234").dialUri)
  }

  @Test
  fun `an extension is not dialled`() {
    assertEquals("tel:+31111123456", phone("+31 111 123 456 x9").dialUri)
    assertEquals("tel:+492241123", phone("+49 2241 123 ext. 45").dialUri)
  }

  @Test
  fun `a phone without digits has nothing to dial`() {
    assertNull(phone("ask at the bar").dialUri)
  }

  @Test
  fun `details come from the plain or the contact keys`() {
    assertEquals(
      PlaceDetails("https://a.nl", "+31 1", "Mo-Su 08:00-18:00"),
      PlaceDetails.ofTags(
        mapOf(
          "website" to "https://a.nl",
          "phone" to "+31 1",
          "opening_hours" to "Mo-Su 08:00-18:00",
        )
      ),
    )
    assertEquals(
      PlaceDetails("https://b.nl", "+31 2", null),
      PlaceDetails.ofTags(mapOf("contact:website" to "https://b.nl", "contact:phone" to "+31 2")),
    )
    assertEquals(
      PlaceDetails(null, "+31 6 1", null),
      PlaceDetails.ofTags(mapOf("contact:mobile" to "+31 6 1")),
    )
    assertEquals(
      PlaceDetails("https://a.nl", null, null),
      PlaceDetails.ofTags(
        mapOf("website" to " https://a.nl ", "contact:website" to "https://c.nl")
      ),
    )
  }

  @Test
  fun `a place without details has none`() {
    assertNull(PlaceDetails.ofTags(mapOf("amenity" to "drinking_water", "phone" to " ")))
  }

  private fun hours(tagged: String) = PlaceDetails(null, null, tagged)

  /** Wednesday, 16 June 2027, at [hour]:[minute], in the networks' local time. */
  private fun wednesday(hour: Int, minute: Int = 0) =
    LocalMoment(CalendarDay(2027, 6, 16), hour * 60 + minute)

  @Test
  fun `Café open now tells until when`() {
    val status = hours("Mo-Su 08:00-18:00").openingStatusAt(wednesday(14))
    assertEquals(FerryStatus(true, "Open until 18:00"), status)
  }

  @Test
  fun `Restaurant opening later tells when it opens`() {
    assertEquals(
      "Closed until 17:00",
      hours("Tu-Su 17:00-22:00").openingStatusAt(wednesday(14))?.text,
    )
  }

  @Test
  fun `Restaurant closed on its rest day opens tomorrow`() {
    val monday = LocalMoment(CalendarDay(2027, 6, 14), 14 * 60)
    assertEquals(
      "Closed until tomorrow 17:00",
      hours("Tu-Su 17:00-22:00").openingStatusAt(monday)?.text,
    )
  }

  @Test
  fun `Lunch and dinner close in between`() {
    assertEquals(
      "Closed until 17:30",
      hours("Mo-Su 11:30-14:00,17:30-22:00").openingStatusAt(wednesday(15))?.text,
    )
  }

  @Test
  fun `Hours the app can't be sure of give no status`() {
    assertNull(hours("Mo-Fr 08:00-18:00; SH off").openingStatusAt(wednesday(10)))
  }

  // Scenario: Unreadable hours
  @Test
  fun `Unreadable hours give no status`() {
    assertNull(hours("summer only, ask inside").openingStatusAt(wednesday(10)))
  }

  @Test
  fun `Place without opening hours has no status`() {
    assertNull(PlaceDetails("https://a.nl", null, null).openingStatusAt(wednesday(10)))
  }

  @Test
  fun `Opening hours while navigating are worded in the app's language`() {
    // The card reads the same status while navigating; in German, for instance:
    val status = hours("Mo-Su 08:00-18:00").openingStatusAt(wednesday(14), Language.DE)
    assertEquals(true, status?.open)
    assertTrue(status!!.text.contains("18:00"))
  }
}
