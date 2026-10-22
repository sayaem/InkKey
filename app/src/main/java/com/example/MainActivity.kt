package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontRepository
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.ui.screens.CaptureStudioScreen
import com.example.ui.screens.FontStudioScreen
import com.example.ui.screens.HandwritingStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeyboardSetupScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.InkKeyTheme
import kotlinx.coroutines.launch

enum class AppDestination {
    SPLASH,
    ONBOARDING,
    HOME,
    CAPTURE_STUDIO,
    FONT_STUDIO,
    EXPORT_STUDIO,
    KEYBOARD_SETUP,
    SETTINGS
}

private data class NavItem(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private lateinit var fontRepository: FontRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fontRepository = FontRepository(this)
        preferencesRepository = UserPreferencesRepository(this)

        val initialRouteExtra = intent.getStringExtra("EXTRA_ROUTE")

        setContent {
            val userPrefs by preferencesRepository.userPreferencesFlow.collectAsState(
                initial = com.example.data.UserPreferences()
            )
            val activeProfile by fontRepository.activeProfileFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            val isDark = when (userPrefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }

            // Ensure baseline profile exists
            LaunchedEffect(Unit) {
                fontRepository.ensureDefaultProfile()
            }

            // Handle incoming intent route if any
            LaunchedEffect(initialRouteExtra) {
                if (initialRouteExtra == "EXPORT_STUDIO") {
                    currentDestination = AppDestination.EXPORT_STUDIO
                }
            }

            InkKeyTheme(darkTheme = isDark) {
                val profileId = activeProfile?.id ?: 1L

                // Handle system back button per destination
                BackHandler(enabled = currentDestination != AppDestination.HOME && currentDestination != AppDestination.SPLASH) {
                    when (currentDestination) {
                        AppDestination.CAPTURE_STUDIO,
                        AppDestination.FONT_STUDIO,
                        AppDestination.EXPORT_STUDIO,
                        AppDestination.KEYBOARD_SETUP,
                        AppDestination.SETTINGS -> {
                            currentDestination = AppDestination.HOME
                        }
                        AppDestination.ONBOARDING -> {
                            // Already at onboarding
                        }
                        else -> {
                            currentDestination = AppDestination.HOME
                        }
                    }
                }

                val showBottomNav = currentDestination in listOf(
                    AppDestination.HOME,
                    AppDestination.CAPTURE_STUDIO,
                    AppDestination.FONT_STUDIO,
                    AppDestination.EXPORT_STUDIO,
                    AppDestination.SETTINGS
                )

                val navItems = listOf(
                    NavItem(AppDestination.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
                    NavItem(AppDestination.CAPTURE_STUDIO, "Capture", Icons.Filled.Create, Icons.Outlined.Create, "nav_capture"),
                    NavItem(AppDestination.FONT_STUDIO, "Font Studio", Icons.Filled.FontDownload, Icons.Outlined.FontDownload, "nav_font"),
                    NavItem(AppDestination.EXPORT_STUDIO, "Handwriting", Icons.Filled.Share, Icons.Outlined.Share, "nav_export"),
                    NavItem(AppDestination.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            NavigationBar(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("bottom_nav_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentDestination == item.destination
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentDestination = item.destination },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentDestination) {
                            AppDestination.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentDestination = if (userPrefs.onboardingCompleted) {
                                            AppDestination.HOME
                                        } else {
                                            AppDestination.ONBOARDING
                                        }
                                    }
                                )
                            }

                            AppDestination.ONBOARDING -> {
                                OnboardingScreen(
                                    onComplete = { profileName ->
                                        scope.launch {
                                            fontRepository.createProfile(profileName)
                                            preferencesRepository.setOnboardingCompleted(true)
                                            currentDestination = AppDestination.CAPTURE_STUDIO
                                        }
                                    }
                                )
                            }

                            AppDestination.HOME -> {
                                HomeScreen(
                                    fontRepository = fontRepository,
                                    onNavigateToCapture = { currentDestination = AppDestination.CAPTURE_STUDIO },
                                    onNavigateToFontStudio = { currentDestination = AppDestination.FONT_STUDIO },
                                    onNavigateToHandwritingStudio = { currentDestination = AppDestination.EXPORT_STUDIO },
                                    onNavigateToKeyboardSetup = { currentDestination = AppDestination.KEYBOARD_SETUP },
                                    onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
                                )
                            }

                            AppDestination.CAPTURE_STUDIO -> {
                                CaptureStudioScreen(
                                    profileId = profileId,
                                    fontRepository = fontRepository,
                                    onNavigateBack = { currentDestination = AppDestination.HOME },
                                    onNavigateToFontStudio = { currentDestination = AppDestination.FONT_STUDIO }
                                )
                            }

                            AppDestination.FONT_STUDIO -> {
                                FontStudioScreen(
                                    profileId = profileId,
                                    fontRepository = fontRepository,
                                    preferencesRepository = preferencesRepository,
                                    onNavigateBack = { currentDestination = AppDestination.HOME },
                                    onNavigateToKeyboardSetup = { currentDestination = AppDestination.KEYBOARD_SETUP }
                                )
                            }

                            AppDestination.EXPORT_STUDIO -> {
                                HandwritingStudioScreen(
                                    profileId = profileId,
                                    fontRepository = fontRepository,
                                    onNavigateBack = { currentDestination = AppDestination.HOME }
                                )
                            }

                            AppDestination.KEYBOARD_SETUP -> {
                                KeyboardSetupScreen(
                                    onNavigateBack = { currentDestination = AppDestination.HOME }
                                )
                            }

                            AppDestination.SETTINGS -> {
                                SettingsScreen(
                                    fontRepository = fontRepository,
                                    preferencesRepository = preferencesRepository,
                                    onNavigateBack = { currentDestination = AppDestination.HOME },
                                    onReplayOnboarding = {
                                        currentDestination = AppDestination.ONBOARDING
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
