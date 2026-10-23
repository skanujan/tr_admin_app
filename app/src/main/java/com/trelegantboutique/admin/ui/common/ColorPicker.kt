package com.trelegantboutique.admin.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.trelegantboutique.admin.ui.theme.DeepGreen
import com.trelegantboutique.admin.ui.theme.Line
import com.trelegantboutique.admin.ui.theme.Marcellus
import com.trelegantboutique.admin.ui.theme.Muted
import com.trelegantboutique.admin.util.ColorTools
import com.trelegantboutique.admin.util.dominantColorOfUrl
import kotlinx.coroutines.launch

/** 0xRRGGBB to a Compose colour. */
fun rgbColor(rgb: Int): Color = Color(0xFF000000.toInt() or (rgb and 0xFFFFFF))

private fun hsvToRgb(h: Float, s: Float, v: Float): Int =
    android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) and 0xFFFFFF

private fun rgbToHsv(rgb: Int): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(0xFF000000.toInt() or rgb, out)
    return out
}

/** Round colour dot that opens the picker, as on the website's "Dot colour". */
@Composable
fun ColorDot(hex: String, size: Int = 36, onClick: () -> Unit) {
    val rgb = ColorTools.parseHex(hex)
    Box(
        Modifier
            .size(size.dp)
            .background(if (rgb != null) rgbColor(rgb) else Color(0xFFEEEEEE), CircleShape)
            .border(1.dp, Line, CircleShape)
            .clickable { onClick() }
    )
}

/**
 * Colour picker like the website's: a shade square, a rainbow bar, quick
 * swatches, a hex box and "From the photo" (the main colour of [photoUrl]).
 * The colour name is suggested from the same list as the website.
 * [onPick] gets the hex ("#b22222") and the suggested name.
 */
@Composable
fun ColorPickerDialog(
    initialHex: String,
    photoUrl: String?,
    onPick: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val start = ColorTools.parseHex(initialHex) ?: 0x14603A
    val hsv0 = remember { rgbToHsv(start) }
    var hue by remember { mutableStateOf(hsv0[0]) }
    var sat by remember { mutableStateOf(hsv0[1]) }
    var value by remember { mutableStateOf(hsv0[2]) }
    var hexText by remember { mutableStateOf(ColorTools.toHex(start)) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rgb = hsvToRgb(hue, sat, value)
    val name = ColorTools.name(rgb)

    fun setRgb(c: Int) {
        val h = rgbToHsv(c)
        hue = h[0]; sat = h[1]; value = h[2]
        hexText = ColorTools.toHex(c)
    }
    fun syncHex() { hexText = ColorTools.toHex(hsvToRgb(hue, sat, value)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick the colour", fontFamily = Marcellus) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(rgbColor(rgb), RoundedCornerShape(10.dp)).border(1.dp, Line, RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text(ColorTools.toHex(rgb), color = Muted)
                    }
                }

                // Shade square: left to right = paler to stronger, top to bottom = lighter to darker.
                var sqSize by remember { mutableStateOf(IntSize(1, 1)) }
                fun setSv(o: Offset) {
                    sat = (o.x / sqSize.width.toFloat()).coerceIn(0f, 1f)
                    value = 1f - (o.y / sqSize.height.toFloat()).coerceIn(0f, 1f)
                    syncHex()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .onSizeChanged { sqSize = IntSize(maxOf(1, it.width), maxOf(1, it.height)) }
                        .background(Brush.horizontalGradient(listOf(Color.White, rgbColor(hsvToRgb(hue, 1f, 1f)))), RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { setSv(it) } }
                        .pointerInput(Unit) { detectDragGestures { change, _ -> setSv(change.position) } }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(sat * size.width, (1f - value) * size.height)
                        drawCircle(Color.White, radius = 11f, center = c, style = Stroke(width = 5f))
                        drawCircle(Color.Black, radius = 14f, center = c, style = Stroke(width = 2f))
                    }
                }

                // Rainbow bar for the shade.
                var barW by remember { mutableStateOf(1) }
                fun setHue(x: Float) {
                    hue = (x / barW.toFloat()).coerceIn(0f, 1f) * 360f
                    if (sat < 0.05f) sat = 0.8f
                    if (value < 0.05f) value = 0.8f
                    syncHex()
                }
                val rainbow = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { rgbColor(hsvToRgb(it, 1f, 1f)) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .onSizeChanged { barW = maxOf(1, it.width) }
                        .background(Brush.horizontalGradient(rainbow), RoundedCornerShape(14.dp))
                        .pointerInput(Unit) { detectTapGestures { setHue(it.x) } }
                        .pointerInput(Unit) { detectDragGestures { change, _ -> setHue(change.position.x) } }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val x = hue / 360f * size.width
                        drawCircle(Color.White, radius = size.height / 2f - 2f, center = Offset(x, size.height / 2f), style = Stroke(width = 5f))
                    }
                }

                Text("Quick colours", style = MaterialTheme.typography.bodySmall)
                for (row in ColorTools.QUICK.chunked(8)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in row) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(rgbColor(c), CircleShape)
                                    .border(1.dp, Line, CircleShape)
                                    .clickable { setRgb(c) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { t ->
                        hexText = t
                        val c = ColorTools.parseHex(t)
                        if (c != null) { val h = rgbToHsv(c); hue = h[0]; sat = h[1]; value = h[2] }
                    },
                    label = { Text("Colour code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!photoUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                val c = dominantColorOfUrl(context, photoUrl)
                                busy = false
                                if (c != null) setRgb(c)
                            }
                        },
                        enabled = !busy,
                        border = BorderStroke(1.dp, DeepGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Reading the photo..." else "Pick from the photo") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(ColorTools.toHex(rgb), name) }) { Text("Use this colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
