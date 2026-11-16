package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.GuestStore
import com.example.data.JoDiBujRepository
import com.example.data.UserProfile
import com.example.notifications.DailyReminderManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.PendingRegistrationData
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.launchGoogleSignIn
import com.example.ui.theme.BorderGray
import com.example.ui.theme.JoDiBujTheme
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.RoomViewModel
import com.example.ui.viewmodel.UiState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

const val ROUTE_HOME = "home"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_PROFILE = "profile"
const val ROUTE_ROOM = "room/{roomCode}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailyReminderManager.ensureChannel(this)

        setContent {
            JoDiBujTheme {
                JoDiBujRootApp()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun JoDiBujRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var pendingReg by remember { mutableStateOf(PendingRegistrationData()) }
    var silentSignInTried by remember { mutableStateOf(false) }

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    val isGuestActive by GuestStore.observeGuestActive(context)
        .collectAsStateWithLifecycle(initialValue = false)

    // Schedule daily reminder if enabled
    LaunchedEffect(Unit) {
        val settings = DailyReminderManager.observeSettings(context).first()
        if (settings.enabled) {
            DailyReminderManager.scheduleDailyAlarm(context, settings.hour, settings.minute)
        }
    }

    // Silent auto sign-in attempt on launch (only when not explicitly in guest mode)
    LaunchedEffect(showSplash, isGuestActive) {
        if (!showSplash && !isGuestActive && !silentSignInTried && Firebase.auth.currentUser == null) {
            silentSignInTried = true
            attemptSilentAutoSignIn(context, credentialManager)
        }
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else if (currentUser != null) {
        val user = currentUser!!
        key(user.uid) {
            AuthenticatedJoDiBujApp(
                currentUser = user,
                pendingReg = pendingReg,
                onSignOut = {
                    scope.launch {
                        withContext(NonCancellable) {
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (_: Exception) {
                            } finally {
                                GuestStore.exitGuestSession(context)
                                Firebase.auth.signOut()
                            }
                        }
                    }
                },
            )
        }
    } else if (isGuestActive) {
        GuestJoDiBujApp(
            onExitGuest = {
                scope.launch { GuestStore.exitGuestSession(context) }
            },
            onConnectGoogleWithProfile = { regData ->
                pendingReg = regData
            },
        )
    } else {
        AuthScreen(
            onAuthSuccess = { regData ->
                pendingReg = regData
                scope.launch { GuestStore.exitGuestSession(context) }
            },
            onGuestClick = { regData ->
                scope.launch {
                    GuestStore.startGuestSession(
                        context = context,
                        username = regData.username,
                        bio = regData.bio,
                        avatarIcon = regData.avatarIcon,
                        avatarColor = regData.avatarColor,
                    )
                }
            },
        )
    }
}

@Composable
private fun GuestJoDiBujApp(
    onExitGuest: () -> Unit,
    onConnectGoogleWithProfile: (PendingRegistrationData) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val guestProfile by GuestStore.observeGuestProfile(context)
        .collectAsStateWithLifecycle(initialValue = UserProfile(userId = "guest_local", username = "Invitado"))

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val triggerGoogleUpgrade: () -> Unit = {
        launchGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onSuccess = {
                onConnectGoogleWithProfile(
                    PendingRegistrationData(
                        username = guestProfile.username,
                        bio = guestProfile.bio,
                        avatarIcon = guestProfile.avatarIcon,
                        avatarColor = guestProfile.avatarColor,
                    )
                )
                scope.launch { GuestStore.exitGuestSession(context) }
            },
            onCancelled = {
                scope.launch { snackbarHostState.showSnackbar("Inicio de sesión cancelado.") }
            },
            onError = { err ->
                scope.launch { snackbarHostState.showSnackbar(err) }
            },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PureWhite,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 0.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        BottomNavItems(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = {
                                    Icon(
                                        imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MacawBlue,
                                    selectedTextColor = MacawBlue,
                                    indicatorColor = MacawBlue.copy(alpha = 0.12f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PureWhite),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        BottomNavItems(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ).forEach { item ->
                            NavigationRailItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = {
                                    Icon(
                                        imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MacawBlue,
                                    selectedTextColor = MacawBlue,
                                    indicatorColor = MacawBlue.copy(alpha = 0.12f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            profileState = UiState.Success(guestProfile),
                            waitingRoomsState = UiState.Success(emptyList()),
                            isBusy = false,
                            isGuestMode = true,
                            onConnectGoogle = triggerGoogleUpgrade,
                            onCreateRoom = { triggerGoogleUpgrade() },
                            onJoinRoom = { triggerGoogleUpgrade() },
                            onSoloPracticeCompleted = { xp ->
                                scope.launch {
                                    GuestStore.addGuestPoints(context, xp)
                                    snackbarHostState.showSnackbar("+$xp XP ganados en modo invitado")
                                }
                            },
                        )
                    }
                    composable(ROUTE_LEADERBOARD) {
                        LeaderboardScreen(
                            leaderboardState = UiState.Success(emptyList()),
                            currentUserId = "guest_local",
                            isGuestMode = true,
                            onConnectGoogle = triggerGoogleUpgrade,
                        )
                    }
                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profileState = UiState.Success(guestProfile),
                            isBusy = false,
                            isGuestMode = true,
                            onConnectGoogle = triggerGoogleUpgrade,
                            onSaveProfile = { username, _, bio, icon, color ->
                                scope.launch {
                                    GuestStore.updateGuestProfile(context, username, bio, icon, color)
                                    snackbarHostState.showSnackbar("¡Perfil de invitado guardado!")
                                }
                            },
                            onSignOut = onExitGuest,
                            onFeedbackMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedJoDiBujApp(
    currentUser: FirebaseUser,
    pendingReg: PendingRegistrationData,
    onSignOut: () -> Unit,
) {
    val uid = currentUser.uid
    val mainViewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = JoDiBujRepository(db),
                    currentUserId = uid,
                    initialGoogleName = currentUser.displayName ?: "",
                    initialGoogleEmail = currentUser.email ?: "",
                    pendingUsername = pendingReg.username,
                    pendingBio = pendingReg.bio,
                    pendingAvatarIcon = pendingReg.avatarIcon,
                    pendingAvatarColor = pendingReg.avatarColor,
                )
            }
        },
    )

    LaunchedEffect(uid) {
        mainViewModel.ensureProfileInitialized()
    }

    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboard.collectAsStateWithLifecycle()
    val waitingRoomsState by mainViewModel.waitingRooms.collectAsStateWithLifecycle()
    val isBusy by mainViewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
    val inRoom = currentRoute.startsWith("room/")

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearMessage()
        }
    }

    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        val useRail = maxWidth >= 600.dp && !inRoom

        Scaffold(
            containerColor = PureWhite,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail && !inRoom) {
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 0.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        BottomNavItems(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = {
                                    Icon(
                                        imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MacawBlue,
                                    selectedTextColor = MacawBlue,
                                    indicatorColor = MacawBlue.copy(alpha = 0.12f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PureWhite),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        BottomNavItems(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ).forEach { item ->
                            NavigationRailItem(
                                selected = item.selected,
                                onClick = item.onClick,
                                icon = {
                                    Icon(
                                        imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MacawBlue,
                                    selectedTextColor = MacawBlue,
                                    indicatorColor = MacawBlue.copy(alpha = 0.12f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 12.dp)
                            .background(BorderGray),
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(ROUTE_HOME) {
                        val profile = (profileState as? UiState.Success)?.data
                        HomeScreen(
                            profileState = profileState,
                            waitingRoomsState = waitingRoomsState,
                            isBusy = isBusy,
                            onCreateRoom = { prompt ->
                                if (profile != null) {
                                    mainViewModel.createRoom(profile, prompt) { roomCode ->
                                        navController.navigate("room/$roomCode")
                                    }
                                }
                            },
                            onJoinRoom = { code ->
                                if (profile != null) {
                                    mainViewModel.joinRoom(code, profile) { roomCode ->
                                        navController.navigate("room/$roomCode")
                                    }
                                }
                            },
                            onSoloPracticeCompleted = { xp ->
                                mainViewModel.awardSoloLevelXp(xp)
                            },
                        )
                    }

                    composable(ROUTE_LEADERBOARD) {
                        LeaderboardScreen(
                            leaderboardState = leaderboardState,
                            currentUserId = uid,
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profileState = profileState,
                            isBusy = isBusy,
                            onSaveProfile = { username, displayName, bio, icon, color ->
                                mainViewModel.saveProfile(username, displayName, bio, icon, color)
                            },
                            onSignOut = onSignOut,
                            onFeedbackMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }

                    composable(
                        route = ROUTE_ROOM,
                        arguments = listOf(navArgument("roomCode") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val roomCode = backStackEntry.arguments?.getString("roomCode") ?: return@composable
                        val profile = (profileState as? UiState.Success)?.data
                            ?: UserProfile(userId = uid, username = currentUser.displayName ?: "Jugador")

                        val roomViewModel: RoomViewModel = viewModel(
                            key = "${uid}_$roomCode",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY])
                                    val databaseId = app.getString(R.string.firestore_database_id)
                                    val db = FirebaseFirestore.getInstance(databaseId)
                                    RoomViewModel(
                                        repository = JoDiBujRepository(db),
                                        roomCode = roomCode,
                                        currentUserId = uid,
                                    )
                                }
                            },
                        )

                        RoomScreen(
                            viewModel = roomViewModel,
                            userProfile = profile,
                            onExitRoom = { navController.popBackStack() },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class NavItemSpec(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun BottomNavItems(
    currentRoute: String,
    onNavigate: (String) -> Unit,
): List<NavItemSpec> = listOf(
    NavItemSpec(
        label = stringResource(R.string.nav_play),
        selectedIcon = Icons.Rounded.Brush,
        unselectedIcon = Icons.Outlined.Brush,
        selected = currentRoute == ROUTE_HOME,
        testTag = "nav_home",
        onClick = { onNavigate(ROUTE_HOME) },
    ),
    NavItemSpec(
        label = stringResource(R.string.nav_leaderboard),
        selectedIcon = Icons.Rounded.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        selected = currentRoute == ROUTE_LEADERBOARD,
        testTag = "nav_leaderboard",
        onClick = { onNavigate(ROUTE_LEADERBOARD) },
    ),
    NavItemSpec(
        label = stringResource(R.string.nav_profile),
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        selected = currentRoute == ROUTE_PROFILE,
        testTag = "nav_profile",
        onClick = { onNavigate(ROUTE_PROFILE) },
    ),
)

private suspend fun attemptSilentAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(authCredential).await()
        }
    } catch (_: Exception) {
        // Silent sign-in failed cleanly; user sees AuthScreen interactive Google Sign-In
    }
}
