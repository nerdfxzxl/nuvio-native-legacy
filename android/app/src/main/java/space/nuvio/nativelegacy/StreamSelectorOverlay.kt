package space.nuvio.nativelegacy

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

private data class SelectorStream(
    val index: Int,
    val addonOrder: Int,
    val addonKey: String,
    val name: String,
    val provider: String,
    val description: String,
    val filename: String,
    val infoHash: String,
    val height: Int,
    val dolbyVision: Boolean,
    val dolbyAtmos: Boolean,
    val av1: Boolean,
    val mp4: Boolean,
    val hasUrl: Boolean,
    val sizeBytes: Long,
    val sizeMb: Long,
    val badgeMask: Long,
    val audioLanguages: String,
    val subtitleLanguages: String
) {
    fun searchText(): String = buildString {
        append(name).append(' ')
        append(provider).append(' ')
        append(addonKey).append(' ')
        append(description).append(' ')
        append(filename).append(' ')
        append(infoHash).append(' ')
        append(audioLanguages).append(' ')
        append(subtitleLanguages).append(' ')
        if (height > 0) append(height).append(' ').append(height).append('p').append(' ')
        if (height >= 2160) append("4K UHD ")
        if (dolbyVision) append("Dolby Vision DolbyVision DV ")
        if (dolbyAtmos) append("Dolby Atmos DolbyAtmos Atmos ")
        if (av1) append("AV1 ")
        if (mp4) append("MP4 ")
        if (sizeBytes > 0L) append(formatStreamSize(sizeBytes))
        badgeLabels(badgeMask).forEach { append(it).append(' ') }
    }.lowercase(Locale.ROOT)
}

private val selectorBadgeDefinitions = listOf(
    0 to "4K", 1 to "1080p", 2 to "720p", 3 to "SD",
    4 to "REMUX", 5 to "BluRay", 6 to "WEB-DL", 7 to "WEBRip", 8 to "SeaDex",
    9 to "Dolby Vision", 10 to "HDR10+", 11 to "HDR10", 12 to "HDR", 13 to "HLG",
    14 to "IMAX Enhanced", 15 to "IMAX", 16 to "SDR",
    17 to "Atmos DV", 18 to "Atmos", 19 to "TrueHD DV", 20 to "TrueHD",
    21 to "DTS:X", 22 to "DTS-HD MA", 23 to "DTS-HD", 24 to "DTS",
    25 to "Dolby Digital DV", 26 to "DD+", 27 to "Dolby Digital",
    28 to "7.1", 29 to "5.1", 30 to "x265", 31 to "x264", 32 to "AV1",
    33 to "Netflix", 34 to "Prime Video", 35 to "Apple TV+", 36 to "Disney+",
    37 to "Max", 38 to "Hulu", 39 to "Peacock", 40 to "Paramount+",
    41 to "Crave", 42 to "Crunchyroll"
)

private fun badgeLabels(mask: Long): List<String> =
    selectorBadgeDefinitions.mapNotNull { (bit, label) ->
        if ((mask and (1L shl bit)) != 0L) label else null
    }

private data class ProviderFilter(val key: String, val label: String, val status: Int = 0)

private data class SelectorSnapshot(
    val generation: Int = 0,
    val revision: Int = -1,
    val title: String = "",
    val backdrop: String = "",
    val poster: String = "",
    val logo: String = "",
    val genres: String = "",
    val meta: String = "",
    val isEpisode: Boolean = false,
    val season: Int = 0,
    val episode: Int = 0,
    val episodeName: String = "",
    val providers: List<ProviderFilter> = emptyList(),
    val streams: List<SelectorStream> = emptyList()
)

private fun parseSelectorSnapshot(raw: String): SelectorSnapshot = runCatching {
    val root = JSONObject(raw)
    val array = root.optJSONArray("streams")
    val streams = if (array == null) emptyList() else buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(
                SelectorStream(
                    index = item.optInt("index", -1),
                    addonOrder = item.optInt("addonOrder", 0),
                    addonKey = item.optString("addonKey", ""),
                    name = item.optString("name", ""),
                    provider = item.optString("provider", ""),
                    description = item.optString("description", ""),
                    filename = item.optString("filename", ""),
                    infoHash = item.optString("infoHash", ""),
                    height = item.optInt("height", 0),
                    dolbyVision = item.optInt("dolbyVision", 0) != 0,
                    dolbyAtmos = item.optInt("dolbyAtmos", 0) != 0,
                    av1 = item.optInt("av1", 0) != 0,
                    mp4 = item.optInt("mp4", 0) != 0,
                    hasUrl = item.optBoolean("hasUrl", false),
                    sizeBytes = item.optLong("sizeBytes", 0L),
                    sizeMb = item.optLong("sizeMB", 0L),
                    badgeMask = item.optLong("badges", 0L),
                    audioLanguages = item.optString("audioLanguages", ""),
                    subtitleLanguages = item.optString("subtitleLanguages", "")
                )
            )
        }
    }
    val providerArray = root.optJSONArray("providers")
    val providers = if (providerArray == null) emptyList() else buildList {
        for (i in 0 until providerArray.length()) {
            val item = providerArray.optJSONObject(i) ?: continue
            add(
                ProviderFilter(
                    key = item.optString("key", ""),
                    label = item.optString("name", ""),
                    status = item.optInt("status", 0)
                )
            )
        }
    }
    SelectorSnapshot(
        generation = root.optInt("generation", 0),
        revision = root.optInt("revision", -1),
        title = root.optString("title", ""),
        backdrop = root.optString("backdrop", ""),
        poster = root.optString("poster", ""),
        logo = root.optString("logo", ""),
        genres = root.optString("genres", ""),
        meta = root.optString("meta", ""),
        isEpisode = root.optBoolean("isEpisode", false),
        season = root.optInt("season", 0),
        episode = root.optInt("episode", 0),
        episodeName = root.optString("episodeName", ""),
        providers = providers,
        streams = streams.sortedWith(compareBy<SelectorStream> { it.addonOrder }.thenBy { it.index })
    )
}.getOrDefault(SelectorSnapshot())

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun StreamSelectorOverlay(
    title: String,
    activity: NuvioActivity,
    onDismiss: () -> Unit
) {
    NuvioSelectorTheme {
        StreamSelectorContent(title, activity, onDismiss)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StreamSelectorContent(
    title: String,
    activity: NuvioActivity,
    onDismiss: () -> Unit
) {
    var snapshot by remember { mutableStateOf(SelectorSnapshot()) }
    var query by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember { mutableStateOf(false) }
    var userInteracted by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    val firstStreamFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    // Poll only while the selector is visible. Native list revisions change as
    // asynchronous add-on responses arrive; indices remain native-owned.
    LaunchedEffect(Unit) {
        var lastSnapshotVersion = Long.MIN_VALUE
        while (isActive) {
            val currentVersion = withContext(Dispatchers.IO) { activity.streamSnapshotRevision() }
            if (currentVersion != lastSnapshotVersion) {
                val next = withContext(Dispatchers.IO) {
                    runCatching { parseSelectorSnapshot(activity.snapshotStreams()) }
                        .getOrDefault(SelectorSnapshot())
                }
                snapshot = next
                lastSnapshotVersion =
                    ((next.generation.toLong() and 0xffffffffL) shl 32) or
                        (next.revision.toLong() and 0xffffffffL)
            }
            delay(300)
        }
    }

    val providers = snapshot.providers
    val visibleStreams = remember(snapshot, query, selectedProvider) {
        val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        snapshot.streams.filter { stream ->
            val searchable = stream.searchText()
            (selectedProvider == null || stream.addonKey.ifBlank { stream.provider } == selectedProvider) &&
                terms.all { searchable.contains(it) }
        }
    }

    LaunchedEffect(visibleStreams.firstOrNull()?.index) {
        if (!initialFocusRequested && !userInteracted && visibleStreams.isNotEmpty()) {
            initialFocusRequested = true
            runCatching { firstStreamFocus.requestFocus() }
        }
    }

    val displayTitle = snapshot.title.ifBlank { title }
    val backdrop = snapshot.backdrop.ifBlank { snapshot.poster }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back) {
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) onDismiss()
                    true
                } else false
            }
    ) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.5f },
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF0D0D0D),
                            0.15f to Color(0xFF0D0D0D).copy(alpha = 0.85f),
                            0.30f to Color(0xFF0D0D0D).copy(alpha = 0.40f),
                            0.50f to Color(0xFF0D0D0D).copy(alpha = 0.15f),
                            0.70f to Color(0xFF0D0D0D).copy(alpha = 0.40f),
                            0.85f to Color(0xFF0D0D0D).copy(alpha = 0.85f),
                            1.0f to Color(0xFF0D0D0D)
                        )
                    )
                )
        )
        Row(modifier = Modifier.fillMaxSize()) {
            // Official selector composition: title/content column on the left,
            // translucent source panel on the right.
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(start = 48.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (snapshot.logo.isNotBlank()) {
                    AsyncImage(
                        model = snapshot.logo,
                        contentDescription = displayTitle,
                        modifier = Modifier.fillMaxWidth(0.8f).height(120.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = displayTitle.ifBlank { "Choose a source" },
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFFF4F4F6),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (snapshot.isEpisode && snapshot.season > 0 && snapshot.episode > 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Season " + snapshot.season + " · Episode " + snapshot.episode,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFB4B5BF)
                    )
                    if (snapshot.episodeName.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = snapshot.episodeName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF0F0F3),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val info = listOf(snapshot.genres, snapshot.meta)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (info.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFB4B5BF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(top = 48.dp, end = 48.dp, bottom = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF222222).copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                // Provider chips stay independent of search. Clearing the query
                // restores every source in the selected provider tab.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProviderChip(
                        label = "↻",
                        selected = false,
                        onClick = { activity.refreshStreams() },
                        onFocus = { userInteracted = true }
                    )
                    ProviderChip(
                        label = "All",
                        selected = selectedProvider == null,
                        onClick = { selectedProvider = null },
                        onFocus = { userInteracted = true }
                    )
                    providers.forEach { provider ->
                        ProviderChip(
                            label = provider.label,
                            status = provider.status,
                            selected = selectedProvider == provider.key,
                            onClick = { selectedProvider = provider.key },
                            onFocus = { userInteracted = true }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF222222))
                        .border(BorderStroke(if (searchFocused) 2.dp else 1.dp, if (searchFocused) Color.White else Color(0xFF333333)), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", color = Color(0xFFB3B3B3), fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search streams…",
                                color = Color(0xFF808080),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFFF4F4F6),
                                fontSize = 17.sp
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    searchFocused = it.isFocused
                                    if (it.isFocused) userInteracted = true
                                }
                        )
                    }
                    if (query.isNotEmpty()) {
                        Text(
                            text = "×",
                            color = Color(0xFFE0E1E7),
                            fontSize = 25.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { query = "" }
                                .onFocusChanged { if (it.isFocused) userInteracted = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when {
                    visibleStreams.isEmpty() && providers.firstOrNull { it.key == selectedProvider }?.status == 1 -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Waiting for " + (providers.firstOrNull { it.key == selectedProvider }?.label ?: "sources") + "…",
                                color = Color(0xFFB3B3B3),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    visibleStreams.isEmpty() && providers.firstOrNull { it.key == selectedProvider }?.status == 2 -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                (providers.firstOrNull { it.key == selectedProvider }?.label ?: "Source") + " did not respond. Press refresh to retry.",
                                color = Color(0xFFB3B3B3),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    snapshot.streams.isEmpty() && providers.isNotEmpty() && providers.all { it.status == 2 } -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No sources returned. Press refresh to retry.",
                                color = Color(0xFFB3B3B3),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    snapshot.streams.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Waiting for sources…",
                                color = Color(0xFFB3B3B3),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    visibleStreams.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No streams match your search",
                                color = Color(0xFFB3B3B3),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                visibleStreams,
                                key = { _, stream -> stream.index }
                            ) { index, stream ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (index == 0 || visibleStreams[index - 1].addonKey != stream.addonKey || visibleStreams[index - 1].provider != stream.provider) {
                                        Text(
                                            text = stream.provider,
                                            modifier = Modifier.padding(start = 16.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB3B3B3)
                                        )
                                    }
                                    StreamSourceCard(
                                        stream = stream,
                                        focusRequester = if (index == 0) firstStreamFocus else null,
                                        onClick = {
                                            val accepted = activity.chooseStream(stream.index, snapshot.generation)
                                            if (accepted) {
                                                keyboard?.hide()
                                                activity.ocultarSeletorFontes()
                                            } else {
                                                snapshot = parseSelectorSnapshot(activity.snapshotStreams())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioSelectorTheme(content: @Composable () -> Unit) {
    val font = FontFamily.SansSerif
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.5).sp),
        displayMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
    )
    androidx.tv.material3.MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9E9E9E),
            onPrimary = Color.White,
            secondary = Color(0xFFF5F5F5),
            onSecondary = Color(0xFF111111),
            background = Color(0xFF0D0D0D),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB3B3B3),
            error = Color(0xFFCF6679)
        ),
        typography = typography,
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    status: Int = 0,
    onClick: () -> Unit,
    onFocus: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFFF5F5F5) else Color(0xFF222222))
            .border(
                BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    if (focused) Color(0xFFE7E8EF) else Color(0x334A4B54)
                ),
                CircleShape
            )
            .clickable(onClick = onClick)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFF111111) else Color(0xFFF5F5F5),
                maxLines = 1
            )
            if (status == 1) {
                Text("…", color = if (selected) Color(0xFF111111) else Color(0xFFB3B3B3))
            } else if (status == 2) {
                Text("!", color = Color(0xFFCF6679))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StreamSourceCard(
    stream: SelectorStream,
    focusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val badges = badgeLabels(stream.badgeMask).ifEmpty {
        buildList {
            if (stream.height > 0) add(if (stream.height >= 2160) "4K" else "${stream.height}p")
            if (stream.dolbyVision) add("Dolby Vision")
            if (stream.dolbyAtmos) add("Atmos")
            if (stream.av1) add("AV1")
            if (stream.mp4) add("MP4")
        }
    }
    Card(
        onClick = {
            if (stream.hasUrl || stream.infoHash.isNotBlank()) onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x22333333)),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF222222),
            focusedContainerColor = Color(0xFF222222)
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(14.dp)),
        scale = CardDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = stream.name.ifBlank { "Unknown stream" },
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF2F2F5),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (stream.description.isNotBlank() && stream.description != stream.name) {
                Text(
                    text = stream.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badges.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    badges.forEach { StreamBadge(it) }
                    if (stream.sizeBytes > 0L && badges.none { it == formatStreamSize(stream.sizeBytes) }) {
                        StreamBadge(formatStreamSize(stream.sizeBytes))
                    }
                }
            } else if (stream.sizeBytes > 0L) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StreamBadge(formatStreamSize(stream.sizeBytes))
                }
            }
        }
    }
}

@Composable
private fun StreamBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF2D2D2D))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD6D7DE),
            maxLines = 1
        )
    }
}

private fun formatStreamSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.ROOT, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.ROOT, "%.0f MB", bytes / 1_048_576.0)
    else -> String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0)
}
