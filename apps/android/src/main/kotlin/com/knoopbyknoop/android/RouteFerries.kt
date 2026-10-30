package com.knoopbyknoop.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoopbyknoop.core.FerryArrivalState
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
 * The route's ferries with whether they are open now, their operating times, fee and website, see
 * `map-planning`; while [navigation] runs, also when the rider gets to each ferry ahead, see
 * `knot-navigation` "Ferry arrival estimate".
 */
@Composable
fun RouteFerries(ferries: List<RouteFerry>, navigation: Navigation? = null) {
  val uriHandler = LocalUriHandler.current
  // Updated at every full minute while shown, see "Ferry open now".
  val now by
    produceState(ferryMoment(Instant.now())) {
      while (true) {
        delay(millisToNextMinute(Instant.now()))
        value = ferryMoment(Instant.now())
      }
    }
  Column(modifier = Modifier.padding(top = 8.dp)) {
    Text("Ferries on this route", style = MaterialTheme.typography.titleSmall)
    for (ferry in ferries) {
      Column(modifier = Modifier.padding(top = 6.dp)) {
        Text("⛴ ${ferry.title}", style = MaterialTheme.typography.bodyMedium)
        // While navigating, the arrival line takes the place of the status for now, see
        // `knot-navigation` "Ferry arrival estimate".
        val arrival = navigation?.ferryArrival(ferry.fromPosition)
        if (arrival != null) {
          Text(
            arrival.text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color =
              when (arrival.state) {
                FerryArrivalState.OPEN -> ActionColors.iconGreen
                FerryArrivalState.WARNING -> MaterialTheme.colorScheme.error
                FerryArrivalState.UNKNOWN -> MaterialTheme.colorScheme.onSurface
              },
          )
        } else
          ferry.statusAt(now)?.let { status ->
            Text(
              status.text,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = if (status.open) ActionColors.iconGreen else MaterialTheme.colorScheme.error,
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
