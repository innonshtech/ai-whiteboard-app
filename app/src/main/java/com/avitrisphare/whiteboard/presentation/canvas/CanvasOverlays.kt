package com.avitrisphare.whiteboard.presentation.canvas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.avitrisphare.whiteboard.presentation.canvas.utility.CalculatorFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.CalculatorUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.PeriodicTableFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.PeriodicTableUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.StopwatchFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.StopwatchUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.TimerFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.TimerUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.UtilityToolKind

@Composable
fun boardFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF1F2937),
    unfocusedTextColor = Color(0xFF1F2937),
    focusedBorderColor = Color(0xFF6366F1),
    unfocusedBorderColor = Color(0xFFE5E7EB),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC),
    cursorColor = Color(0xFF6366F1)
)

@Composable
fun BoardModalHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun BoardModal(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmDestructive: Boolean = false,
    cancelLabel: String? = "Cancel",
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                BoardModalHeader(title, onDismiss)
                Spacer(Modifier.height(14.dp))
                content()
                if (confirmLabel != null || cancelLabel != null) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cancelLabel != null) {
                            TextButton(onClick = onDismiss) {
                                Text(cancelLabel, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        if (confirmLabel != null && onConfirm != null) {
                            val fill = if (confirmDestructive) {
                                Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316)))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFA855F7))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(fill)
                                    .clickable(onClick = onConfirm)
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(confirmLabel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCollaborationDialog(
    context: Context,
    isActive: Boolean,
    userName: String,
    collabRoomId: String,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onUserNameChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = if (!isActive) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = "Live collaboration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5B5BD6)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isActive) {
                    Text(
                        text = "Invite people to collaborate on your drawing.",
                        fontSize = 14.sp,
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Don't worry, the session is end-to-end encrypted, and fully private. Not even our server can see what you draw.",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onStartSession,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5B5BD6),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start session",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Your name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onUserNameChange,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            focusedBorderColor = Color(0xFF5B5BD6),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val roomUrl = "https://avitrisphere.app/#room=$collabRoomId"
                    Text(
                        text = "Link",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = roomUrl,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedBorderColor = Color(0xFFD1D5DB),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AvitriSphare Live Session", roomUrl)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B5BD6),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy link", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val qrBitmap = remember(roomUrl) { QRCodeHelper.generateQRCodeBitmap(roomUrl, 240) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Room QR Code",
                            modifier = Modifier.size(160.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🔒 Don't worry, the session is end-to-end encrypted, and fully private. Not even our server can see what you draw.",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Stopping the session will disconnect you from the room, but you'll be able to continue working with the scene, locally. Note that this won't affect other people, and they'll still be able to collaborate on their version.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStopSession,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasCommandPaletteDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    commands: List<Pair<String, () -> Unit>>
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search tools & commands...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB),
                        focusedBorderColor = Color(0xFF5B5BD6),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                commands
                    .filter { it.first.contains(searchQuery, ignoreCase = true) }
                    .forEach { (name, action) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { action() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, null, tint = Color(0xFF5B5BD6), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(name, fontSize = 14.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                        }
                    }
            }
        }
    }
}

@Composable
fun CanvasHelpDialog(
    panelId: String,
    licenseState: String,
    onDismiss: () -> Unit
) {
    BoardModal(
        title = "Whiteboard guide",
        onDismiss = onDismiss,
        confirmLabel = "Got it",
        onConfirm = onDismiss,
        cancelLabel = null
    ) {
        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text(
                "V selects. H pans. R rectangle, D diamond, O circle, A arrow, L line. P pen, plus pencil, marker, highlighter, and brush. T text. E eraser: Standard erases inside the circle, Smart removes the whole stroke. Palm touches wider than 3 cm are ignored while a stylus is down. K laser, B bucket fill, 9 inserts an image.",
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Drag the ruler, compass, or protractor from the toolbar onto the board. Drag either end to rotate. On the compass, drag the pencil to set the radius in centimetres, then tap it to draw a circle or sweep it to draw an arc. On the protractor, tap the scale to mark an angle in degrees. Draw along the ruler edge to place a straight line with its length. Two fingers on a tool move and rotate it. Drag a tool back to the toolbar to remove it. Graph paper adds a squared background. A rough circle, ellipse, rectangle, square, triangle, line, arrow, star, or diamond offers Convert to shape half a second after you lift the pen. Yes converts it, No keeps the drawing, and no answer converts it after two seconds. Turn Shape recognition off in the menu for free drawing. A converted shape can be moved, resized from its corners, and recoloured.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Pinch with 2 fingers on empty board to zoom from 10% to 500%. Two fingers also pan. Ctrl+Z undoes, Ctrl+Y redoes. Double tap fits the page.", fontSize = 12.sp, color = Color(0xFF6B7280))
            if (panelId.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Panel: $panelId · $licenseState",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
fun ImageSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    searchTerm: String,
    onSearchTermChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectImage: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                BoardModalHeader("Google Images", onDismiss)
                Text(
                    "Search stays in this window. Tap a picture to paste it on the board.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text("Search images") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchTermChange(query.trim()) }),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { onSearchTermChange(query.trim()) }) {
                    Text("Search in this window", color = Color(0xFF5B5BD6))
                }
                AndroidView(
                    factory = { viewContext ->
                        android.webkit.WebView(viewContext).apply {
                            browseInsideApp(onImage = { raw ->
                                val image = googleImageResultUrl(raw) ?: raw
                                if (image.startsWith("http://") || image.startsWith("https://")) {
                                    onSelectImage(image)
                                }
                            })
                        }
                    },
                    update = { web ->
                        val target = if (searchTerm.isBlank()) {
                            "https://www.google.com/search?tbm=isch&udm=2"
                        } else {
                            googleImageSearchUrl(searchTerm)
                        }
                        if (web.tag != target) {
                            web.tag = target
                            web.loadUrl(target)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
            }
        }
    }
}

@Composable
fun YouTubeDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectVideo: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                BoardModalHeader("YouTube", onDismiss)
                Text("Paste a link or tap a video in the results.", fontSize = 12.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text("Link or search") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    if (youtubeVideoId(query) != null) {
                        onSelectVideo(query)
                    }
                }) { Text("Place link on board") }
                AndroidView(
                    factory = { viewContext ->
                        android.webkit.WebView(viewContext).apply {
                            browseInsideApp(onNavigate = { url ->
                                if (youtubeVideoId(url) == null) return@browseInsideApp false
                                onSelectVideo(url)
                                true
                            })
                        }
                    },
                    update = { web ->
                        val target = if (query.isBlank()) "https://m.youtube.com" else youtubeSearchUrl(query)
                        if (web.tag != target) {
                            web.tag = target
                            web.loadUrl(target)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
            }
        }
    }
}

@Composable
fun CanvasSidebarMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRenameBoard: () -> Unit,
    onOpen: () -> Unit,
    onExportLesson: () -> Unit,
    onOpenIwb: () -> Unit,
    onMindmapTemplates: () -> Unit,
    onCommandPalette: () -> Unit,
    onFindOnCanvas: () -> Unit,
    onHelp: () -> Unit,
    onResetCanvas: () -> Unit,
    currentLanguageName: String = "English",
    onChangeLanguage: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { -it },
        exit = fadeOut() + slideOutHorizontally { -it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x40000000))
                .clickable(onClick = onDismiss)
        ) {
            Surface(
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Sidebar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "AvitriSphare",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "Whiteboard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SidebarMenuItem(
                        icon = Icons.Default.Edit,
                        label = "Rename board",
                        onClick = {
                            onDismiss()
                            onRenameBoard()
                        }
                    )

                    // 1. Open (Ctrl+O)
                    SidebarMenuItem(
                        icon = Icons.Default.FolderOpen,
                        label = "Open",
                        shortcut = "Ctrl+O",
                        onClick = {
                            onDismiss()
                            onOpen()
                        }
                    )

                    // 2. Export lesson (PDF, PNG ZIP, IWB)
                    SidebarMenuItem(
                        icon = Icons.Default.IosShare,
                        label = "Export lesson...",
                        shortcut = "Ctrl+Shift+E",
                        isSelected = true,
                        onClick = {
                            onDismiss()
                            onExportLesson()
                        }
                    )

                    // 3. Open GYOASH Lesson (.iwb)...
                    SidebarMenuItem(
                        icon = Icons.Default.FolderOpen,
                        label = "Open GYOASH Lesson (.iwb)...",
                        onClick = {
                            onDismiss()
                            onOpenIwb()
                        }
                    )

                    // 4. Mind Map Templates...
                    SidebarMenuItem(
                        icon = Icons.Default.Hub,
                        label = "Mind Map Templates...",
                        onClick = {
                            onDismiss()
                            onMindmapTemplates()
                        }
                    )

                    Divider(
                        color = Color(0xFFF3F4F6),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 5. Command palette (Ctrl+/)
                    SidebarMenuItem(
                        icon = Icons.Default.Bolt,
                        label = "Command palette",
                        shortcut = "Ctrl+/",
                        onClick = {
                            onDismiss()
                            onCommandPalette()
                        }
                    )

                    // 6. Find on canvas (Ctrl+F)
                    SidebarMenuItem(
                        icon = Icons.Default.Search,
                        label = "Find on canvas",
                        shortcut = "Ctrl+F",
                        onClick = {
                            onDismiss()
                            onFindOnCanvas()
                        }
                    )

                    // 7. Help (?)
                    SidebarMenuItem(
                        icon = Icons.Default.HelpOutline,
                        label = "Help",
                        shortcut = "?",
                        onClick = {
                            onDismiss()
                            onHelp()
                        }
                    )

                    // 8. Reset the canvas
                    SidebarMenuItem(
                        icon = Icons.Default.LayersClear,
                        label = "Reset the canvas",
                        onClick = {
                            onDismiss()
                            onResetCanvas()
                        }
                    )

                    if (onChangeLanguage != null) {
                        Divider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        SidebarMenuItem(
                            icon = Icons.Default.Language,
                            label = "Change Language",
                            shortcut = currentLanguageName,
                            onClick = {
                                onDismiss()
                                onChangeLanguage()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasUtilityPanelsHost(
    toolZOrder: List<UtilityToolKind>,
    timerState: TimerUiState,
    onTimerStateChange: (TimerUiState) -> Unit,
    stopwatchState: StopwatchUiState,
    onStopwatchStateChange: (StopwatchUiState) -> Unit,
    calculatorState: CalculatorUiState,
    onCalculatorStateChange: (CalculatorUiState) -> Unit,
    periodicTableState: PeriodicTableUiState,
    onPeriodicTableStateChange: (PeriodicTableUiState) -> Unit,
    onBringToFront: (UtilityToolKind) -> Unit
) {
    toolZOrder.forEach { toolKind ->
        when (toolKind) {
            UtilityToolKind.TIMER -> {
                if (timerState.isVisible) {
                    TimerFloatingPanel(
                        state = timerState,
                        onStateChange = onTimerStateChange,
                        onBringToFront = { onBringToFront(UtilityToolKind.TIMER) }
                    )
                }
            }
            UtilityToolKind.STOPWATCH -> {
                if (stopwatchState.isVisible) {
                    StopwatchFloatingPanel(
                        state = stopwatchState,
                        onStateChange = onStopwatchStateChange,
                        onBringToFront = { onBringToFront(UtilityToolKind.STOPWATCH) }
                    )
                }
            }
            UtilityToolKind.CALCULATOR -> {
                if (calculatorState.isVisible) {
                    CalculatorFloatingPanel(
                        state = calculatorState,
                        onStateChange = onCalculatorStateChange,
                        onBringToFront = { onBringToFront(UtilityToolKind.CALCULATOR) }
                    )
                }
            }
            UtilityToolKind.PERIODIC_TABLE -> {
                if (periodicTableState.isVisible) {
                    PeriodicTableFloatingPanel(
                        state = periodicTableState,
                        onStateChange = onPeriodicTableStateChange,
                        onBringToFront = { onBringToFront(UtilityToolKind.PERIODIC_TABLE) }
                    )
                }
            }
        }
    }
}
