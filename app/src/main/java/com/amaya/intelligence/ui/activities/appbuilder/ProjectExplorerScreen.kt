package com.amaya.intelligence.ui.activities.appbuilder

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR PALETTE — Premium dark theme (Catppuccin-inspired)
// ═══════════════════════════════════════════════════════════════════════════════

private val EditorBg           = Color(0xFF0D0E15)
private val EditorSurface      = Color(0xFF161822)
private val EditorGutter       = Color(0xFF12131A)
private val EditorCurrentLine  = Color(0xFF1A1B2E)
private val EditorCardBg       = Color(0xFF1C1D2B)
private val AccentBlue         = Color(0xFF0A84FF)
private val AccentCyan         = Color(0xFF00D4AA)
private val TextPrimary        = Color(0xFFF2F2F7)
private val TextSecondary      = Color(0xFF8E8E93)
private val TextMuted          = Color(0xFF48495A)
private val DividerClr         = Color.White.copy(alpha = 0.06f)
private val FolderYellow       = Color(0xFFFFCA28)
private val FolderOpenYellow   = Color(0xFFFFE082)

// Syntax highlighting colors
private val SyntaxKeyword      = Color(0xFFCF8EF4)   // purple — fun, class, val, var, if, when…
private val SyntaxType         = Color(0xFF66D9EF)   // cyan — types, classes
private val SyntaxString       = Color(0xFFE6DB74)   // yellow
private val SyntaxComment      = Color(0xFF5C6370)   // gray
private val SyntaxNumber       = Color(0xFFF78C6C)   // orange
private val SyntaxAnnotation   = Color(0xFF56D6C2)   // teal — @Composable etc.
private val SyntaxFunction     = Color(0xFF82AAFF)   // blue — function calls
private val SyntaxXmlTag       = Color(0xFFFF6188)   // red-pink — XML tags
private val SyntaxXmlAttr      = Color(0xFFA9DC76)   // green — XML attribute names

// File type colors
private val KotlinPurple = Color(0xFF7F52FF)
private val JavaOrange   = Color(0xFFF89820)
private val XmlGreen     = Color(0xFF4CAF50)
private val GradleTeal   = Color(0xFF02303A)
private val PropBlue     = Color(0xFF29B6F6)
private val ImagePink    = Color(0xFFE91E63)

// ═══════════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════════

data class TreeItem(
    val file: File,
    val depth: Int,
    val isDirectory: Boolean,
    val isExpanded: Boolean
)

data class OpenTab(
    val file: File,
    val content: TextFieldValue,
    val originalContent: String,
    val undoStack: List<TextFieldValue> = emptyList(),
    val redoStack: List<TextFieldValue> = emptyList()
)

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN — Drawer File Explorer + Tabbed Code Editor
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Premium project file explorer + code editor.
 * Left drawer shows the file tree; main area shows tabbed code editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectExplorerScreen(
    projectDir: File,
    projectName: String,
    onBack: () -> Unit
) {
    // ── State ────────────────────────────────────────────────────────────────
    var isExplorerOpen by remember { mutableStateOf(true) }
    var expandedPaths by remember {
        mutableStateOf(
            setOf(
                projectDir.absolutePath,
                File(projectDir, "app").absolutePath,
                File(projectDir, "app/src").absolutePath,
                File(projectDir, "app/src/main").absolutePath
            )
        )
    }
    var refreshTrigger by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var openTabs by remember { mutableStateOf<List<OpenTab>>(emptyList()) }
    var activeTabIndex by remember { mutableStateOf(-1) }
    var contextMenuFile by remember { mutableStateOf<File?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    // Build the file tree
    val treeItems = remember(projectDir, expandedPaths, refreshTrigger) {
        buildFileTree(projectDir, depth = 0, expandedPaths)
    }

    // Filtered tree items when searching
    val displayedItems = remember(treeItems, searchQuery) {
        if (searchQuery.isBlank()) treeItems
        else treeItems.filter {
            it.file.name.contains(searchQuery, ignoreCase = true)
        }
    }

    // Helpers
    fun openFile(file: File) {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
        } else {
            val content = try {
                if (file.length() > 500_000) "File too large to display (${formatFileSize(file.length())})"
                else file.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                "Unable to read file: ${e.message}"
            }
            val tfv = TextFieldValue(content)
            openTabs = openTabs + OpenTab(file = file, content = tfv, originalContent = content)
            activeTabIndex = openTabs.size // will be last index after add
        }
    }

    fun closeTab(index: Int) {
        if (index < 0 || index >= openTabs.size) return
        openTabs = openTabs.toMutableList().also { it.removeAt(index) }
        activeTabIndex = when {
            openTabs.isEmpty() -> -1
            activeTabIndex >= openTabs.size -> openTabs.size - 1
            activeTabIndex > index -> activeTabIndex - 1
            else -> activeTabIndex.coerceIn(-1, openTabs.size - 1)
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Toolbar ──────────────────────────────────────────────
            ExplorerEditorToolbar(
                projectName = projectName,
                isExplorerOpen = isExplorerOpen,
                onToggleExplorer = { isExplorerOpen = !isExplorerOpen },
                onBack = onBack,
                onRefresh = { refreshTrigger++ },
                hasUnsavedChanges = openTabs.any { it.content.text != it.originalContent },
                onSaveAll = {
                    openTabs = openTabs.map { tab ->
                        if (tab.content.text != tab.originalContent) {
                            try {
                                tab.file.writeText(tab.content.text, Charsets.UTF_8)
                                tab.copy(originalContent = tab.content.text)
                            } catch (_: Exception) {
                                tab
                            }
                        } else tab
                    }
                }
            )

            // ── Main Content: Explorer (drawer) + Editor ─────────────────
            Row(modifier = Modifier.weight(1f)) {
                // File Explorer Panel (animated slide)
                AnimatedVisibility(
                    visible = isExplorerOpen,
                    enter = expandHorizontally(
                        expandFrom = Alignment.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkHorizontally(
                        shrinkTowards = Alignment.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    FileExplorerPanel(
                        projectDir = projectDir,
                        treeItems = displayedItems,
                        expandedPaths = expandedPaths,
                        searchQuery = searchQuery,
                        isSearching = isSearching,
                        onSearchQueryChange = { searchQuery = it },
                        onToggleSearch = { isSearching = !isSearching; if (!isSearching) searchQuery = "" },
                        onToggleFolder = { path ->
                            expandedPaths = if (expandedPaths.contains(path))
                                expandedPaths - path
                            else
                                expandedPaths + path
                        },
                        onOpenFile = { file -> openFile(file) },
                        onLongPress = { file -> contextMenuFile = file },
                        activeFilePath = openTabs.getOrNull(activeTabIndex)?.file?.absolutePath,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                    )
                }

                // Vertical divider between explorer and editor
                if (isExplorerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(DividerClr)
                    )
                }

                // ── Code Editor Area ─────────────────────────────────────
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (openTabs.isEmpty()) {
                        // Empty state
                        EmptyEditorState(onOpenExplorer = { isExplorerOpen = true })
                    } else {
                        // Tab bar
                        EditorTabBar(
                            tabs = openTabs,
                            activeIndex = activeTabIndex,
                            onTabClick = { activeTabIndex = it },
                            onTabClose = { closeTab(it) }
                        )

                        // Editor content
                        val currentTab = openTabs.getOrNull(activeTabIndex)
                        if (currentTab != null) {
                            CodeEditorPane(
                                tab = currentTab,
                                onContentChange = { newValue ->
                                    openTabs = openTabs.toMutableList().also { list ->
                                        val old = list[activeTabIndex]
                                        list[activeTabIndex] = old.copy(
                                            content = newValue,
                                            undoStack = old.undoStack + listOf(old.content),
                                            redoStack = emptyList()
                                        )
                                    }
                                },
                                onUndo = {
                                    openTabs = openTabs.toMutableList().also { list ->
                                        val old = list[activeTabIndex]
                                        if (old.undoStack.isNotEmpty()) {
                                            val prev = old.undoStack.last()
                                            list[activeTabIndex] = old.copy(
                                                content = prev,
                                                undoStack = old.undoStack.dropLast(1),
                                                redoStack = old.redoStack + listOf(old.content)
                                            )
                                        }
                                    }
                                },
                                onRedo = {
                                    openTabs = openTabs.toMutableList().also { list ->
                                        val old = list[activeTabIndex]
                                        if (old.redoStack.isNotEmpty()) {
                                            val next = old.redoStack.last()
                                            list[activeTabIndex] = old.copy(
                                                content = next,
                                                redoStack = old.redoStack.dropLast(1),
                                                undoStack = old.undoStack + listOf(old.content)
                                            )
                                        }
                                    }
                                },
                                onSave = {
                                    openTabs = openTabs.toMutableList().also { list ->
                                        val tab = list[activeTabIndex]
                                        try {
                                            tab.file.writeText(tab.content.text, Charsets.UTF_8)
                                            list[activeTabIndex] = tab.copy(originalContent = tab.content.text)
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Status bar
                            EditorStatusBar(
                                tab = currentTab,
                                isModified = currentTab.content.text != currentTab.originalContent
                            )
                        }
                    }
                }
            }
        }

        // ── Context Menu Popup ───────────────────────────────────────────
        contextMenuFile?.let { file ->
            FileContextMenu(
                file = file,
                onDismiss = { contextMenuFile = null },
                onOpenFile = {
                    if (!file.isDirectory) openFile(file)
                    contextMenuFile = null
                },
                onCopyPath = {
                    contextMenuFile = null
                },
                onDelete = {
                    try {
                        file.deleteRecursively()
                        refreshTrigger++
                    } catch (_: Exception) {}
                    contextMenuFile = null
                },
                onNewFile = {
                    val dir = if (file.isDirectory) file else file.parentFile
                    if (dir != null) {
                        val newFile = File(dir, "NewFile.kt")
                        newFile.writeText("// New file\n")
                        expandedPaths = expandedPaths + dir.absolutePath
                        refreshTrigger++
                    }
                    contextMenuFile = null
                },
                onNewFolder = {
                    val dir = if (file.isDirectory) file else file.parentFile
                    if (dir != null) {
                        val newDir = File(dir, "newfolder")
                        newDir.mkdirs()
                        expandedPaths = expandedPaths + dir.absolutePath
                        refreshTrigger++
                    }
                    contextMenuFile = null
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP TOOLBAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExplorerEditorToolbar(
    projectName: String,
    isExplorerOpen: Boolean,
    onToggleExplorer: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    hasUnsavedChanges: Boolean,
    onSaveAll: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        color = EditorSurface,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                // Toggle explorer
                IconButton(onClick = onToggleExplorer) {
                    Icon(
                        if (isExplorerOpen) Icons.Default.ViewSidebar else Icons.Default.Menu,
                        contentDescription = "Toggle Explorer",
                        tint = if (isExplorerOpen) AccentBlue else TextSecondary
                    )
                }

                // Project name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = projectName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Save all
                if (hasUnsavedChanges) {
                    IconButton(onClick = {
                        onSaveAll()
                        Toast.makeText(context, "All files saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save All", tint = AccentCyan)
                    }
                }

                // Refresh
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                }
            }
            HorizontalDivider(color = DividerClr, thickness = 0.5.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILE EXPLORER PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FileExplorerPanel(
    projectDir: File,
    treeItems: List<TreeItem>,
    expandedPaths: Set<String>,
    searchQuery: String,
    isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onToggleFolder: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onLongPress: (File) -> Unit,
    activeFilePath: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(EditorBg)
    ) {
        // ── Explorer Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = FolderYellow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "EXPLORER",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearching) AccentBlue else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Search Bar ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EditorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Default
                        ),
                        cursorBrush = SolidColor(AccentBlue),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search files...", color = TextMuted, fontSize = 13.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // ── Breadcrumb ───────────────────────────────────────────────────
        BreadcrumbBar(projectDir = projectDir)

        HorizontalDivider(color = DividerClr, thickness = 0.5.dp)

        // ── File Tree ────────────────────────────────────────────────────
        if (treeItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (searchQuery.isNotBlank()) "No matching files" else "Empty project",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp)
            ) {
                items(treeItems, key = { it.file.absolutePath }) { item ->
                    FileTreeRow(
                        item = item,
                        isActive = item.file.absolutePath == activeFilePath,
                        onToggleFolder = { onToggleFolder(item.file.absolutePath) },
                        onOpenFile = { onOpenFile(item.file) },
                        onLongPress = { onLongPress(item.file) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BREADCRUMB BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BreadcrumbBar(projectDir: File) {
    val parts = remember(projectDir) {
        val name = projectDir.name
        listOf(name, "app", "src", "main")
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(parts) { index, part ->
            if (index > 0) {
                Text("›", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp))
            }
            Text(
                text = part,
                color = if (index == parts.lastIndex) AccentBlue else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (index == parts.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILE TREE ROW (with connector lines and animations)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FileTreeRow(
    item: TreeItem,
    isActive: Boolean,
    onToggleFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onLongPress: () -> Unit
) {
    val indent = (item.depth * 16).dp
    val chevronRotation by animateFloatAsState(
        targetValue = if (item.isExpanded) 90f else 0f,
        animationSpec = tween(180),
        label = "chevron"
    )

    val bgColor = when {
        isActive -> AccentBlue.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    val connectorColor = TextMuted.copy(alpha = 0.25f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (item.isDirectory) onToggleFolder()
                        else onOpenFile()
                    },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(start = 8.dp + indent, top = 3.dp, bottom = 3.dp, end = 8.dp)
            // Draw connector lines
            .drawBehind {
                if (item.depth > 0) {
                    for (i in 0 until item.depth) {
                        val x = (8 + i * 16).dp.toPx() + 8.dp.toPx()
                        drawLine(
                            color = connectorColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isDirectory) {
            // Folder chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
            Spacer(Modifier.width(4.dp))
            // Folder icon
            Icon(
                imageVector = if (item.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = if (item.isExpanded) FolderOpenYellow else FolderYellow,
                modifier = Modifier.size(18.dp)
            )
        } else {
            // File icon indent
            Spacer(Modifier.width(18.dp))
            Icon(
                imageVector = getFileIcon(item.file.extension),
                contentDescription = null,
                tint = getFileColor(item.file.extension),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // File/folder name
        Text(
            text = item.file.name,
            color = if (isActive) AccentBlue else if (item.isDirectory) TextPrimary else TextPrimary.copy(alpha = 0.88f),
            fontSize = 13.sp,
            fontWeight = if (item.isDirectory) FontWeight.Medium else FontWeight.Normal,
            fontFamily = FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // File info badge
        if (!item.isDirectory) {
            Text(
                text = formatFileSize(item.file.length()),
                color = TextMuted,
                fontSize = 10.sp
            )
        } else {
            // Child count badge
            val childCount = item.file.listFiles()?.size ?: 0
            if (childCount > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EditorSurface,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "$childCount",
                        color = TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EDITOR TAB BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EditorTabBar(
    tabs: List<OpenTab>,
    activeIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit
) {
    Surface(color = EditorSurface) {
        Column {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(tabs) { index, tab ->
                    val isActive = index == activeIndex
                    val isModified = tab.content.text != tab.originalContent

                    Surface(
                        color = if (isActive) EditorBg else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onTabClick(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .height(36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // File icon
                            Icon(
                                imageVector = getFileIcon(tab.file.extension),
                                contentDescription = null,
                                tint = getFileColor(tab.file.extension),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))

                            // File name
                            Text(
                                text = tab.file.name,
                                color = if (isActive) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1
                            )

                            // Modified indicator dot
                            if (isModified) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            // Close button
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = if (isActive) TextSecondary else TextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .clickable { onTabClose(index) }
                            )
                        }
                    }

                    // Tab separator
                    if (index < tabs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(DividerClr)
                        )
                    }
                }
            }

            // Active tab bottom accent line
            HorizontalDivider(color = DividerClr, thickness = 0.5.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CODE EDITOR PANE (with syntax highlighting + line numbers)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CodeEditorPane(
    tab: OpenTab,
    onContentChange: (TextFieldValue) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isModified = tab.content.text != tab.originalContent

    Column(modifier = modifier.background(EditorBg)) {
        // ── Editor Action Bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorSurface.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Undo
            IconButton(
                onClick = onUndo,
                enabled = tab.undoStack.isNotEmpty(),
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (tab.undoStack.isNotEmpty()) TextSecondary else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Redo
            IconButton(
                onClick = onRedo,
                enabled = tab.redoStack.isNotEmpty(),
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (tab.redoStack.isNotEmpty()) TextSecondary else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Copy all
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(tab.content.text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }

            // Save
            IconButton(
                onClick = {
                    onSave()
                    Toast.makeText(context, "Saved ${tab.file.name}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (isModified) AccentCyan else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(color = DividerClr, thickness = 0.5.dp)

        // ── Code Area with Line Numbers ──────────────────────────────────
        val lines = remember(tab.content.text) { tab.content.text.lines() }
        val scrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        // Calculate current line from cursor position
        val currentLine = remember(tab.content.selection) {
            val pos = tab.content.selection.start
            tab.content.text.substring(0, pos.coerceAtMost(tab.content.text.length)).count { it == '\n' }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ── Line Numbers Gutter ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(EditorGutter)
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                lines.indices.forEach { index ->
                    val isCurrentLine = index == currentLine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(if (isCurrentLine) EditorCurrentLine else Color.Transparent),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrentLine) AccentBlue else TextMuted.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // Gutter separator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(DividerClr)
            )

            // ── Code Content ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .horizontalScroll(horizontalScrollState)
                    .background(EditorBg)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                // Syntax highlighted editable text
                val fileExt = tab.file.extension.lowercase()

                BasicTextField(
                    value = tab.content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 500.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(AccentBlue),
                    decorationBox = { innerTextField ->
                        // Overlay the syntax-highlighted text
                        Box {
                            // Show syntax highlighting as background layer
                            Text(
                                text = highlightSyntax(tab.content.text, fileExt),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                            // Invisible editable layer on top
                            Box(modifier = Modifier.matchParentSize()) {
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EditorStatusBar(
    tab: OpenTab,
    isModified: Boolean
) {
    val cursorPos = tab.content.selection.start
    val textBeforeCursor = tab.content.text.substring(0, cursorPos.coerceAtMost(tab.content.text.length))
    val currentLine = textBeforeCursor.count { it == '\n' } + 1
    val currentCol = textBeforeCursor.length - textBeforeCursor.lastIndexOf('\n')
    val totalLines = tab.content.text.lines().size
    val language = getLanguageName(tab.file.extension)

    Surface(color = EditorSurface) {
        Column {
            HorizontalDivider(color = DividerClr, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Modified indicator
                if (isModified) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Modified", color = AccentCyan, fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                // Line:Col
                Text(
                    text = "Ln $currentLine, Col $currentCol",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Total lines
                Text(
                    text = "$totalLines lines",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                // Encoding
                Text(
                    text = "UTF-8",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                // Language
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getFileColor(tab.file.extension).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = language,
                        color = getFileColor(tab.file.extension),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMPTY EDITOR STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyEditorState(onOpenExplorer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated code icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = AccentBlue.copy(alpha = alpha),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "No file open",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a file from the explorer to start editing",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            // Open Explorer button
            OutlinedButton(
                onClick = onOpenExplorer,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Explorer", fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Keyboard shortcuts hint
            Column(horizontalAlignment = Alignment.Start) {
                ShortcutHint(icon = Icons.Default.Save, text = "Save file")
                ShortcutHint(icon = Icons.Default.Undo, text = "Undo changes")
                ShortcutHint(icon = Icons.Default.Redo, text = "Redo changes")
                ShortcutHint(icon = Icons.Default.ContentCopy, text = "Copy code")
            }
        }
    }
}

@Composable
private fun ShortcutHint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextMuted, fontSize = 11.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXT MENU
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FileContextMenu(
    file: File,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onCopyPath: () -> Unit,
    onDelete: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.extension),
                    contentDescription = null,
                    tint = if (file.isDirectory) FolderYellow else getFileColor(file.extension),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    file.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column {
                if (!file.isDirectory) {
                    ContextMenuItem(Icons.Default.OpenInNew, "Open", AccentBlue) { onOpenFile() }
                }
                ContextMenuItem(Icons.Default.ContentCopy, "Copy Path", TextSecondary) {
                    clipboardManager.setText(AnnotatedString(file.absolutePath))
                    Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                    onCopyPath()
                }
                if (file.isDirectory) {
                    ContextMenuItem(Icons.Default.NoteAdd, "New File", AccentCyan) { onNewFile() }
                    ContextMenuItem(Icons.Default.CreateNewFolder, "New Folder", FolderYellow) { onNewFolder() }
                }
                HorizontalDivider(color = DividerClr, modifier = Modifier.padding(vertical = 4.dp))
                ContextMenuItem(Icons.Default.Delete, "Delete", Color(0xFFFF6B6B)) { onDelete() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SYNTAX HIGHLIGHTING ENGINE
// ═══════════════════════════════════════════════════════════════════════════════

private fun highlightSyntax(code: String, extension: String): AnnotatedString {
    return when (extension) {
        "kt", "kts" -> highlightKotlin(code)
        "java" -> highlightJava(code)
        "xml" -> highlightXml(code)
        "gradle" -> highlightKotlin(code) // Gradle KTS uses Kotlin syntax
        "properties" -> highlightProperties(code)
        else -> buildAnnotatedString { append(code) }
    }
}

private fun highlightKotlin(code: String): AnnotatedString {
    val keywords = setOf(
        "fun", "class", "object", "interface", "val", "var", "const",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "package", "import", "as", "is", "in", "by", "override", "open", "abstract",
        "sealed", "data", "enum", "companion", "private", "protected", "internal",
        "public", "suspend", "inline", "crossinline", "noinline", "reified",
        "try", "catch", "finally", "throw", "true", "false", "null",
        "this", "super", "it", "constructor", "init", "typealias",
        "lateinit", "lazy", "get", "set", "field"
    )

    return buildHighlightedString(code, keywords)
}

private fun highlightJava(code: String): AnnotatedString {
    val keywords = setOf(
        "public", "private", "protected", "static", "final", "abstract",
        "class", "interface", "extends", "implements", "new", "this", "super",
        "if", "else", "for", "while", "do", "switch", "case", "default",
        "break", "continue", "return", "try", "catch", "finally", "throw", "throws",
        "import", "package", "void", "int", "long", "double", "float", "boolean",
        "char", "byte", "short", "String", "true", "false", "null",
        "synchronized", "volatile", "transient", "instanceof", "enum"
    )

    return buildHighlightedString(code, keywords)
}

private fun buildHighlightedString(code: String, keywords: Set<String>): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = code.length

        while (i < len) {
            val ch = code[i]

            // ── Line comments ────────────────────────────────────────────
            if (ch == '/' && i + 1 < len && code[i + 1] == '/') {
                val end = code.indexOf('\n', i).let { if (it == -1) len else it }
                withStyle(SpanStyle(color = SyntaxComment)) {
                    append(code.substring(i, end))
                }
                i = end
                continue
            }

            // ── Block comments ───────────────────────────────────────────
            if (ch == '/' && i + 1 < len && code[i + 1] == '*') {
                val end = code.indexOf("*/", i + 2).let { if (it == -1) len else it + 2 }
                withStyle(SpanStyle(color = SyntaxComment)) {
                    append(code.substring(i, end))
                }
                i = end
                continue
            }

            // ── Strings ──────────────────────────────────────────────────
            if (ch == '"') {
                val sb = StringBuilder()
                sb.append(ch)
                i++
                while (i < len && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < len) {
                        sb.append(code[i])
                        i++
                    }
                    sb.append(code[i])
                    i++
                }
                if (i < len) {
                    sb.append(code[i])
                    i++
                }
                withStyle(SpanStyle(color = SyntaxString)) {
                    append(sb.toString())
                }
                continue
            }

            // ── Annotations ──────────────────────────────────────────────
            if (ch == '@') {
                val sb = StringBuilder()
                sb.append(ch)
                i++
                while (i < len && (code[i].isLetterOrDigit() || code[i] == '_')) {
                    sb.append(code[i])
                    i++
                }
                withStyle(SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.Medium)) {
                    append(sb.toString())
                }
                continue
            }

            // ── Numbers ──────────────────────────────────────────────────
            if (ch.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit())) {
                val sb = StringBuilder()
                while (i < len && (code[i].isDigit() || code[i] == '.' || code[i] == 'f' || code[i] == 'L' || code[i] == 'x' || code[i] == 'X')) {
                    sb.append(code[i])
                    i++
                }
                withStyle(SpanStyle(color = SyntaxNumber)) {
                    append(sb.toString())
                }
                continue
            }

            // ── Words (keywords, identifiers) ────────────────────────────
            if (ch.isLetter() || ch == '_') {
                val sb = StringBuilder()
                while (i < len && (code[i].isLetterOrDigit() || code[i] == '_')) {
                    sb.append(code[i])
                    i++
                }
                val word = sb.toString()
                when {
                    word in keywords -> withStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold)) {
                        append(word)
                    }
                    word.first().isUpperCase() -> withStyle(SpanStyle(color = SyntaxType)) {
                        append(word)
                    }
                    i < len && code[i] == '(' -> withStyle(SpanStyle(color = SyntaxFunction)) {
                        append(word)
                    }
                    else -> withStyle(SpanStyle(color = TextPrimary)) {
                        append(word)
                    }
                }
                continue
            }

            // ── Default character ────────────────────────────────────────
            withStyle(SpanStyle(color = TextPrimary)) {
                append(ch.toString())
            }
            i++
        }
    }
}

private fun highlightXml(code: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = code.length

        while (i < len) {
            val ch = code[i]

            // ── XML comments ─────────────────────────────────────────────
            if (ch == '<' && i + 3 < len && code.substring(i, i + 4) == "<!--") {
                val end = code.indexOf("-->", i + 4).let { if (it == -1) len else it + 3 }
                withStyle(SpanStyle(color = SyntaxComment)) {
                    append(code.substring(i, end))
                }
                i = end
                continue
            }

            // ── XML tags ─────────────────────────────────────────────────
            if (ch == '<') {
                withStyle(SpanStyle(color = TextMuted)) { append("<") }
                i++
                // Possible closing slash
                if (i < len && code[i] == '/') {
                    withStyle(SpanStyle(color = TextMuted)) { append("/") }
                    i++
                }
                // Tag name
                val sb = StringBuilder()
                while (i < len && code[i] != ' ' && code[i] != '>' && code[i] != '/' && code[i] != '\n') {
                    sb.append(code[i])
                    i++
                }
                withStyle(SpanStyle(color = SyntaxXmlTag, fontWeight = FontWeight.Medium)) {
                    append(sb.toString())
                }

                // Attributes inside the tag
                while (i < len && code[i] != '>') {
                    if (code[i] == '"') {
                        // Attribute value
                        val valSb = StringBuilder()
                        valSb.append(code[i])
                        i++
                        while (i < len && code[i] != '"') {
                            valSb.append(code[i])
                            i++
                        }
                        if (i < len) { valSb.append(code[i]); i++ }
                        withStyle(SpanStyle(color = SyntaxString)) {
                            append(valSb.toString())
                        }
                    } else if (code[i].isLetter() || code[i] == ':') {
                        // Attribute name
                        val attrSb = StringBuilder()
                        while (i < len && code[i] != '=' && code[i] != '>' && code[i] != ' ' && code[i] != '\n') {
                            attrSb.append(code[i])
                            i++
                        }
                        withStyle(SpanStyle(color = SyntaxXmlAttr)) {
                            append(attrSb.toString())
                        }
                    } else {
                        withStyle(SpanStyle(color = TextPrimary)) { append(code[i].toString()) }
                        i++
                    }
                }
                if (i < len) {
                    withStyle(SpanStyle(color = TextMuted)) { append(">") }
                    i++
                }
                continue
            }

            // Default text
            withStyle(SpanStyle(color = TextPrimary)) { append(ch.toString()) }
            i++
        }
    }
}

private fun highlightProperties(code: String): AnnotatedString {
    return buildAnnotatedString {
        code.lines().forEachIndexed { index, line ->
            if (index > 0) append("\n")

            when {
                line.trimStart().startsWith("#") || line.trimStart().startsWith("!") -> {
                    withStyle(SpanStyle(color = SyntaxComment)) { append(line) }
                }
                line.contains("=") -> {
                    val eqIdx = line.indexOf('=')
                    withStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Medium)) {
                        append(line.substring(0, eqIdx))
                    }
                    withStyle(SpanStyle(color = TextMuted)) { append("=") }
                    withStyle(SpanStyle(color = SyntaxString)) {
                        append(line.substring(eqIdx + 1))
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = TextPrimary)) { append(line) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════

private fun buildFileTree(
    dir: File,
    depth: Int,
    expandedPaths: Set<String>
): List<TreeItem> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    val result = mutableListOf<TreeItem>()
    val files = dir.listFiles()
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: return emptyList()

    for (file in files) {
        val isDir = file.isDirectory
        val isExpanded = expandedPaths.contains(file.absolutePath)
        result.add(TreeItem(file, depth, isDir, isExpanded))

        if (isDir && isExpanded) {
            result.addAll(buildFileTree(file, depth + 1, expandedPaths))
        }
    }

    return result
}

private fun getFileIcon(ext: String): ImageVector {
    return when (ext.lowercase()) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.Code
        "xml" -> Icons.Default.DataObject
        "gradle" -> Icons.Default.Settings
        "properties" -> Icons.Default.Tune
        "json" -> Icons.Default.DataObject
        "md", "txt" -> Icons.Default.Description
        "png", "jpg", "jpeg", "webp", "svg" -> Icons.Default.Image
        "pro" -> Icons.Default.Shield
        "bat", "sh" -> Icons.Default.Terminal
        "gitignore" -> Icons.Default.VisibilityOff
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getFileColor(ext: String): Color {
    return when (ext.lowercase()) {
        "kt", "kts" -> KotlinPurple
        "java" -> JavaOrange
        "xml" -> XmlGreen
        "gradle" -> GradleTeal
        "properties" -> PropBlue
        "json" -> Color(0xFFFBC02D)
        "md" -> Color(0xFF42A5F5)
        "txt" -> TextSecondary
        "png", "jpg", "jpeg", "webp" -> ImagePink
        "pro" -> Color(0xFFFF7043)
        "bat", "sh" -> Color(0xFF66BB6A)
        "gitignore" -> TextMuted
        else -> TextSecondary
    }
}

private fun getLanguageName(ext: String): String {
    return when (ext.lowercase()) {
        "kt" -> "Kotlin"
        "kts" -> "Kotlin Script"
        "java" -> "Java"
        "xml" -> "XML"
        "gradle" -> "Gradle"
        "properties" -> "Properties"
        "json" -> "JSON"
        "md" -> "Markdown"
        "txt" -> "Plain Text"
        "pro" -> "ProGuard"
        "bat" -> "Batch"
        "sh" -> "Shell"
        else -> ext.uppercase()
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
