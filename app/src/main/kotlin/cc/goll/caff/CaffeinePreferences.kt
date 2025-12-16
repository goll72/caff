package cc.goll.caff


import android.os.Bundle
import android.os.Build 

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.indication

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

import java.util.TreeSet

import cc.goll.caff.utils.HumanReadableTime


private val DarkColors = darkColorScheme()
private val LightColors = lightColorScheme()


@Composable
fun CaffeineTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LocalContext.current.let {
            if (darkTheme) dynamicDarkColorScheme(it) else dynamicLightColorScheme(it)
        }
    } else {
        if (darkTheme) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}


class CaffeinePreferences : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val durations = CaffeineDurations.get(this)
    
        enableEdgeToEdge()

        setContent {
            CaffeineTheme {
                PreferencesDialog(ObservableSet(durations))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        CaffeineDurations.save(this)
    }
}


@Stable
class ObservableSet<T>(private val inner: TreeSet<T>) {
    private var flag by mutableStateOf(0)

    fun add(element: T): Boolean {
        if (inner.add(element)) {
            flag++
            return true
        }

        return false
    }

    fun remove(element: T): Boolean {
        if (inner.remove(element)) {
            flag++
            return true
        }

        return false
    }

    fun contains(element: T): Boolean {
        flag
        return inner.contains(element)
    }
}


private const val MIN_DURATION = 5 * 60
private const val MAX_DURATION = 55 * 60

private const val DURATION_STEP = 5 * 60


@Composable
private fun Modifier.ripple(): Modifier =
    this then Modifier.indication(
        remember { MutableInteractionSource() },
        androidx.compose.material3.ripple()
    )


@Composable
fun PreferencesDialog(durations: ObservableSet<Int>) {
    var currentDuration: Int by rememberSaveable { mutableStateOf(5 * 60) }

    val durationWasAdded: Boolean by remember {
        derivedStateOf {
            durations.contains(currentDuration)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { currentDuration -= DURATION_STEP  },
                enabled = currentDuration > MIN_DURATION,
                modifier = Modifier.ripple()
            ) {
                Icon(
                    modifier = Modifier.rotate(180f),
                    painter = painterResource(R.drawable.arrow_forward_ios),
                    contentDescription = "Decrease duration",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
    
            Text(
                text = HumanReadableTime(currentDuration).toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.padding(horizontal = 24.dp)
            )
    
            IconButton(
                onClick = { currentDuration += DURATION_STEP },
                enabled = currentDuration < MAX_DURATION,
                modifier = Modifier.ripple()
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward_ios),
                    contentDescription = "Increase duration",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { durations.remove(currentDuration) },
                enabled = durationWasAdded,
                modifier = Modifier.ripple()
            ) {
                Text("Remove")
            }

            Button(
                onClick = { durations.add(currentDuration) },
                enabled = !durationWasAdded,
                modifier = Modifier.ripple()
            ) {
                Text("Add")
            }
        }
    }
}
