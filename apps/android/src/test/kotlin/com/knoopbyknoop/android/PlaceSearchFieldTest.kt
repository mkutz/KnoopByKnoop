package com.knoopbyknoop.android

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Covers `place-search` "Typed text is kept". */
class PlaceSearchFieldTest {

  @Test
  fun `earlier text echoed late`() {
    val field = TextFieldValue("Blu", TextRange(3))

    assertNull(fieldAfterQuery(field, "Bl", sent = setOf("B", "Bl", "Blu")))
  }

  @Test
  fun `cleared from the app`() {
    val field = TextFieldValue("Blue", TextRange(4))

    assertEquals(
      TextFieldValue(""),
      fieldAfterQuery(field, "", sent = setOf("B", "Bl", "Blu", "Blue")),
    )
  }

  @Test
  fun `a picked hit replaces the text with the cursor at the end`() {
    val field = TextFieldValue("blue lot", TextRange(8))

    assertEquals(
      TextFieldValue("Blue Lotus", TextRange(10)),
      fieldAfterQuery(field, "Blue Lotus", sent = setOf("blue lo", "blue lot")),
    )
  }

  @Test
  fun `the same text keeps the field`() {
    val field = TextFieldValue("Blue", TextRange(2))

    assertNull(fieldAfterQuery(field, "Blue", sent = setOf()))
  }
}
