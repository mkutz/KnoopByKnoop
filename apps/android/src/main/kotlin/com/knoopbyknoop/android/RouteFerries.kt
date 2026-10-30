package com.knoopbyknoop.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoopbyknoop.core.FerryArrivalState
import com.knoopbyknoop.core.LocalMoment
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.RouteFerry
import java.time.Instant
import kotlinx.coroutines.delay

/** Marks a ferry between two knots of the route summary, see `map-planning`. */
@Composable
fun FerryBadge() {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
  ) {
    Text("⛴", fontSize = 14.sp)
  }
}

/**
 * The route's ferries, each in one line with whether it is open now, expanded on tap to its
 * operating times, fee and website, see `map-planning` "Ferries on the route"; while [navigation]
 * runs, the line tells when the rider gets to each ferry ahead instead, see `knot-navigation`
 * "Ferry arrival estimate".
 */
@Composable
fun RouteFerries(ferries: List<RouteFerry>, navigation: Navigation? = null) {
  // Updated at every full minute while shown, see "Ferry open now".
  val now by
    produceState(ferryMoment(Instant.now())) {
      while (true) {
        delay(millisToNextMinute(Instant.now()))
        value = ferryMoment(Instant.now())
      }
    }
  // The route positions of the expanded ferries; a new route starts collapsed.
  var expanded by remember(ferries) { mutableStateOf(setOf<Int>()) }
  Column(modifier = Modifier.padding(top = 4.dp)) {
    for (ferry in ferries) {
      val position = ferry.fromPosition
      FerryRow(
        ferry,
        status(ferry, now, navigation),
        expanded = position in expanded,
        onToggle = {
          expanded = if (position in expanded) expanded - position else expanded + position
        },
      )
    }
  }
}

/**
 * What a ferry's line tells after its name, and in which color.
 *
 * @property isStatus whether it is a status or arrival, not the tagged times or their absence
 */
private data class FerryLine(val text: String, val color: Color, val isStatus: Boolean = true)

/**
 * While navigating, the arrival takes the place of the status for now, see `knot-navigation` "Ferry
 * arrival estimate"; without a status, the tagged times, see `map-planning` "Ferry open now".
 */
@Composable
private fun status(ferry: RouteFerry, now: LocalMoment, navigation: Navigation?): FerryLine {
  val arrival = navigation?.ferryArrival(ferry.fromPosition)
  if (arrival != null) {
    return FerryLine(
      arrival.text,
      when (arrival.state) {
        FerryArrivalState.OPEN -> ActionColors.iconGreen
        FerryArrivalState.WARNING -> MaterialTheme.colorScheme.error
        FerryArrivalState.UNKNOWN -> MaterialTheme.colorScheme.onSurface
      },
    )
  }
  val status = ferry.statusAt(now)
  return when {
    status != null ->
      FerryLine(
        status.text,
        if (status.open) ActionColors.iconGreen else MaterialTheme.colorScheme.error,
      )
    ferry.timesKnown ->
      FerryLine(ferry.times, MaterialTheme.colorScheme.onSurfaceVariant, isStatus = false)
    else ->
      FerryLine("Operating times not known", MaterialTheme.colorScheme.error, isStatus = false)
  }
}

@Composable
private fun FerryRow(ferry: RouteFerry, line: FerryLine, expanded: Boolean, onToggle: () -> Unit) {
  val uriHandler = LocalUriHandler.current
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .clickable(role = Role.Button, onClick = onToggle)
          .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        // The name gives way to the status, which answers the usual question, see "Ferry line too
        // long".
        Text(
          "⛴ ${ferry.title}",
          style = MaterialTheme.typography.bodyMedium,
          maxLines = if (expanded) Int.MAX_VALUE else 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (!expanded) {
          Text(
            " · ${line.text}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = line.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
          )
        }
      }
      Icon(
        painterResource(R.drawable.ic_expand),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp).size(20.dp).rotate(if (expanded) 180f else 0f),
      )
    }
    if (expanded) {
      Column(modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)) {
        if (line.isStatus) {
          Text(
            line.text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = line.color,
          )
        }
        Text(
          ferry.times,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (ferry.timesKnown) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
        )
        ferry.fee?.let {
          Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          "Times according to OpenStreetMap",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ferry.ferry.website?.let { website ->
          TextButton(
            onClick = { uriHandler.openUri(if ("://" in website) website else "https://$website") }
          ) {
            Text("Website")
          }
        }
      }
    }
  }
}
