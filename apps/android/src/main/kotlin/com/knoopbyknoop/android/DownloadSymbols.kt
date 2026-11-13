package com.knoopbyknoop.android

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.knoopbyknoop.core.PathDataStatus
import kotlinx.coroutines.launch

/**
 * The map and, on cycling routes, the ways as symbols with their status, at the end of the action
 * row, see `network-sync` "Base map ready for the ride" and "Path data ready for the ride".
 */
@Composable
fun DownloadSymbols(baseMap: PathDataStatus?, pathData: PathDataStatus?) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    baseMap?.let {
      DownloadSymbol(
        R.drawable.ic_route_map,
        it,
        stringResource(
          when (it) {
            PathDataStatus.LOADING -> R.string.base_map_loading
            PathDataStatus.READY -> R.string.base_map_ready
            PathDataStatus.UNAVAILABLE -> R.string.base_map_unavailable
          }
        ),
      )
    }
    pathData?.let {
      DownloadSymbol(
        R.drawable.ic_route_ways,
        it,
        stringResource(
          when (it) {
            PathDataStatus.LOADING -> R.string.path_data_loading
            PathDataStatus.READY -> R.string.path_data_ready
            PathDataStatus.UNAVAILABLE -> R.string.path_data_unavailable
          }
        ),
      )
    }
  }
}

/**
 * One symbol with a mark at its corner: a spinner while loading, a checkmark when loaded, a warning
 * when it couldn't be loaded. Read as [sentence], again when it changes; a tap shows it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSymbol(@DrawableRes icon: Int, status: PathDataStatus, sentence: String) {
  val tooltip = rememberTooltipState()
  val scope = rememberCoroutineScope()
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(sentence) } },
    state = tooltip,
  ) {
    Box(
      modifier =
        Modifier.size(32.dp)
          .clickable { scope.launch { tooltip.show() } }
          .clearAndSetSemantics {
            contentDescription = sentence
            liveRegion = LiveRegionMode.Polite
          }
    ) {
      Icon(
        painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp).align(Alignment.Center),
      )
      // Ringed in the panel's colour, so it stands apart from the symbol. Without a fade where
      // animations are removed, as Compose follows the system's animation scale.
      Crossfade(
        status,
        label = "download status",
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .offset(x = 1.dp, y = 1.dp)
            .size(15.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
            .padding(1.dp),
      ) { shown ->
        Box(contentAlignment = Alignment.Center) {
          when (shown) {
            PathDataStatus.LOADING ->
              CircularProgressIndicator(
                color = ActionColors.iconGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(10.dp),
              )
            PathDataStatus.READY -> Mark(R.drawable.ic_status_ready, ActionColors.iconGreen)
            PathDataStatus.UNAVAILABLE -> Mark(R.drawable.ic_status_failed, ActionColors.warning)
          }
        }
      }
    }
  }
}

@Composable
private fun Mark(@DrawableRes icon: Int, tint: Color) {
  Icon(
    painterResource(icon),
    contentDescription = null,
    tint = tint,
    modifier = Modifier.size(13.dp),
  )
}
