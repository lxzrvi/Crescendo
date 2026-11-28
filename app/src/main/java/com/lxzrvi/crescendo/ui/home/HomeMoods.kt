package com.lxzrvi.crescendo.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.data.Song
import kotlin.math.PI
import kotlin.math.sin

private data class Mood(val name: String, val color: Color, val symbol: String)
private val defaultMoods = listOf(
    Mood("Chill", Color(0xFF527B9C), "☁"),
    Mood("Focus", Color(0xFF557D79), "✦"),
    Mood("Happy", Color(0xFFD28A48), "☀"),
    Mood("Energy", Color(0xFFB55462), "⚡"),
    Mood("Night", Color(0xFF665B94), "☾"),
)
private val extraMoodColors = listOf(Color(0xFF937065), Color(0xFF527F80), Color(0xFF8D6586), Color(0xFF6E8059))
private fun moodKey(name: String) = "songs_" + name.trim().lowercase()

/** User-curated moods: collections and their song memberships persist on this device. */
@Composable
internal fun HomeMoods(songs: List<Song>, onSongClick: (Song) -> Unit, animate: Boolean, listState: LazyListState) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("home_moods", android.content.Context.MODE_PRIVATE) }
    var custom by remember(prefs) { mutableStateOf(prefs.getStringSet("names", emptySet()).orEmpty().sorted()) }
    var version by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<Mood?>(null) }
    var editing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val moods = remember(custom) {
        defaultMoods + custom.mapIndexed { index, name -> Mood(name, extraMoodColors[index % extraMoodColors.size], "♪") }
    }
    val counts = remember(moods, version, songs) {
        val valid = songs.map { it.historyKey }.toSet()
        moods.associate { it.name to prefs.getStringSet(moodKey(it.name), emptySet()).orEmpty().count { id -> id in valid } }
    }
    val phase: State<Float> = if (animate) {
        val transition = rememberInfiniteTransition(label = "moodMotion")
        transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "moodFloat")
    } else remember { mutableFloatStateOf(0f) }

    LazyRow(state = listState, contentPadding = PaddingValues(horizontal = ContentSidePadding),
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(moods, key = { it.name.lowercase() }) { mood ->
            MoodTile(mood, counts[mood.name] ?: 0, phase) {
                selected = prefs.getStringSet(moodKey(mood.name), emptySet()).orEmpty().toSet()
                current = mood
                editing = false
            }
        }
        item(key = "add-mood") {
            Box(Modifier.width(118.dp).height(115.dp).clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                .clickable(role = Role.Button) { draft = ""; adding = true }
                .semantics { contentDescription = "Add a mood" }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+", color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp, lineHeight = 34.sp)
                    Text("Add mood", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }

    if (adding) {
        val name = draft.trim().take(28)
        val valid = name.isNotEmpty() && moods.none { it.name.equals(name, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Create a mood") },
            text = { OutlinedTextField(value = draft, onValueChange = { draft = it.take(28) }, singleLine = true,
                label = { Text("Mood name") }) },
            confirmButton = { TextButton(enabled = valid, onClick = {
                custom = (custom + name).sorted()
                prefs.edit().putStringSet("names", custom.toSet()).apply()
                adding = false
            }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
    current?.let { mood ->
        val shown = if (editing) songs else songs.filter { it.historyKey in selected }
        AlertDialog(
            onDismissRequest = { current = null },
            title = { Text(if (editing) "Choose songs · ${mood.name}" else mood.name) },
            text = {
                Column {
                    Text(if (editing) "Tap songs to add or remove them." else "Your songs for this mood.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    if (shown.isEmpty()) {
                        Text(if (songs.isEmpty()) "No music found on this device." else "No songs yet. Tap Edit songs to add some.",
                            fontSize = 13.sp, lineHeight = 18.sp)
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                            items(shown, key = { it.id }) { song ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) {
                                        if (editing) {
                                            val updated = selected.toMutableSet()
                                            if (!updated.add(song.historyKey)) updated.remove(song.historyKey)
                                            selected = updated.toSet()
                                            prefs.edit().putStringSet(moodKey(mood.name), selected).apply()
                                            version++
                                        } else {
                                            onSongClick(song)
                                            current = null
                                        }
                                    }.padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 12.sp)
                                    }
                                    if (editing) Text(if (song.historyKey in selected) "✓" else "+",
                                        color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (editing) editing = false else current = null }) {
                Text(if (editing) "Done" else "Close")
            } },
            dismissButton = { if (!editing && songs.isNotEmpty()) TextButton(onClick = { editing = true }) { Text("Edit songs") } },
        )
    }
}

@Composable
private fun MoodTile(mood: Mood, count: Int, phase: State<Float>, onClick: () -> Unit) {
    val darkColor = lerp(mood.color, Color(0xFF16121D), 0.56f)
    Box(Modifier.width(142.dp).height(115.dp).clip(RoundedCornerShape(19.dp))
        .background(Brush.linearGradient(listOf(mood.color, darkColor)))
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "${mood.name} mood, $count songs" }) {
        Canvas(Modifier.fillMaxSize()) {
            val frame = phase.value
            repeat(5) { i ->
                val t = frame * 2f * PI.toFloat() + i * 1.3f
                val x = size.width * (0.46f + i * 0.13f)
                val y = size.height * (0.16f + i * 0.075f) + sin(t) * size.height * 0.06f
                drawCircle(Color.White.copy(alpha = 0.07f + i * 0.012f), radius = size.minDimension * (0.14f + i * 0.022f), center = Offset(x, y))
            }
            if (mood.name == "Energy" || mood.name == "Focus") repeat(5) { i ->
                val x = size.width * (0.55f + i * 0.075f)
                val height = size.height * (0.12f + (i % 3) * 0.06f + frame * 0.05f)
                drawLine(Color.White.copy(alpha = 0.23f), Offset(x, size.height * 0.48f - height),
                    Offset(x, size.height * 0.48f + height), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Text(mood.symbol, color = Color.White.copy(alpha = 0.45f), fontSize = 40.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 5.dp)
                .graphicsLayer { translationY = phase.value * 5.dp.toPx(); rotationZ = (phase.value - 0.5f) * 8f })
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(mood.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (count == 1) "1 song" else "$count songs", color = Color.White.copy(alpha = 0.74f), fontSize = 11.sp)
        }
    }
}
