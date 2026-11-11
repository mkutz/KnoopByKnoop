package com.knoopbyknoop.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.PlaceDetails
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * The details of a tapped place below its name: whether it is open now with its opening hours, its
 * website and its phone number, see `map-planning` "Opening hours on the place card" and "Contacts
 * on the place card". What OpenStreetMap doesn't have is left out.
 */
@Composable
fun PlaceDetailsRows(details: PlaceDetails, language: Language) {
  details.openingHours?.let { OpeningRow(details, it, language) }
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current
  val website = details.displayWebsite
  val websiteUri = details.websiteUri
  if (website != null && websiteUri != null) {
    val description = stringResource(R.string.place_website_description, website)
    DetailRow(R.drawable.ic_website, website, description) {
      try {
        uriHandler.openUri(websiteUri)
      } catch (exception: IllegalArgumentException) {
        // No browser for it: nothing to open.
      }
    }
  }
  val phone = details.displayPhone
  val dialUri = details.dialUri
  if (phone != null && dialUri != null) {
    val description = stringResource(R.string.place_phone_description, phone)
    DetailRow(R.drawable.ic_phone, phone, description) {
      // The dialler with the number, which calls only when the user confirms there.
      try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(dialUri)))
      } catch (exception: ActivityNotFoundException) {
        // A device without a phone app.
      }
    }
  }
}

/** A website or phone number that opens what it names when tapped. */
@Composable
private fun DetailRow(
  @DrawableRes icon: Int,
  text: String,
  description: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .clearAndSetSemantics {
          contentDescription = description
          role = Role.Button
        },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.widthIn(max = 240.dp),
    )
  }
}

/**
 * Whether the place is open now, kept current every minute like the ferries, and its opening hours
 * as tagged below; without a sure answer only the hours.
 */
@Composable
private fun OpeningRow(details: PlaceDetails, hours: String, language: Language) {
  val now by
    produceState(ferryMoment(Instant.now())) {
      while (true) {
        delay(millisToNextMinute(Instant.now()))
        value = ferryMoment(Instant.now())
      }
    }
  val status = details.openingStatusAt(now, language)
  Row(
    modifier =
      Modifier.padding(horizontal = 12.dp, vertical = 8.dp).semantics(mergeDescendants = true) {},
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      painterResource(R.drawable.ic_opening_hours),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      if (status != null) {
        Text(
          status.text,
          style = MaterialTheme.typography.bodyMedium,
          color = if (status.open) ActionColors.iconGreen else MaterialTheme.colorScheme.error,
        )
      }
      Text(
        hours,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 240.dp),
      )
    }
  }
}
