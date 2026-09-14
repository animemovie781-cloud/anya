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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedTab  by remember { mutableStateOf(0) }            // 0=Projects, 1=Store
    var searchQuery  by remember { mutableStateOf("") }
    var isSearching  by remember { mutableStateOf(false) }
    var projects     by remember { mutableStateOf(sampleProjects) }

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
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick          = { /* New project action */ },
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
            AppBuilderBottomBar(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it }
            )
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
                    onProjectClick = { /* open project */ },
                    onNewProject   = { /* new project */ }
                )
                1 -> StoreTab()
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = rememberRipple(),
                                onClick           = onSearchClick
                            )
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = rememberRipple(),
                onClick           = onClick
            )
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = rememberRipple(),
                            onClick           = onClick
                        )
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
