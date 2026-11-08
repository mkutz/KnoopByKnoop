package com.knoopbyknoop.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BaseMapPlacesTest {

  @Test
  fun `the dark map draws its places in the layers the apps filter`() {
    val style =
      Json.parseToJsonElement(File("../../map-styles/liberty-dark.json").readText()).jsonObject
    val layers =
      style.getValue("layers").jsonArray.associateBy {
        it.jsonObject.getValue("id").jsonPrimitive.content
      }
    for (id in PlaceKind.BASE_MAP_LAYERS) {
      val layer = layers.getValue(id).jsonObject
      assertEquals("openmaptiles", layer.getValue("source").jsonPrimitive.content, id)
      assertEquals(PlaceKind.SOURCE_LAYER, layer.getValue("source-layer").jsonPrimitive.content, id)
    }
  }
}
