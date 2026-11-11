package com.knoopbyknoop.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BaseMapPlacesTest {

  /** A feature of a vector tile's `poi` layer: its id and the properties the app reads. */
  private data class TilePlace(
    val id: Long,
    val className: String?,
    val subclass: String?,
    val name: String?,
  )

  /**
   * The `poi` features of a stored base map tile, read with just enough of the vector tile format.
   */
  private fun tilePlaces(name: String): List<TilePlace> {
    val bytes = File("src/jvmTest/resources/base-map-tiles/$name.pbf").readBytes()
    for (layer in fields(bytes).filter { it.first == 3 }.map { it.second as ByteArray }) {
      val layerFields = fields(layer)
      val layerName = (layerFields.first { it.first == 1 }.second as ByteArray).decodeToString()
      if (layerName != PlaceKind.SOURCE_LAYER) continue
      val keys =
        layerFields.filter { it.first == 3 }.map { (it.second as ByteArray).decodeToString() }
      val values =
        layerFields
          .filter { it.first == 4 }
          .map { value ->
            fields(value.second as ByteArray).first().let {
              (it.second as? ByteArray)?.decodeToString()
            }
          }
      return layerFields
        .filter { it.first == 2 }
        .map { feature ->
          val featureFields = fields(feature.second as ByteArray)
          val id = featureFields.firstOrNull { it.first == 1 }?.second as Long? ?: 0
          val tags =
            featureFields.firstOrNull { it.first == 2 }?.second as ByteArray? ?: ByteArray(0)
          val numbers = ArrayList<Long>()
          var position = 0
          while (position < tags.size) {
            val (number, next) = varint(tags, position)
            numbers.add(number)
            position = next
          }
          val properties =
            numbers.chunked(2).associate { (key, value) ->
              keys[key.toInt()] to values[value.toInt()]
            }
          TilePlace(id, properties["class"], properties["subclass"], properties["name"])
        }
    }
    error("No ${PlaceKind.SOURCE_LAYER} layer in $name")
  }

  private fun varint(bytes: ByteArray, start: Int): Pair<Long, Int> {
    var result = 0L
    var shift = 0
    var position = start
    while (true) {
      val byte = bytes[position++].toInt() and 0xFF
      result = result or ((byte and 0x7F).toLong() shl shift)
      shift += 7
      if (byte < 0x80) return result to position
    }
  }

  /** The fields of a protocol buffer message: numbers for varints, bytes for everything else. */
  private fun fields(bytes: ByteArray): List<Pair<Int, Any>> {
    val result = ArrayList<Pair<Int, Any>>()
    var position = 0
    while (position < bytes.size) {
      val (key, afterKey) = varint(bytes, position)
      position = afterKey
      val field = (key ushr 3).toInt()
      when ((key and 7).toInt()) {
        0 ->
          varint(bytes, position).let { (value, next) ->
            result.add(field to value)
            position = next
          }
        2 ->
          varint(bytes, position).let { (size, next) ->
            result.add(field to bytes.copyOfRange(next, next + size.toInt()))
            position = next + size.toInt()
          }
        5 -> {
          result.add(field to bytes.copyOfRange(position, position + 4))
          position += 4
        }
        1 -> {
          result.add(field to bytes.copyOfRange(position, position + 8))
          position += 8
        }
        else -> error("Unexpected wire type in $key")
      }
    }
    return result
  }

  // Scenario: Place drawn from a building
  @Test
  fun `the tiles' feature ids are the OpenStreetMap nodes and ways the place details are keyed by`() {
    val byName = tilePlaces("renesse-14-8363-5431").associateBy { it.name }
    // Looked up on the OpenStreetMap API on 2026-11-10: the hotel and the restaurant are nodes, the
    // minicamping is mapped as an area (a way).
    assertEquals(
      PlaceFile.nodeFeatureId(2807330910),
      byName.getValue("Fletcher Strandhotel Renesse").id,
    )
    assertEquals(PlaceFile.nodeFeatureId(2807324298), byName.getValue("Het Wapen van Zeeland").id)
    assertEquals(PlaceFile.wayFeatureId(1333572075), byName.getValue("Minicamping Manege Grol").id)
  }

  // Scenario: Campsites near Renesse
  @Test
  fun `the tiles near Renesse have campsites, hotels and guest houses of our kinds`() {
    val kinds =
      tilePlaces("renesse-14-8363-5431").groupBy { PlaceKind.of(it.className, it.subclass) }
    val campsites = kinds[PlaceKind.CAMPSITE].orEmpty().map { it.name }
    val lodging = kinds[PlaceKind.LODGING].orEmpty().map { it.name }
    assertTrue("Minicamping Manege Grol" in campsites, "$campsites")
    assertTrue("Fletcher Strandhotel Renesse" in lodging, "$lodging")
    assertTrue("Maison Bellefleur" in lodging, "$lodging")
    // Holiday homes stay in the base map's style.
    assertFalse(kinds[null].orEmpty().none { it.subclass == "chalet" })
  }

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
