package com.knoopbyknoop.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.PlaceHit

/**
 * The search field over the map and the hits below it, see `place-search`. The hits are shown right
 * under the field, so the keyboard does not hide them.
 */
@Composable
fun PlaceSearchField(state: AppState, viewModel: AppViewModel) {
  val focusManager = LocalFocusManager.current
  Column(modifier = Modifier.widthIn(max = 480.dp)) {
    TextField(
      value = if (state.online) state.searchQuery else "",
      onValueChange = viewModel::search,
      enabled = state.online,
      singleLine = true,
      placeholder = {
        Text(if (state.online) "Search places" else "Search needs an internet connection")
      },
      trailingIcon = {
        if (state.searchQuery.isNotEmpty() && state.online) {
          TextButton(
            onClick = {
              viewModel.clearSearch()
              focusManager.clearFocus()
            }
          ) {
            Text("✕")
          }
        }
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      shape = MaterialTheme.shapes.medium,
      colors =
        TextFieldDefaults.colors(
          focusedContainerColor = mapOverlayColor,
          unfocusedContainerColor = mapOverlayColor,
          disabledContainerColor = mapOverlayColor.copy(alpha = 0.85f),
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent,
        ),
      modifier = Modifier.fillMaxWidth(),
    )
    val result = state.searchResult
    if (result != SearchResult.None && state.online) {
      Card(
        colors = CardDefaults.cardColors(containerColor = mapOverlayColor),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      ) {
        when (result) {
          SearchResult.Searching -> Message("Searching…")
          SearchResult.NoHits -> Message("Nothing found")
          SearchResult.Unavailable -> Message("Search is not available right now")
          is SearchResult.Hits ->
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
              items(result.hits) { hit ->
                Hit(
                  hit,
                  onClick = {
                    viewModel.choose(hit)
                    focusManager.clearFocus()
                  },
                )
                HorizontalDivider()
              }
            }
          SearchResult.None -> {}
        }
      }
    }
  }
}

@Composable
private fun Hit(hit: PlaceHit, onClick: () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Text(hit.name, style = MaterialTheme.typography.bodyLarge)
    if (hit.description.isNotEmpty()) {
      Text(
        hit.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun Message(text: String) {
  Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
}
