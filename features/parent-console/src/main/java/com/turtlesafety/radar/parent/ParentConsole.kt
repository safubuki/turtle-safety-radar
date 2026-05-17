package com.turtlesafety.radar.parent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.turtlesafety.radar.parent.screens.ChecklistScreen
import com.turtlesafety.radar.parent.screens.HomeScreen
import com.turtlesafety.radar.parent.screens.LogListScreen
import com.turtlesafety.radar.parent.screens.PinGateScreen
import com.turtlesafety.radar.parent.screens.PinSetupScreen
import com.turtlesafety.radar.parent.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentConsole(modifier: Modifier = Modifier) {
    val vm: ParentConsoleViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.gateStatus) {
        if (state.gateStatus == GateStatus.UNLOCKED) {
            vm.refreshPermissions(context)
        }
    }

    when (state.gateStatus) {
        GateStatus.UNINITIALIZED ->
            PinSetupScreen(error = state.gateError, onSubmit = vm::setupPin)
        GateStatus.LOCKED ->
            PinGateScreen(error = state.gateError, onSubmit = vm::verifyPin)
        GateStatus.UNLOCKED -> AuthenticatedScaffold(state = state, vm = vm, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedScaffold(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Turtle Safety Radar") },
                actions = {
                    IconButton(onClick = { vm.lock() }) {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = "ロック")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.currentScreen == Screen.Home,
                    onClick = { vm.navigate(Screen.Home) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("ホーム") },
                )
                NavigationBarItem(
                    selected = state.currentScreen == Screen.Logs,
                    onClick = { vm.navigate(Screen.Logs) },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    label = { Text("ログ") },
                )
                NavigationBarItem(
                    selected = state.currentScreen == Screen.Checklist,
                    onClick = { vm.navigate(Screen.Checklist) },
                    icon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
                    label = { Text("チェック") },
                )
                NavigationBarItem(
                    selected = state.currentScreen == Screen.Settings,
                    onClick = { vm.navigate(Screen.Settings) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("設定") },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state.currentScreen) {
                Screen.Home -> HomeScreen(state = state, vm = vm)
                Screen.Logs -> LogListScreen(state = state, vm = vm)
                Screen.Settings -> SettingsScreen(state = state, vm = vm)
                Screen.Checklist -> ChecklistScreen(state = state, vm = vm)
            }
        }
    }
}
