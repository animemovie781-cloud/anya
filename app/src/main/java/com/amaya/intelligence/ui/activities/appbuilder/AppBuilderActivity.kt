package com.amaya.intelligence.ui.activities.appbuilder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.amaya.intelligence.appbuilder.engine.GenerationResult
import com.amaya.intelligence.appbuilder.engine.ProjectConfig
import com.amaya.intelligence.appbuilder.engine.ProjectManager
import com.amaya.intelligence.appbuilder.engine.TemplateManager
import java.io.File
import com.amaya.intelligence.ui.theme.AmayaTheme
import dagger.hilt.android.AndroidEntryPoint

// ─── Sample Data Model ────────────────────────────────────────────────────────

data class AppProject(
    val id: String,
    val appName: String,
    val workspaceName: String,
    val versionName: String,
    val versionCode: String,
    val packageName: String,
    val isPinned: Boolean = false
)

private val sampleProjects = listOf(
    AppProject("789", "jc",    "NewProject22", "1.0", "1", "com.my.newproject22"),
    AppProject("788", "new",   "NewProject21", "1.0", "1", "com.my.newproject21"),
    AppProject("787", "kanha", "NewProject19", "1.0", "1", "com.my.newproject19"),
    AppProject("786", "kanah", "NewProject18", "1.0", "1", "com.my.newproject18"),
    AppProject("785", "Ystudio", "YstudioProject", "1.0", "1", "com.yuvextech.Ystudio"),
    AppProject("784", "Vectr",  "Vectr_AI_IDE", "1.2", "1", "genius.DMTech.Vectr"),
    AppProject("783", "Alpha",  "AlphaProject", "2.0", "3", "dev.alpha.studio"),
    AppProject("782", "Beta",   "BetaApp",      "1.5", "2", "com.beta.app"),
)

// ─── Activity ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class AppBuilderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmayaTheme {
                AppBuilderScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(context, AppBuilderActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

// ─── Color palette (matches screenshot dark theme) ────────────────────────────

private val BgColor       = Color(0xFF0B0B0F)
private val SurfaceColor  = Color(0xFF1C1C1E)
private val BorderColor   = Color.White.copy(alpha = 0.09f)
private val PrimaryText   = Color(0xFFF2F2F7)
private val SecondaryText = Color(0xFFEBEBF5).copy(alpha = 0.55f)
private val AccentBlue    = Color(0xFF0A84FF)
private val CardBg        = Color(0xFF1C1C1E)
private val DividerColor  = Color.White.copy(alpha = 0.08f)
private val AndroidGreen  = Color(0xFF3DDC84)

// ─── Root Composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBuilderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val projectManager = remember { ProjectManager(TemplateManager(context)) }
    val projectsRoot = remember { projectManager.getProjectsRoot(context) }

    var selectedTab  by remember { mutableStateOf(0) }            // 0=Projects, 1=Store
    var searchQuery  by remember { mutableStateOf("") }
    var isSearching  by remember { mutableStateOf(false) }
    var projects     by remember { mutableStateOf(sampleProjects) }
    var showNewProject by remember { mutableStateOf(false) }
    var activeProjectDir by remember { mutableStateOf<File?>(null) }
    var activeProjectName by remember { mutableStateOf<String?>(null) }

    // Load real projects from disk and merge with sample projects
    LaunchedEffect(Unit) {
        val realProjects = projectManager.listProjects(projectsRoot)
        if (realProjects.isNotEmpty()) {
            val realWorkspaces = realProjects.map { it.workspaceName }.toSet()
            val remainingSamples = sampleProjects.filter { it.workspaceName !in realWorkspaces }
            projects = realProjects + remainingSamples
        }
    }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { p ->
            p.appName.contains(searchQuery, ignoreCase = true) ||
            p.workspaceName.contains(searchQuery, ignoreCase = true) ||
            p.packageName.contains(searchQuery, ignoreCase = true) ||
            p.id.contains(searchQuery)
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            AppBuilderTopBar(
                isSearching   = isSearching,
                searchQuery   = searchQuery,
                onSearchQuery = { searchQuery = it },
                onSearchClick = { isSearching = true },
                onCancelSearch = { isSearching = false; searchQuery = "" },
                onBack        = onBack
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && !showNewProject && activeProjectDir == null) {
                ExtendedFloatingActionButton(
                    onClick          = { showNewProject = true },
                    containerColor   = AccentBlue,
                    contentColor     = Color.White,
                    shape            = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New project", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        bottomBar = {
            if (activeProjectDir == null) {
                AppBuilderBottomBar(
                    selectedTab = selectedTab,
                    onTabChange = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgColor)
        ) {
            when (selectedTab) {
                0 -> ProjectsTab(
                    projects = filteredProjects,
                    onRestoreClick = { /* restore */ },
                    onProjectClick = { project ->
                        val existing = File(projectsRoot, project.workspaceName)
                        if (existing.exists() && existing.isDirectory) {
                            activeProjectDir = existing
                            activeProjectName = project.appName
                        } else {
                            // Generate starter files on disk so it can be browsed in explorer
                            val config = ProjectConfig(
                                projectName = project.workspaceName,
                                packageName = project.packageName,
                                appName = project.appName
                            )
                            val gen = projectManager.generateProject(config, projectsRoot, allowOverwrite = false)
                            when (gen) {
                                is GenerationResult.Success -> {
                                    activeProjectDir = gen.projectDir
                                    activeProjectName = project.appName
                                }
                                is GenerationResult.DuplicateProject -> {
                                    activeProjectDir = gen.existingDir
                                    activeProjectName = project.appName
                                }
                                else -> {}
                            }
                        }
                    },
                    onNewProject   = { showNewProject = true }
                )
                1 -> StoreTab()
            }

            // Real Project File Explorer overlay
            activeProjectDir?.let { dir ->
                ProjectExplorerScreen(
                    projectDir = dir,
                    projectName = activeProjectName ?: dir.name,
                    onBack = { activeProjectDir = null }
                )
            }

            // New Project overlay screen
            if (showNewProject) {
                NewProjectScreen(
                    onBack   = { showNewProject = false },
                    onProjectCreated = { createdProject: AppProject, dir: File ->
                        projects = listOf(createdProject) + projects.filter { it.workspaceName != createdProject.workspaceName }
                        showNewProject = false
                        activeProjectDir = dir
                        activeProjectName = createdProject.appName
                    }
                )
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBuilderTopBar(
    isSearching:    Boolean,
    searchQuery:    String,
    onSearchQuery:  (String) -> Unit,
    onSearchClick:  () -> Unit,
    onCancelSearch: () -> Unit,
    onBack:         () -> Unit
) {
    Surface(
        color         = BgColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu / back icon
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }

                if (isSearching) {
                    // Inline search field
                    Surface(
                        shape  = RoundedCornerShape(12.dp),
                        color  = SurfaceColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value         = searchQuery,
                                onValueChange = onSearchQuery,
                                modifier      = Modifier.weight(1f),
                                singleLine    = true,
                                textStyle     = LocalTextStyle.current.copy(color = PrimaryText, fontSize = 15.sp),
                                cursorBrush   = SolidColor(AccentBlue),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search for projects...", color = SecondaryText, fontSize = 15.sp)
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onCancelSearch) {
                        Text("Cancel", color = AccentBlue, fontWeight = FontWeight.Medium)
                    }
                } else {
                    // Search bar (tappable, shows hint)
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = SurfaceColor,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onSearchClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Menu, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search for projects...", color = SecondaryText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Search, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}

// ─── Projects Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ProjectsTab(
    projects:       List<AppProject>,
    onRestoreClick: () -> Unit,
    onProjectClick: (AppProject) -> Unit,
    onNewProject:   () -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Restore Projects card
        item {
            RestoreProjectsCard(onClick = onRestoreClick)
            Spacer(Modifier.height(12.dp))
        }

        // Section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Projects",
                    color      = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    modifier   = Modifier.weight(1f)
                )
                // A-Z sort button
                IconButton(onClick = { /* sort */ }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = SecondaryText)
                }
            }
        }

        // Project items
        itemsIndexed(projects, key = { _, p -> p.id }) { index, project ->
            ProjectItem(
                project  = project,
                isFirst  = index == 0,
                isLast   = index == projects.lastIndex,
                onClick  = { onProjectClick(project) }
            )
        }

        if (projects.isEmpty()) {
            item {
                Box(
                    modifier          = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment  = Alignment.Center
                ) {
                    Text("No projects found", color = SecondaryText, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─── Restore Projects Card ────────────────────────────────────────────────────

@Composable
private fun RestoreProjectsCard(onClick: () -> Unit) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = CardBg,
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History icon in a card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceColor
            ) {
                Box(
                    modifier         = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector      = Icons.Default.History,
                        contentDescription = null,
                        tint             = AccentBlue,
                        modifier         = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Restore Projects",
                    color      = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = "Get your projects in a couple of clicks",
                    color    = SecondaryText,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector      = Icons.Default.ChevronRight,
                contentDescription = null,
                tint             = SecondaryText,
                modifier         = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Project Item ─────────────────────────────────────────────────────────────

@Composable
private fun ProjectItem(
    project: AppProject,
    isFirst: Boolean,
    isLast:  Boolean,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue  = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label        = "chevron"
    )

    // shape: round top corners for first, round bottom for last
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(16.dp)
        isFirst           -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        isLast            -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else              -> RoundedCornerShape(0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            shape  = shape,
            color  = CardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon
                    AndroidAppIcon(id = project.id)

                    Spacer(Modifier.width(16.dp))

                    // App info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = project.appName,
                            color      = PrimaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text     = "${project.workspaceName} - ${project.versionName} (${project.versionCode})",
                            color    = SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = project.packageName,
                            color    = SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Expand/collapse chevron
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector      = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint             = SecondaryText,
                            modifier         = Modifier.rotate(chevronRotation)
                        )
                    }
                }

                // Expanded options panel
                AnimatedVisibility(
                    visible = expanded,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column {
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        ProjectExpandedOptions(project = project)
                    }
                }
            }
        }

        // Separator between items (except last)
        if (!isLast) {
            HorizontalDivider(
                color = DividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 80.dp)
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Android App Icon ─────────────────────────────────────────────────────────

@Composable
private fun AndroidAppIcon(id: String) {
    Box(contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = Color(0xFF1A3A2A),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Android robot icon (using Build icon as proxy)
                Icon(
                    imageVector      = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint             = AndroidGreen,
                    modifier         = Modifier.size(38.dp)
                )
            }
        }
        // ID badge at the bottom
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(text = id, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Expanded Options ─────────────────────────────────────────────────────────

@Composable
private fun ProjectExpandedOptions(project: AppProject) {
    val options = listOf(
        Icons.Default.Settings    to "Settings",
        Icons.Default.SaveAlt     to "Backup",
        Icons.Default.Bookmark    to "Pin",
        Icons.Default.Share       to "Export / Sign",
        Icons.Default.Build       to "Config",
        Icons.Default.Delete      to "Delete"
    )

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (icon, label) ->
            OptionChip(icon = icon, label = label)
        }
    }
}

@Composable
private fun OptionChip(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector      = icon,
            contentDescription = label,
            tint             = if (label == "Delete") MaterialTheme.colorScheme.error else AccentBlue,
            modifier         = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = label,
            color    = if (label == "Delete") MaterialTheme.colorScheme.error else SecondaryText,
            fontSize = 10.sp
        )
    }
}

// ─── Store Tab ────────────────────────────────────────────────────────────────

@Composable
private fun StoreTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector      = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint             = SecondaryText,
                modifier         = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Store", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("Coming Soon", color = SecondaryText, fontSize = 14.sp)
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
private fun AppBuilderBottomBar(
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    Surface(
        color          = BgColor,
        tonalElevation  = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            NavigationBar(
                containerColor  = BgColor,
                tonalElevation  = 0.dp,
                modifier        = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { onTabChange(0) },
                    icon     = {
                        Icon(Icons.Default.Apps, contentDescription = "Projects")
                    },
                    label    = { Text("Projects") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = AccentBlue,
                        selectedTextColor   = AccentBlue,
                        unselectedIconColor = SecondaryText,
                        unselectedTextColor = SecondaryText,
                        indicatorColor      = AccentBlue.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { onTabChange(1) },
                    icon     = {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Store")
                    },
                    label    = { Text("Store") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = AccentBlue,
                        selectedTextColor   = AccentBlue,
                        unselectedIconColor = SecondaryText,
                        unselectedTextColor = SecondaryText,
                        indicatorColor      = AccentBlue.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NEW PROJECT SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Theme preset data ────────────────────────────────────────────────────────

data class ThemePreset(
    val name: String,
    val primary:     Color,
    val primaryDark: Color,
    val accent:      Color
)

private val themePresets = listOf(
    ThemePreset("Material Purple", Color(0xFF6200EE), Color(0xFF3700B3), Color(0xFF03DAC5)),
    ThemePreset("Material Blue",   Color(0xFF1976D2), Color(0xFF0D47A1), Color(0xFF03DAC5)),
    ThemePreset("Material Green",  Color(0xFF388E3C), Color(0xFF1B5E20), Color(0xFF03DAC5)),
    ThemePreset("Material Red",    Color(0xFFD32F2F), Color(0xFFB71C1C), Color(0xFF03DAC5)),
    ThemePreset("Material Orange", Color(0xFFF57C00), Color(0xFFE65100), Color(0xFF03DAC5)),
    ThemePreset("Material Teal",   Color(0xFF00796B), Color(0xFF004D40), Color(0xFF03DAC5)),
    ThemePreset("Material Indigo", Color(0xFF3F51B5), Color(0xFF1A237E), Color(0xFF03DAC5)),
    ThemePreset("Material Pink",   Color(0xFFC2185B), Color(0xFF880E4F), Color(0xFF03DAC5)),
    ThemePreset("Dark Purple",     Color(0xFFBB86FC), Color(0xFF3700B3), Color(0xFF03DAC5)),
    ThemePreset("Dark Blue",       Color(0xFF64B5F6), Color(0xFF1976D2), Color(0xFF03DAC5)),
)

private val codeLanguages = listOf(
    "Java", "Kotlin", "Python", "JavaScript", "TypeScript",
    "C++", "C#", "Swift", "Rust", "Go", "Dart (Flutter)"
)

private val uiTypes = listOf("XML Views", "Jetpack Compose")

// ─── New Project Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    onProjectCreated: (AppProject, File) -> Unit
) {
    val context = LocalContext.current
    val projectManager = remember { ProjectManager(TemplateManager(context)) }
    val projectsRoot = remember { projectManager.getProjectsRoot(context) }

    // State
    var appName       by remember { mutableStateOf("") }
    var packageName   by remember { mutableStateOf("com.my.newproject") }
    var projectName   by remember { mutableStateOf("NewProject") }
    var activityName  by remember { mutableStateOf("MainActivity") }
    var versionCode   by remember { mutableStateOf("1") }
    var versionName   by remember { mutableStateOf("1.0") }
    var selectedLang  by remember { mutableStateOf("Kotlin") }
    var selectedUiType by remember { mutableStateOf("XML Views") }
    var langExpanded  by remember { mutableStateOf(false) }
    var uiExpanded    by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf<ThemePreset?>(null) }
    var errorMessage  by remember { mutableStateOf<String?>(null) }
    var duplicateProjectDir by remember { mutableStateOf<File?>(null) }

    // Current colors
    var colorAccent      by remember { mutableStateOf(Color(0xFF03DAC5)) }
    var colorPrimary     by remember { mutableStateOf(Color(0xFF6200EE)) }
    var colorPrimaryDark by remember { mutableStateOf(Color(0xFF3700B3)) }
    var colorControlH    by remember { mutableStateOf(Color(0xFFE8EAF6)) }
    var colorControlN    by remember { mutableStateOf(Color(0xFFBDBDBD)) }

    fun executeCreate(allowOverwrite: Boolean = false) {
        val langKey = if (selectedLang.equals("Kotlin", ignoreCase = true)) "kotlin" else "java"
        val uiKey = if (selectedUiType.contains("Compose", ignoreCase = true)) "compose" else "xml"

        val config = ProjectConfig(
            projectName = projectName.trim(),
            packageName = packageName.trim(),
            language = langKey,
            uiType = uiKey,
            activityName = activityName.trim().ifBlank { "MainActivity" },
            appName = appName.trim().ifBlank { projectName.trim() },
            versionName = versionName.trim(),
            versionCode = versionCode.toIntOrNull() ?: 1
        )

        when (val result = projectManager.generateProject(config, projectsRoot, allowOverwrite)) {
            is GenerationResult.Success -> {
                val newProject = AppProject(
                    id = (Math.abs(config.projectName.hashCode()) % 1000).toString(),
                    appName = config.appName,
                    workspaceName = config.projectName,
                    versionName = config.versionName,
                    versionCode = config.versionCode.toString(),
                    packageName = config.packageName
                )
                onProjectCreated(newProject, result.projectDir)
            }
            is GenerationResult.DuplicateProject -> {
                duplicateProjectDir = result.existingDir
            }
            is GenerationResult.Failure -> {
                errorMessage = result.message
            }
        }
    }

    // Full-screen dark overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Surface(color = BgColor, shadowElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                    Text(
                        text       = "New Project",
                        color      = PrimaryText,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }

            // ── Scrollable Content ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Error Banner
                errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = msg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // App Icon
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape  = CircleShape,
                        color  = Color(0xFFF2F2F7),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector      = Icons.Default.PhoneAndroid,
                                contentDescription = "App Icon",
                                tint             = AndroidGreen,
                                modifier         = Modifier.size(56.dp)
                            )
                        }
                    }
                    Surface(
                        shape  = CircleShape,
                        color  = AccentBlue,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap to create new Icon",
                    color    = SecondaryText,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── App Name ─────────────────────────────────────────────────
                NewProjectTextField(
                    value         = appName,
                    onValueChange = { appName = it },
                    label         = "Enter application name",
                    icon          = Icons.Default.PhoneAndroid,
                    keyboardType  = KeyboardType.Text
                )

                Spacer(Modifier.height(12.dp))

                // ── Package Name ──────────────────────────────────────────────
                NewProjectOutlinedField(
                    value         = packageName,
                    onValueChange = {
                        packageName = it
                        errorMessage = null
                    },
                    label         = "Package name",
                    icon          = Icons.Default.Label,
                    keyboardType  = KeyboardType.Ascii
                )

                Spacer(Modifier.height(12.dp))

                // ── Project Name ──────────────────────────────────────────────
                NewProjectOutlinedField(
                    value         = projectName,
                    onValueChange = {
                        projectName = it
                        errorMessage = null
                    },
                    label         = "Project name",
                    icon          = Icons.Default.FolderOpen,
                    keyboardType  = KeyboardType.Text
                )

                Spacer(Modifier.height(12.dp))

                // ── Activity Name ─────────────────────────────────────────────
                NewProjectOutlinedField(
                    value         = activityName,
                    onValueChange = {
                        activityName = it
                        errorMessage = null
                    },
                    label         = "Activity name",
                    icon          = Icons.Default.Terminal,
                    keyboardType  = KeyboardType.Text
                )

                Spacer(Modifier.height(16.dp))

                // ── Code Language Dropdown ────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded  = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value         = selectedLang,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Code Language", color = AccentBlue, fontSize = 12.sp) },
                        leadingIcon   = {
                            Icon(Icons.Default.Code, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded)
                        },
                        colors        = newProjectFieldColors(),
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded         = langExpanded,
                        onDismissRequest = { langExpanded = false },
                        modifier         = Modifier.background(SurfaceColor)
                    ) {
                        codeLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text    = { Text(lang, color = PrimaryText) },
                                onClick = {
                                    selectedLang = lang
                                    langExpanded = false
                                    if (lang.equals("Java", ignoreCase = true) && selectedUiType.contains("Compose", ignoreCase = true)) {
                                        selectedUiType = "XML Views"
                                    }
                                },
                                leadingIcon = {
                                    if (lang == selectedLang)
                                        Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── UI Framework Dropdown ─────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded  = uiExpanded,
                    onExpandedChange = { uiExpanded = !uiExpanded }
                ) {
                    OutlinedTextField(
                        value         = selectedUiType,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("UI Type", color = AccentBlue, fontSize = 12.sp) },
                        leadingIcon   = {
                            Icon(Icons.Default.Layers, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiExpanded)
                        },
                        colors        = newProjectFieldColors(),
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded         = uiExpanded,
                        onDismissRequest = { uiExpanded = false },
                        modifier         = Modifier.background(SurfaceColor)
                    ) {
                        uiTypes.forEach { ui ->
                            val isUnsupportedJavaCompose = selectedLang.equals("Java", ignoreCase = true) && ui.contains("Compose", ignoreCase = true)
                            DropdownMenuItem(
                                text    = {
                                    Column {
                                        Text(
                                            ui,
                                            color = if (isUnsupportedJavaCompose) SecondaryText.copy(alpha = 0.5f) else PrimaryText
                                        )
                                        if (isUnsupportedJavaCompose) {
                                            Text("Requires Kotlin", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    if (!isUnsupportedJavaCompose) {
                                        selectedUiType = ui
                                        uiExpanded = false
                                    }
                                },
                                leadingIcon = {
                                    if (ui == selectedUiType)
                                        Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                if (selectedLang.equals("Java", ignoreCase = true) && selectedUiType.contains("Compose", ignoreCase = true)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Jetpack Compose is supported with Kotlin. Please select Kotlin.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Color Pickers Row ─────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    listOf(
                        "colorAccent"      to colorAccent,
                        "colorPrimary"     to colorPrimary,
                        "colorPrimaryDark" to colorPrimaryDark,
                        "colorCon..."      to colorControlH
                    ).forEach { (label, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable { /* color picker */ }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = SecondaryText, fontSize = 9.sp)
                        }
                    }
                    // Help icon
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Help, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Theme Presets Card ────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBg
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                null,
                                tint     = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Theme Presets",
                                    color      = PrimaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 15.sp
                                )
                            }
                            // Generate random
                            TextButton(onClick = { /* random */ }) {
                                Text("Generate Random", color = AccentBlue, fontSize = 12.sp)
                            }
                            // Reset
                            IconButton(onClick = { selectedTheme = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Choose from predefined themes or generate a random one",
                            color    = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(themePresets) { preset ->
                                ThemePresetChip(
                                    preset   = preset,
                                    selected = selectedTheme == preset,
                                    onClick  = {
                                        selectedTheme    = preset
                                        colorPrimary     = preset.primary
                                        colorPrimaryDark = preset.primaryDark
                                        colorAccent      = preset.accent
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Version Code & Name ───────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Version code
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = CardBg,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* version picker */ }
                    ) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                versionCode,
                                color      = PrimaryText,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Version code", color = SecondaryText, fontSize = 12.sp)
                        }
                    }

                    // Swap icon
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, tint = SecondaryText, modifier = Modifier.size(22.dp))
                    }

                    // Version name
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = CardBg,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* version picker */ }
                    ) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                versionName,
                                color      = PrimaryText,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Version name", color = SecondaryText, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Bottom Buttons ────────────────────────────────────────────────
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                OutlinedButton(
                    onClick = onBack,
                    shape   = RoundedCornerShape(14.dp),
                    border  = BorderStroke(1.dp, SecondaryText.copy(alpha = 0.4f)),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                // Create
                Button(
                    onClick = { executeCreate(allowOverwrite = false) },
                    shape   = RoundedCornerShape(14.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Create", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // Duplicate Project Confirmation Dialog
        duplicateProjectDir?.let { existingDir ->
            AlertDialog(
                onDismissRequest = { duplicateProjectDir = null },
                title = { Text("Project already exists", color = PrimaryText, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "A project named '$projectName' already exists on disk.\n\nChoose an option below:",
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val dir = duplicateProjectDir!!
                        duplicateProjectDir = null
                        val existingProject = AppProject(
                            id = (Math.abs(projectName.hashCode()) % 1000).toString(),
                            appName = appName.ifBlank { projectName },
                            workspaceName = projectName,
                            versionName = versionName,
                            versionCode = versionCode,
                            packageName = packageName
                        )
                        onProjectCreated(existingProject, dir)
                    }) {
                        Text("Open Existing", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            duplicateProjectDir = null
                        }) {
                            Text("Change Name", color = AccentBlue)
                        }
                        TextButton(onClick = { duplicateProjectDir = null }) {
                            Text("Cancel", color = SecondaryText)
                        }
                    }
                },
                containerColor = SurfaceColor
            )
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun newProjectFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentBlue,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor    = AccentBlue,
    unfocusedLabelColor  = SecondaryText,
    cursorColor          = AccentBlue,
    focusedTextColor     = PrimaryText,
    unfocusedTextColor   = PrimaryText
)

@Composable
private fun NewProjectTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    icon:          ImageVector,
    keyboardType:  KeyboardType = KeyboardType.Text
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier.weight(1f).padding(vertical = 14.dp),
                singleLine    = true,
                textStyle     = LocalTextStyle.current.copy(color = PrimaryText, fontSize = 15.sp),
                cursorBrush   = SolidColor(AccentBlue),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, color = SecondaryText, fontSize = 15.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun NewProjectOutlinedField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    icon:          ImageVector,
    keyboardType:  KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 12.sp) },
        leadingIcon   = { Icon(icon, null, tint = SecondaryText, modifier = Modifier.size(20.dp)) },
        singleLine    = true,
        colors        = newProjectFieldColors(),
        shape         = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier      = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ThemePresetChip(
    preset:   ThemePreset,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable(onClick = onClick)
    ) {
        // Color preview strips
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = Color.Transparent,
            border   = if (selected) BorderStroke(2.dp, AccentBlue) else BorderStroke(1.dp, BorderColor),
            modifier = Modifier.size(width = 70.dp, height = 44.dp)
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(preset.primary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(preset.primaryDark)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text     = preset.name,
            color    = if (selected) AccentBlue else SecondaryText,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

