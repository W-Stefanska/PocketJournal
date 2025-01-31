package com.example.pocketjournal

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.util.Calendar

enum class EntryType {
    BOOLEAN,
    INT,
    FLOAT,
    STRING,
    NONE,
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

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM entries")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :startOfDay AND date < :endOfDay")
    fun getEntriesByDate(startOfDay: Long, endOfDay: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE name = :name")
    fun getEntriesByName(name: String): Flow<List<Entry>>

    @Query("SELECT COUNT(*) FROM entries WHERE name LIKE :word")
    fun checkCatExists(word: String): Int

    @Query("SELECT * FROM entries WHERE id = :id")
    fun getEntryById(id: Int): Flow<Entry>

    @Query("SELECT * FROM entries WHERE date >= :startOfDay AND date < :endOfDay")
    fun getEntriesForToday(startOfDay: Long, endOfDay: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY name ASC")
    fun getEntriesByNameAsc(): Flow<List<Entry>>
    
    @Query("SELECT * FROM entries ORDER BY name DESC")
    fun getEntriesByNameDesc(): Flow<List<Entry>>
    
    @Query("SELECT * FROM entries ORDER BY type ASC")
    fun getEntriesByTypeAsc(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :startOfDay AND date < :endOfDay ORDER BY name ASC")
    fun getEntriesForTodayByNameAsc(startOfDay: Long, endOfDay: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :startOfDay AND date < :endOfDay ORDER BY name DESC")
    fun getEntriesForTodayByNameDesc(startOfDay: Long, endOfDay: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :startOfDay AND date < :endOfDay ORDER BY type ASC")
    fun getEntriesForTodayByTypeAsc(startOfDay: Long, endOfDay: Long): Flow<List<Entry>>
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
    suspend fun deleteById(id: Int) {
        entryDao.deleteById(id)
    }
    fun getEntries(): Flow<List<Entry>> {
        return entryDao.getAllEntries()
    }
    fun getEntriesByDate(date: Long): Flow<List<Entry>> {
        val startOfDay = getStartOfDay(date)
        val endOfDay = getEndOfDay(date)
        return entryDao.getEntriesByDate(startOfDay, endOfDay)
    }
    fun getEntriesByName(name: String): Flow<List<Entry>> {
        return entryDao.getEntriesByName(name)
    }
    fun checkCatExists(word: String): Int {
        return entryDao.checkCatExists(word)
    }
    fun getEntryById(id: Int): Flow<Entry> {
        return entryDao.getEntryById(id)
    }
    fun getEntriesByNameAsc(): Flow<List<Entry>> {
        return entryDao.getEntriesByNameAsc()
    }
    fun getEntriesByNameDesc(): Flow<List<Entry>> {
        return entryDao.getEntriesByNameDesc()
    }
    fun getEntriesByTypeAsc(): Flow<List<Entry>> {
        return entryDao.getEntriesByTypeAsc()
    }
    fun getEntriesForTodayAsc(): Flow<List<Entry>> {
        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        return entryDao.getEntriesForTodayByNameAsc(startOfDay, endOfDay)
    }
    fun getEntriesForTodayDesc(): Flow<List<Entry>> {
        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        return entryDao.getEntriesForTodayByNameDesc(startOfDay, endOfDay)
    }
    fun getEntriesForTodayTypeAsc(): Flow<List<Entry>> {
        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        return entryDao.getEntriesForTodayByTypeAsc(startOfDay, endOfDay)
    }
    fun getEntriesForToday(): Flow<List<Entry>> {
        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        return entryDao.getEntriesForToday(startOfDay, endOfDay)
    }
    private fun getStartOfDay(time: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
}

class EntryViewModelFactory(private val application: Application)
    :ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
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

    fun deleteById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
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

    fun checkCatExists(word: String): Int {
        return repository.checkCatExists(word)
    }

    fun getEntryById(id: Int): Flow<Entry> {
        return repository.getEntryById(id)
    }

    fun getEntriesForToday(): Flow<List<Entry>> {
        return repository.getEntriesForToday()
    }

    fun getEntriesByDate(date: Long): Flow<List<Entry>> {
        return repository.getEntriesByDate(date)
    }

    fun getEntriesByNameAsc(): Flow<List<Entry>> {
        return repository.getEntriesByNameAsc()
    }

    fun getEntriesByNameDesc(): Flow<List<Entry>> {
        return repository.getEntriesByNameDesc()
    }

    fun getEntriesByTypeAsc(): Flow<List<Entry>> {
        return repository.getEntriesByTypeAsc()
    }

    fun getEntriesForTodayAsc(): Flow<List<Entry>> {
        return repository.getEntriesForTodayAsc()
    }

    fun getEntriesForTodayDesc(): Flow<List<Entry>> {
        return repository.getEntriesForTodayDesc()
    }

    fun getEntriesForTodayTypeAsc(): Flow<List<Entry>> {
        return repository.getEntriesForTodayTypeAsc()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navController: NavHostController? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (navController != null) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions
    )
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

    BottomAppBar (
        modifier = Modifier
            .height(80.dp),
        containerColor = Color.Gray
    ){
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        screens.forEach { screen ->
            if (screen.route == BottomNavScreen.New.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(screen.route) },
                    modifier = Modifier
                        .size(120.dp),
                    containerColor = Color.Black
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(40.dp),
                        tint = Color.White
                    )
                }
            } else {
                NavigationBarItem(
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
fun NavGraph(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavScreen.Home.route) { Home(navController = navController) }
        composable(BottomNavScreen.Month.route) { Month(navController = navController) }
        composable(BottomNavScreen.New.route) { New(navController = navController, -1) }
        composable(BottomNavScreen.Summary.route) { Summary(navController = navController) }
        composable(BottomNavScreen.Settings.route) { Settings(navController = navController) }
        composable("New/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: -1
            New(navController = navController, id = id)
        }
    }
}

enum class SortType {
    NONE,
    A_Z,
    Z_A,
    CATEGORY
}

@Composable
fun Home(modifier: Modifier = Modifier, navController: NavHostController) {
    val viewModel: EntryViewModel = viewModel(
        LocalViewModelStoreOwner.current!!,
        "EntryViewModel",
        EntryViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    var entriesForTodayState by remember { mutableStateOf(emptyList<Entry>()) }
    val entriesForToday = entriesForTodayState
    var expanded by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.NONE) }

    LaunchedEffect(sortType) {
        when (sortType) {
            SortType.A_Z -> {
                viewModel.getEntriesForTodayAsc().collect {
                    entriesForTodayState = it
                }
            }
            SortType.Z_A -> {
                viewModel.getEntriesForTodayDesc().collect {
                    entriesForTodayState = it
                }
            }
            SortType.CATEGORY -> {
                viewModel.getEntriesForTodayTypeAsc().collect {
                    entriesForTodayState = it
                }
            }
            SortType.NONE -> {
                viewModel.getEntriesForToday().collect {
                    entriesForTodayState = it
                }
            }
        }
    }

    Scaffold (
        topBar = {
            TopBar(
                title ="Home",
                navController = navController,
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sort"
                        )
                    }
                }
            )
        },
        modifier = modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    sortType = SortType.A_Z
                },
                text = { Text(text = "Sort by A-Z") },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    sortType = SortType.Z_A
                },
                text = { Text(text = "Sort by Z-A") },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
            /*
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    sortType = SortType.CATEGORY
                },
                text = { Text(text = "Sort by category") },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )*/
        }
        LazyColumn (
            modifier = Modifier.padding(paddingValues)
        ) {
            items(entriesForToday.size) {
                Row(
                    modifier = Modifier
                        .height(70.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(color = Color.LightGray, shape = RoundedCornerShape(50.dp))
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .clickable { navController.navigate("New/${entriesForToday[it].id}") },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entriesForToday[it].name,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier
                            .padding(2.dp)
                    )
                    Text(
                        text = entriesForToday[it].value,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier
                            .padding(2.dp)
                    )
                }
            }
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
fun New(navController: NavHostController, id: Int) {
    val viewModel: EntryViewModel = viewModel(
        LocalViewModelStoreOwner.current!!,
        "EntryViewModel",
        EntryViewModelFactory(LocalContext.current.applicationContext as Application)
    )
    val entryState by viewModel.entryState.collectAsStateWithLifecycle()

    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }

    var selected by remember { mutableStateOf("Select entry or create new") }
    var type by remember { mutableStateOf(EntryType.NONE) }
    var value by remember { mutableStateOf("") }

    val existingEntry = entryState.find { it.name == selected }
    val isCategoryExisting = existingEntry != null

    if (isCategoryExisting) {
        type = existingEntry!!.type
    }

    val entryFlow = remember(id) { viewModel.getEntryById(id) }
    val entry by entryFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(entry) {
        entry?.let {
            selected = it.name
            type = it.type
            value = it.value
        }
    }

    Scaffold(
        modifier = Modifier,
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextField(
                    value = selected,
                    onValueChange = { selected = it },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(color = Color.Transparent),
                )
                DropdownMenu(
                    expanded = expanded1,
                    onDismissRequest = { expanded1 = false },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    entryState
                        .distinctBy { it.name }
                        .forEach { entry ->
                        DropdownMenuItem(
                            onClick = {
                                selected = entry.name
                                expanded1 = false
                            },
                            text = { Text(text = entry.name) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { expanded1 = !expanded1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            if (isCategoryExisting) {
                TextField(
                    value = type.toString(),
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    readOnly = true,
                    label = { Text("Entry type") }
                )
            }

            else if (selected != "Select entry or create new") {
                TextField(
                    value = type.toString(),
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    readOnly = true,
                    label = { Text("Choose entry type") }
                )
                DropdownMenu(
                    expanded = expanded2,
                    onDismissRequest = { expanded2 = false },
                ) {
                    EntryType.entries.forEach { entryType ->
                        DropdownMenuItem(
                            onClick = {
                                type = entryType
                                expanded2 = false
                            },
                            text = { Text(text = entryType.toString()) }
                        )
                    }
                }
                IconButton(
                    onClick = { expanded2 = !expanded2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            when (type) {
                EntryType.BOOLEAN -> {
                    val isChecked = value.toBooleanStrictOrNull() ?: false
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { value = it.toString() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                EntryType.INT, EntryType.FLOAT, EntryType.STRING -> {
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        label = { Text("Insert value") },
                        keyboardOptions = when (type) {
                            EntryType.INT -> KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                            EntryType.FLOAT -> KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                            else -> KeyboardOptions.Default
                        }
                    )
                }
                EntryType.NONE -> {
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            if (id != -1) {
                Button(
                    onClick = {
                        if (entry != null) {
                            navController.popBackStack()
                            viewModel.deleteById(id)
                        } else {
                            Log.e("NewScreen", "Cannot delete: entry is null")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(Color.Black),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(text = "Delete")
                }
            }
            Button(
                onClick = {
                    if (id == -1) {
                        viewModel.addEntry(
                            Entry(
                                0,
                                selected,
                                type,
                                value,
                                System.currentTimeMillis()
                            )
                        )
                    } else {
                        viewModel.updateEntry(
                            Entry(
                                id,
                                selected,
                                type,
                                value,
                                entry!!.date
                            )
                        )
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(Color.Black),
                shape = RoundedCornerShape(100)
            ) {
                Text(text = "Save", color = Color.White)
            }
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
        New(navController = rememberNavController(), -1)
    }
}