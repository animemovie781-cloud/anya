package com.amaya.intelligence.ui.screens.mcp.local

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amaya.intelligence.data.remote.api.AiSettingsManager
import com.amaya.intelligence.data.remote.api.McpConfig
import com.amaya.intelligence.ui.components.shared.SettingsBackButton
import com.amaya.intelligence.ui.screens.mcp.shared.McpEditSheet
import com.amaya.intelligence.ui.screens.mcp.shared.McpServerList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.amaya.intelligence.ui.screens.amaya.iosAmayaColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMcpScreen(
    onNavigateBack: () -> Unit,
    aiSettingsManager: AiSettingsManager
) {
    val colors = iosAmayaColors()
    val scope = rememberCoroutineScope()
    val maxSheetHeight = (0.98f * LocalConfiguration.current.screenHeightDp).dp
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by aiSettingsManager.settingsFlow.collectAsState(
        initial = com.amaya.intelligence.data.remote.api.AiSettings()
    )

    val mcpConfig by remember(settings.mcpConfigJson) {
        derivedStateOf { McpConfig.fromJson(settings.mcpConfigJson) }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    // null = adding new server, non-null = editing this specific server's full config JSON
    var editorJson by remember { mutableStateOf<String?>(null) }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp

    /** Opens the sheet with a blank template to add a brand-new server. */
    fun openAddSheet() {
        editorJson = null
        showAddSheet = true
    }

    /** Opens the sheet pre-filled with [server]'s JSON so only that entry is edited. */
    fun openEditSheet(server: com.amaya.intelligence.data.remote.api.McpServerConfig) {
        val serverJson = org.json.JSONObject().apply {
            put("mcpServers", org.json.JSONObject().apply {
                put(server.name, org.json.JSONObject().apply {
                    put("serverUrl", server.serverUrl)
                    if (server.headers.isNotEmpty()) {
                        put("headers", org.json.JSONObject(server.headers))
                    }
                    put("enabled", server.enabled)
                })
            })
        }.toString(2)
        editorJson = serverJson
        showAddSheet = true
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                }
                if (!json.isNullOrBlank()) {
                    aiSettingsManager.setMcpConfigJson(json)
                    snackbarHostState.showSnackbar("mcp.json imported")
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(colors.groupedBackground)) {
            McpServerList(
                servers = mcpConfig.servers,
                onServerClick = { server -> openEditSheet(server) },
                onToggleEnabled = { server, enabled ->
                    scope.launch {
                        val updated = mcpConfig.servers.map {
                            if (it.name == server.name) it.copy(enabled = enabled) else it
                        }
                        aiSettingsManager.setMcpConfigJson(McpConfig(updated).toJson())
                    }
                },
                onDelete = { server ->
                    scope.launch {
                        val updated = mcpConfig.servers.filter { it.name != server.name }
                        aiSettingsManager.setMcpConfigJson(McpConfig(updated).toJson())
                        snackbarHostState.showSnackbar("${server.name} removed")
                    }
                },
                topPadding = topPadding
            )

            com.amaya.intelligence.ui.screens.amaya.AmayaTopScrim(
                Modifier.align(Alignment.TopCenter)
            )

            TopAppBar(
                title = { 
                    Text(
                        "MCP Servers", 
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    SettingsBackButton(onClick = onNavigateBack)
                },
                actions = {
                    com.amaya.intelligence.ui.components.shared.AmayaTopBarButton(
                        icon = Icons.Default.Add,
                        onClick = { openAddSheet() },
                        contentDescription = "Add MCP Server",
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp),
                windowInsets = WindowInsets(0.dp)
            )
        }
    }

    if (showAddSheet) {
        val isEditing = editorJson != null
        McpEditSheet(
            initialJson = editorJson ?: "",
            title = if (isEditing) "Edit MCP Server" else "Add MCP Server",
            onDismiss = { showAddSheet = false },
            onSave = { json ->
                showAddSheet = false
                scope.launch {
                    if (isEditing) {
                        // Editing: merge the updated single-server entry into the full config
                        val updatedSingle = com.amaya.intelligence.data.remote.api.McpConfig.fromJson(json)
                        val latestJson = aiSettingsManager.loadMcpConfigFromFixedPath() ?: settings.mcpConfigJson
                        val existing = com.amaya.intelligence.data.remote.api.McpConfig.fromJson(latestJson)
                        // Replace only the servers present in updatedSingle; keep the rest
                        val updatedNames = updatedSingle.servers.map { it.name }.toSet()
                        val merged = existing.servers.filter { it.name !in updatedNames } + updatedSingle.servers
                        aiSettingsManager.setMcpConfigJson(
                            com.amaya.intelligence.data.remote.api.McpConfig(merged).toJson()
                        )
                        snackbarHostState.showSnackbar("Server updated ✓")
                    } else {
                        // Adding: merge the new server(s) into the existing config
                        val newEntries = com.amaya.intelligence.data.remote.api.McpConfig.fromJson(json)
                        val latestJson = aiSettingsManager.loadMcpConfigFromFixedPath() ?: settings.mcpConfigJson
                        val existing = com.amaya.intelligence.data.remote.api.McpConfig.fromJson(latestJson)
                        val newNames = newEntries.servers.map { it.name }.toSet()
                        val merged = existing.servers.filter { it.name !in newNames } + newEntries.servers
                        aiSettingsManager.setMcpConfigJson(
                            com.amaya.intelligence.data.remote.api.McpConfig(merged).toJson()
                        )
                        snackbarHostState.showSnackbar("Server added ✓")
                    }
                }
            }
        )
    }
}
