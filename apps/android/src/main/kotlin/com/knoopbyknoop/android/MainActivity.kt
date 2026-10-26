package com.knoopbyknoop.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkType

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val nextKnot = Knot(id = 0, ref = "12", network = NetworkType.CYCLING, 52.09, 5.12)
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Next knot", style = MaterialTheme.typography.titleMedium)
            Text(nextKnot.ref, style = MaterialTheme.typography.displayLarge)
          }
        }
      }
    }
  }
}
