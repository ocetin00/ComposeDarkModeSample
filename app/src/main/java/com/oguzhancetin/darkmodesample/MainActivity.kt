package com.oguzhancetin.darkmodesample

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.oguzhancetin.darkmodesample.ui.theme.DarkModeSampleTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val viewModel: MainActivityViewModel by viewModels()
        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }

        splashScreen.setKeepOnScreenCondition {
            when (uiState) {
                MainActivityUiState.Loading -> true
                is MainActivityUiState.Success -> false
            }
        }


        setContent {
            val systemUiController = rememberSystemUiController()
            val isDarkMode = shouldUseDarkTheme(uiState)

            // Update the dark content of the system bars to match the theme
            DisposableEffect(isDarkMode) {
                systemUiController.systemBarsDarkContentEnabled = !isDarkMode
                onDispose {}
            }

            DarkModeSampleTheme(darkTheme = isDarkMode) {
                // A surface container using the 'background' color from the theme
                SampleApp({ viewModel.toggleDarkMode() },isDarkMode)
            }
        }
    }
}


class MainActivityViewModel(
    private val context: Application,
) : AndroidViewModel(context) {


    val enableDarkMode = context.dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey("dark_mode")] ?: false
        }

    val uiState: StateFlow<MainActivityUiState> = enableDarkMode.map {
        MainActivityUiState.Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun toggleDarkMode(){
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                val current = preferences[booleanPreferencesKey("dark_mode")] ?: false
                preferences[booleanPreferencesKey("dark_mode")] = !current
            }
        }
    }
}

sealed class MainActivityUiState {
    object Loading : MainActivityUiState()
    data class Success(val isDarkModel: Boolean) : MainActivityUiState()
}

@Composable
private fun shouldUseDarkTheme(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    MainActivityUiState.Loading -> isSystemInDarkTheme()
    is MainActivityUiState.Success -> uiState.isDarkModel
}

@Composable
fun SampleApp(onChangeThemeMode: () -> Unit = {}, isDarkMode:Boolean= false) {
    Column(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
       Switch(checked =isDarkMode , onCheckedChange = { onChangeThemeMode() })
        Text(text = "Dark Mode")
    }


}

