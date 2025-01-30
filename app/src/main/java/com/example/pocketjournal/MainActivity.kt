package com.example.pocketjournal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketjournal.ui.theme.PocketJournalTheme

sealed class BottomNavScreen(val route: String, val icon: ImageVector, val label: String) {
    data object Home : BottomNavScreen("home", Icons.Default.Home, "Home")
    data object Month : BottomNavScreen("month", Icons.Default.DateRange, "Month")
    data object Summary : BottomNavScreen("summary", Icons.AutoMirrored.Filled.List, "Summary")
    data object Settings : BottomNavScreen("settings", Icons.Default.Settings, "Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketJournalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Navigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavGraph(navController = navController, modifier = modifier)
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavScreen.Home.route) { Home(navController = navController) }
        composable(BottomNavScreen.Month.route) { Month(navController = navController) }
        composable(BottomNavScreen.Summary.route) { Summary(navController = navController) }
        composable(BottomNavScreen.Settings.route) { Settings(navController = navController) }
    }
}


@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Month,
        BottomNavScreen.Summary,
        BottomNavScreen.Settings
    )

    BottomNavigation {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        screens.forEach { screen ->
            BottomNavigationItem(
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) },
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                label = { Text(text = screen.label) },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun Home(modifier: Modifier = Modifier, navController: NavHostController) {
    Scaffold (
        modifier = modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "Home")
        }
    }
}

@Composable
fun Month(navController: NavHostController) {
    Scaffold (
        modifier = Modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "Month")
        }
    }
}

@Composable
fun Summary(navController: NavHostController) {
    Scaffold (
        modifier = Modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "Summary")
        }
    }
}

@Composable
fun Settings(navController: NavHostController) {
    Scaffold (
        modifier = Modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "Settings")
        }
    }
}