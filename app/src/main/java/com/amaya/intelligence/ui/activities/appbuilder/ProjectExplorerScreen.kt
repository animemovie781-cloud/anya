package com.amaya.intelligence.ui.activities.appbuilder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

private val ExplorerBg = Color(0xFF0D0E15)
private val ExplorerCard = Color(0xFF161822)
private val ExplorerAccent = Color(0xFF0A84FF)
private val ExplorerTextPrimary = Color(0xFFF2F2F7)
private val ExplorerTextSecondary = Color(0xFF8E8E93)
private val CodeEditorBg = Color(0xFF1E1E2E)

data class TreeItem(
    val file: File,
    val depth: Int,
    val isDirectory: Boolean,
    val isExpanded: Boolean
)

/**
 * Real project file explorer reading actual filesystem structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectExplorerScreen(
    projectDir: File,
    projectName: String,
    onBack: () -> Unit
) {
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
    var selectedFileForView by remember { mutableStateOf<File?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val treeItems = remember(projectDir, expandedPaths, refreshTrigger) {
        buildFileTree(projectDir, depth = 0, expandedPaths)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ExplorerBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                color = ExplorerBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ExplorerTextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = projectName,
                            color = ExplorerTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = projectDir.name,
                            color = ExplorerTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ExplorerAccent)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
            }

            // File Tree List
            if (treeItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No files in project", color = ExplorerTextSecondary, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                ) {
                    items(treeItems, key = { it.file.absolutePath }) { item ->
                        TreeItemRow(
                            item = item,
                            onToggleFolder = {
                                val path = item.file.absolutePath
                                expandedPaths = if (expandedPaths.contains(path)) {
                                    expandedPaths - path
                                } else {
                                    expandedPaths + path
                                }
                            },
                            onOpenFile = { file ->
                                selectedFileForView = file
                            }
                        )
                    }
                }
            }
        }

        // Code Viewer Dialog
        selectedFileForView?.let { file ->
            CodeViewerDialog(
                file = file,
                onDismiss = { selectedFileForView = null }
            )
        }
    }
}

private fun buildFileTree(
    dir: File,
    depth: Int,
    expandedPaths: Set<String>
): List<TreeItem> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    val result = mutableListOf<TreeItem>()
    val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
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

@Composable
private fun TreeItemRow(
    item: TreeItem,
    onToggleFolder: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val indent = (item.depth * 18).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.isDirectory) onToggleFolder()
                else onOpenFile(item.file)
            }
            .padding(start = indent, top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isDirectory) {
            Icon(
                imageVector = if (item.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ExplorerTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (item.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFFFCA28),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Spacer(Modifier.width(22.dp))
            Icon(
                imageVector = getFileIconForExtension(item.file.extension),
                contentDescription = null,
                tint = getFileColorForExtension(item.file.extension),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = item.file.name,
            color = if (item.isDirectory) ExplorerTextPrimary else ExplorerTextPrimary.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = if (item.isDirectory) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (!item.isDirectory) {
            Text(
                text = formatFileSize(item.file.length()),
                color = ExplorerTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun getFileIconForExtension(ext: String): ImageVector {
    return when (ext.lowercase()) {
        "kt", "java" -> Icons.Default.Code
        "xml" -> Icons.Default.DataObject
        "kts", "gradle", "properties" -> Icons.Default.Settings
        "png", "jpg", "jpeg", "webp" -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getFileColorForExtension(ext: String): Color {
    return when (ext.lowercase()) {
        "kt" -> Color(0xFF7F52FF)
        "java" -> Color(0xFFF89820)
        "xml" -> Color(0xFF4CAF50)
        "kts", "gradle" -> Color(0xFF02303A)
        "properties" -> Color(0xFF29B6F6)
        "png", "jpg" -> Color(0xFFE91E63)
        else -> ExplorerTextSecondary
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

@Composable
fun CodeViewerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val fileContent = remember(file) {
        try {
            if (file.length() > 500_000) {
                "File too large to display (${formatFileSize(file.length())})"
            } else {
                file.readText(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            "Unable to read file: ${e.message}"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CodeEditorBg,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ExplorerCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getFileIconForExtension(file.extension),
                        contentDescription = null,
                        tint = getFileColorForExtension(file.extension),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = ExplorerTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatFileSize(file.length())} • ${fileContent.lines().size} lines",
                            color = ExplorerTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(fileContent))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ExplorerAccent)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ExplorerTextSecondary)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // Code Area with line numbers
                val lines = remember(fileContent) { fileContent.lines() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CodeEditorBg)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Row {
                        // Line numbers column
                        Column(modifier = Modifier.padding(end = 12.dp)) {
                            lines.indices.forEach { index ->
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White.copy(alpha = 0.25f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Code content
                        Column {
                            lines.forEach { line ->
                                Text(
                                    text = line.ifEmpty { " " },
                                    color = ExplorerTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
