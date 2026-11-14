package com.knoopbyknoop.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.PlaceHit

/**
 * The place search in the waypoint sheet or the side panel, opened by the map's magnifier button:
 * the field with Cancel at the top and the hits below it, on the panel's surface, see
 * `place-search` "Search while typing".
 *
 * Picking a hit or Cancel calls [onClose]; Cancel also clears the search and its marker, the ✕ only
 * empties the field to type again.
 */
@Composable
fun PlaceSearchField(
  state: AppState,
  viewModel: AppViewModel,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focus = remember { FocusRequester() }
  // With the cursor at the end, so typing on after reopening continues the text left from before.
  var field by remember {
    mutableStateOf(TextFieldValue(state.searchQuery, TextRange(state.searchQuery.length)))
  }
  // The texts this field stored as the query since the last change from outside: they come back
  // as the query a frame late and must not overwrite what was typed since, see `place-search`
  // "Typed text is kept".
  val sent = remember { mutableSetOf<String>() }
  LaunchedEffect(state.searchQuery) {
    fieldAfterQuery(field, state.searchQuery, sent)?.let {
      field = it
      sent.clear()
    }
  }
  // The keyboard comes up with the field, as the user asked to search; text left from before is
  // searched again, so its hits show.
  LaunchedEffect(Unit) {
    if (state.online) focus.requestFocus()
    if (state.searchQuery.isNotEmpty()) viewModel.search(state.searchQuery)
  }
  Column(modifier = modifier.imePadding()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
    ) {
      TextField(
        value = if (state.online) field else TextFieldValue(""),
        onValueChange = {
          field = it
          if (it.text != state.searchQuery) {
            sent += it.text
            viewModel.search(it.text)
          }
        },
        enabled = state.online,
        singleLine = true,
        placeholder = {
          Text(
            stringResource(
              if (state.online) R.string.search_places else R.string.search_needs_internet
            )
          )
        },
        trailingIcon = {
          if (state.searchQuery.isNotEmpty() && state.online) {
            TextButton(
              onClick = {
                field = TextFieldValue("")
                sent.clear()
                viewModel.clearSearch()
              }
            ) {
              val clearSearch = stringResource(R.string.clear_search)
              Text(
                "✕",
                modifier = Modifier.clearAndSetSemantics { contentDescription = clearSearch },
              )
            }
          }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { viewModel.search(state.searchQuery) }),
        shape = MaterialTheme.shapes.medium,
        colors =
          TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
          ),
        modifier = Modifier.weight(1f).focusRequester(focus),
      )
      TextButton(
        onClick = {
          viewModel.clearSearch()
          onClose()
        }
      ) {
        Text(stringResource(R.string.cancel))
      }
    }
    val result = state.searchResult
    if (state.online) {
      when (result) {
        SearchResult.Searching -> Message(stringResource(R.string.searching))
        SearchResult.NoHits -> Message(stringResource(R.string.nothing_found))
        SearchResult.Unavailable -> Message(stringResource(R.string.search_unavailable))
        is SearchResult.Hits ->
          LazyColumn {
            items(result.hits) { hit ->
              Hit(
                hit,
                onClick = {
                  viewModel.choose(hit)
                  onClose()
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

/**
 * The field's value after the stored query became [query], or null to keep [field]: a query the
 * field [sent] itself is its own typing coming back late and is ignored, any other replaces the
 * text, with the cursor at the end, see `place-search` "Typed text is kept".
 */
internal fun fieldAfterQuery(
  field: TextFieldValue,
  query: String,
  sent: Set<String>,
): TextFieldValue? =
  if (query == field.text || query in sent) null else TextFieldValue(query, TextRange(query.length))
