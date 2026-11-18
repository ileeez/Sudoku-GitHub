package com.sudokucat.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sudokucat.app.AppContainer
import com.sudokucat.app.data.prefs.UserProfile
import com.sudokucat.app.data.prefs.UserSettings
import com.sudokucat.app.domain.model.Difficulty
import com.sudokucat.app.domain.model.GameMode
import com.sudokucat.app.ui.ai.AiHubScreen
import com.sudokucat.app.ui.components.CatAvatar
import com.sudokucat.app.ui.components.CatChatDialog
import com.sudokucat.app.ui.components.CatSpeechBubble
import com.sudokucat.app.ui.components.Pill
import com.sudokucat.app.ui.game.GameScreen
import com.sudokucat.app.ui.home.HomeScreen
import com.sudokucat.app.ui.learn.LearnScreen
import com.sudokucat.app.ui.learn.LessonScreen
import com.sudokucat.app.ui.photo.PhotoScreen
import com.sudokucat.app.ui.play.PlayScreen
import com.sudokucat.app.ui.settings.SettingsScreen
import com.sudokucat.app.ui.stats.StatsScreen
import com.sudokucat.app.ui.theme.CatTheme
import com.sudokucat.app.util.CatExpression
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val PLAY = "play"
    const val AI = "ai"
    const val PHOTO = "photo"
    const val LEARN = "learn"
    const val LESSON = "lesson/{lessonId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val GAME = "game/{gameId}"

    fun lesson(id: String) = "lesson/$id"
    fun game(id: Long) = "game/$id"
}

private data class DrawerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val drawerDestinations = listOf(
    DrawerDestination(Routes.HOME, "Home", Icons.Rounded.Home),
    DrawerDestination(Routes.PLAY, "Gioca", Icons.Rounded.PlayArrow),
    DrawerDestination(Routes.AI, "Gioca con AI", Icons.Rounded.SmartToy),
    DrawerDestination(Routes.PHOTO, "Risolvi da foto", Icons.Rounded.PhotoCamera),
    DrawerDestination(Routes.LEARN, "Impara Sudoku", Icons.Rounded.School),
    DrawerDestination(Routes.STATS, "Statistiche", Icons.Rounded.BarChart),
    DrawerDestination(Routes.SETTINGS, "Impostazioni", Icons.Rounded.Settings)
)

@Composable
fun SudokuCatRoot(container: AppContainer) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = backStackEntry?.destination
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = UserSettings())
    val profile by container.profileRepository.profile
        .collectAsStateWithLifecycle(initialValue = UserProfile())
    val catState by container.catController.state.collectAsStateWithLifecycle()

    var chatOpen by rememberSaveable { mutableStateOf(false) }
    var chatBoard by remember { mutableStateOf<IntArray?>(null) }
    var greeted by rememberSaveable { mutableStateOf(false) }

    val isGame = currentRoute?.startsWith("game/") == true

    LaunchedEffect(Unit) {
        if (!greeted) {
            greeted = true
            container.catController.greet()
        }
    }

    LaunchedEffect(currentRoute) {
        if (drawerState.isOpen) drawerState.close()
    }

    fun openDrawer() = scope.launch { drawerState.open() }

    fun openChat(board: IntArray?) {
        chatBoard = board
        chatOpen = true
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            // Drawer destinations are true top-level screens: always return to a clean
            // stack so Home cannot restore an old game/play history on top of itself.
            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
            restoreState = false
        }
        scope.launch { drawerState.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isGame,
            drawerContent = {
                DrawerContent(
                    profile = profile,
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTopLevel
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) },
                        onPlay = { navController.navigate(Routes.PLAY) },
                        onGuided = { navController.navigate(Routes.AI) },
                        onPhoto = { navController.navigate(Routes.PHOTO) },
                        onLearn = { navController.navigate(Routes.LEARN) },
                        onStats = { navController.navigate(Routes.STATS) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onResume = { id -> navController.navigate(Routes.game(id)) }
                    )
                }

                composable(Routes.PLAY) {
                    PlayScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) },
                        onStartGame = { id ->
                            navController.navigate(Routes.game(id)) { launchSingleTop = true }
                        }
                    )
                }

                composable(Routes.AI) {
                    AiHubScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) },
                        onStartGame = { id ->
                            navController.navigate(Routes.game(id)) { launchSingleTop = true }
                        },
                        onSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }

                composable(Routes.PHOTO) {
                    PhotoScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) }
                    )
                }

                composable(Routes.LEARN) {
                    LearnScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) },
                        onOpenLesson = { id -> navController.navigate(Routes.lesson(id)) }
                    )
                }

                composable(
                    route = Routes.LESSON,
                    arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                ) { entry ->
                    val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                    LessonScreen(
                        container = container,
                        lessonId = lessonId,
                        onBack = { navController.popBackStack() },
                        onNextLesson = { nextId ->
                            navController.navigate(Routes.lesson(nextId)) {
                                popUpTo(Routes.LESSON) { inclusive = true }
                            }
                        },
                        onOpenCat = { openChat(null) }
                    )
                }

                composable(Routes.STATS) {
                    StatsScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        container = container,
                        onMenu = ::openDrawer,
                        onOpenCat = { openChat(null) }
                    )
                }

                composable(
                    route = Routes.GAME,
                    arguments = listOf(navArgument("gameId") { type = NavType.LongType })
                ) { entry ->
                    val gameId = entry.arguments?.getLong("gameId") ?: -1L
                    GameScreen(
                        container = container,
                        gameId = gameId,
                        onBack = { navController.popBackStack() },
                        onHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        onNewGame = {
                            scope.launch {
                                val previous = runCatching { container.gameRepository.get(gameId) }.getOrNull()
                                val id = runCatching {
                                    container.gameRepository.createGame(
                                        difficulty = previous?.difficultyEnum ?: Difficulty.MEDIUM,
                                        mode = previous?.modeEnum ?: GameMode.CLASSIC
                                    )
                                }.getOrNull()
                                if (id != null) {
                                    navController.navigate(Routes.game(id)) {
                                        popUpTo(Routes.GAME) { inclusive = true }
                                    }
                                }
                            }
                        },
                        onOpenChat = { board -> openChat(board) }
                    )
                }
            }

            if (!isGame && settings.catEnabled) {
                AnimatedVisibility(
                    visible = catState.bubbleVisible && catState.message != null && !drawerState.isOpen,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    CatSpeechBubble(
                        message = catState.message.orEmpty(),
                        expression = catState.expression,
                        reactionToken = catState.reactionToken,
                        onDismiss = { container.catController.dismissBubble() },
                        onSpeak = { container.catController.speakCurrent() },
                        onOpenChat = { openChat(null) }
                    )
                }
            }
            }
        }
    }

    if (chatOpen) {
        CatChatDialog(
            coachService = container.coachService,
            catController = container.catController,
            onDismiss = { chatOpen = false },
            board = chatBoard
        )
    }
}

@Composable
private fun DrawerContent(
    profile: UserProfile,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 26.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatAvatar(modifier = Modifier.size(64.dp), expression = CatExpression.HAPPY)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Sudoku Cat",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Nero, il tuo compagno di griglia",
                        style = MaterialTheme.typography.labelMedium,
                        color = CatTheme.colors.muted
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Pill(
                text = "Livello ${profile.level} · ${profile.xp} XP",
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))

            drawerDestinations.forEach { destination ->
                val selected = when (destination.route) {
                    Routes.LEARN -> currentRoute == Routes.LEARN || currentRoute?.startsWith("lesson/") == true
                    Routes.PLAY -> currentRoute == Routes.PLAY || currentRoute?.startsWith("game/") == true
                    else -> currentRoute == destination.route
                }
                NavigationDrawerItem(
                    label = { Text(destination.label, style = MaterialTheme.typography.bodyLarge) },
                    selected = selected,
                    onClick = { onNavigate(destination.route) },
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    modifier = Modifier.padding(vertical = 2.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(text = "Serie ${profile.streak} g", color = CatTheme.colors.fish.copy(alpha = 0.2f), contentColor = CatTheme.colors.fishDark)
                Pill(text = "Offline", color = CatTheme.colors.mint.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Tutti i progressi restano sul dispositivo. Le funzioni AI sono opzionali e usano una chiave Gemini personale.",
                style = MaterialTheme.typography.labelSmall,
                color = CatTheme.colors.muted
            )
        }
    }
}
