package com.prk.kirinmusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prk.kirinmusic.ui.SettingsScreen
import com.prk.kirinmusic.ui.screens.MainScreen
import com.prk.kirinmusic.ui.theme.MusicPlayerTheme
import com.prk.kirinmusic.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: MainViewModel = viewModel()

            MusicPlayerTheme(viewModel = viewModel) {
                val context = LocalContext.current

                val storagePermission = if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionsToRequest = remember {
                    mutableListOf(storagePermission).apply {
                        if (Build.VERSION.SDK_INT >= 33) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                }

                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val isStorageGranted = permissions[storagePermission] ?: false

                    if (isStorageGranted) {
                        viewModel.loadLocalMusic()
                    }
                }

                LaunchedEffect(Unit) {
                    val isStorageGranted = ContextCompat.checkSelfPermission(
                        context,
                        storagePermission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (isStorageGranted) {
                        viewModel.loadLocalMusic()
                    } else {
                        multiplePermissionsLauncher.launch(permissionsToRequest)
                    }
                }

                val window = (LocalContext.current as? android.app.Activity)?.window
                val view = LocalView.current
                val isDark = isSystemInDarkTheme()

                SideEffect {
                    window?.let { win ->
                        win.statusBarColor = Color.Transparent.toArgb()
                        win.navigationBarColor = Color.Transparent.toArgb()

                        val insets = WindowCompat.getInsetsController(win, view)
                        insets.isAppearanceLightStatusBars = !isDark
                        insets.isAppearanceLightNavigationBars = !isDark
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("home") }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == "settings") {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "ScreenTransition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                "home" -> MainScreen(
                    viewModel = viewModel
                )

                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = "home" }
                )
            }
        }
    }
}
