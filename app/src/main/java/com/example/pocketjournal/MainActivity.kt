package com.example.pocketjournal

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.pocketjournal.ui.theme.PocketJournalTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EntryType {
    BOOLEAN,
    INT,
    FLOAT,
    STRING,
}

@Entity (tableName = "entries")
data class Entry (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: EntryType,
    val value: String,
    val date: Long,
)

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: Entry)

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries")
    suspend fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date = :date")
    suspend fun getEntriesByDate(date: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE name = :name")
    suspend fun getEntriesByName(name: String): Flow<List<Entry>>

}

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class EntryDatabase : RoomDatabase() {
    abstract fun entry(): EntryDao

    companion object {
        @Volatile
        private var Instance: EntryDatabase? = null

        fun getDatabase(context: Context): EntryDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, EntryDatabase::class.java, "entry_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

class EntryRepository(private val entryDao: EntryDao) {
    suspend fun insert(entry: Entry) {
        entryDao.insert(entry)
    }
    suspend fun update(entry: Entry) {
        entryDao.update(entry)
    }
    suspend fun delete(entry: Entry) {
        entryDao.delete(entry)
    }
    suspend fun getEntries(): Flow<List<Entry>> {
        return entryDao.getAllEntries()
    }
    suspend fun getEntriesByDate(date: Long): Flow<List<Entry>> {
        return entryDao.getEntriesByDate(date)
    }
    suspend fun getEntriesByName(name: String): Flow<List<Entry>> {
        return entryDao.getEntriesByName(name)
    }
}

class EntryViewModelFactory(private val application: Application)
    :ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EntryViewModel(application) as T
    }
}

class EntryViewModel(application: Application) : ViewModel() {
    private val repository: EntryRepository
    private val _entryState = MutableStateFlow<List<Entry>>(emptyList())
    val entryState: StateFlow<List<Entry>>
        get() = _entryState

    init {
        val db = EntryDatabase.getDatabase(application)
        val dao = db.entry()
        repository = EntryRepository(dao)

        fetchEntries()
    }

    private fun fetchEntries() {
        viewModelScope.launch {
            repository.getEntries().collect { entry ->
                _entryState.value = entry
            }
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun addEntry(entry: Entry) {
        viewModelScope.launch {
            repository.insert(entry)
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }
}

sealed class BottomNavScreen(val route: String, val icon: ImageVector, val label: String) {
    data object Home : BottomNavScreen("home", Icons.Default.Home, "Home")
    data object Month : BottomNavScreen("month", Icons.Default.DateRange, "Month")
    data object New: BottomNavScreen("new", Icons.Default.Create, "New")
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
        BottomNavScreen.New,
        BottomNavScreen.Summary,
        BottomNavScreen.Settings
    )

    BottomNavigation (
        modifier = Modifier
            .height(80.dp),
        backgroundColor = Color.Gray
    ){
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        screens.forEach { screen ->
            if (screen.route == BottomNavScreen.New.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(screen.route) },
                    modifier = Modifier
                        .size(120.dp),
                    backgroundColor = Color.Black
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier.padding(14.dp).size(40.dp),
                        tint = Color.White
                    )
                }
            }
            else {
                BottomNavigationItem(
                    selected = currentRoute == screen.route,
                    onClick = { navController.navigate(screen.route) },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxSize()
                        )
                    },
                    //label = { Text(text = screen.label) },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
fun New(navController: NavHostController) {
    Scaffold (
        modifier = Modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "New")
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

@Preview(showBackground = true)
@Composable
fun CutePreview() {
    PocketJournalTheme {
        Navigation()
    }
}