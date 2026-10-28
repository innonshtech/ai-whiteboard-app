package com.avitrisphare.whiteboard.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.core.theme.AvitriSphareTheme
import com.avitrisphare.whiteboard.BuildConfig
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.ClassBriefing
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.domain.model.Note
import com.avitrisphare.whiteboard.domain.repository.NoteRepository
import com.avitrisphare.whiteboard.presentation.canvas.CanvasScreen
import com.avitrisphare.whiteboard.presentation.dashboard.DashboardScreen
import com.avitrisphare.whiteboard.presentation.entry.ClassHomeScreen
import com.avitrisphare.whiteboard.presentation.entry.LicenseActivationScreen
import com.avitrisphare.whiteboard.presentation.entry.PanelSessionStore
import com.avitrisphare.whiteboard.presentation.entry.QrLoginScreen
import com.avitrisphare.whiteboard.presentation.entry.PanelLockedScreen
import com.avitrisphare.whiteboard.data.remote.LicenseCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.avitrisphare.whiteboard.core.localization.AppLanguage
import com.avitrisphare.whiteboard.core.localization.LocalAppLanguage
import java.util.Locale

@AndroidEntryPoint
class AvitriSphareActivity : ComponentActivity() {

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var panelApi: PanelApi

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("gyoash_panel", Context.MODE_PRIVATE)
        val lang = prefs.getString("ui_locale", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val ctx = newBase.createConfigurationContext(config)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        enableEdgeToEdge()

        setContent {
            val session = remember { PanelSessionStore(this@AvitriSphareActivity) }
            var currentLanguage by remember { mutableStateOf(AppLanguage.fromCode(session.uiLocaleTag())) }
            val onLanguageChange: (AppLanguage) -> Unit = { newLang ->
                currentLanguage = newLang
                session.setUiLocaleTag(newLang.code)
                Locale.setDefault(Locale(newLang.code))
            }

            val currentLocale = remember(currentLanguage) { Locale(currentLanguage.code) }
            val localizedConfiguration = remember(currentLanguage) {
                Configuration(resources.configuration).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }
            val activity = this@AvitriSphareActivity
            val localizedContext = remember(currentLanguage, localizedConfiguration) {
                val configContext = activity.createConfigurationContext(localizedConfiguration)
                object : ContextWrapper(activity) {
                    override fun getResources(): Resources = configContext.resources
                    override fun getAssets(): AssetManager = configContext.assets
                }
            }

            var teacher by remember { mutableStateOf<PanelTeacher?>(null) }
            var schoolName by remember { mutableStateOf("School") }
            var classroomName by remember { mutableStateOf("Classroom") }
            var classroomId by remember { mutableStateOf<String?>(null) }
            var isDarkMode by remember { mutableStateOf(true) }
            var start by remember { mutableStateOf<String?>(null) }
            var isLocked by remember { mutableStateOf(false) }
            var pendingImport by remember { mutableStateOf(intent?.data?.toString()) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val incoming = pendingImport
                if (BuildConfig.DEMO_MODE && incoming == null) {
                    val savedId = session.demoNoteId()
                    val noteId = if (savedId != null) {
                        val existing = withContext(Dispatchers.IO) { noteRepository.getNoteById(savedId) }
                        if (existing != null) savedId else null
                    } else null
                    val resolvedId = noteId ?: run {
                        val note = Note(
                            id = UUID.randomUUID().toString(),
                            title = "Whiteboard"
                        )
                        withContext(Dispatchers.IO) { noteRepository.insertNote(note) }
                        session.saveDemoNoteId(note.id)
                        note.id
                    }
                    start = "canvas/$resolvedId"
                    return@LaunchedEffect
                }
                val panelId = session.panelId()
                val stored = session.snapshot()
                val key = stored.licenseKey
                if (key != null) {
                    when (val check = panelApi.checkLicense(key, panelId)) {
                        is LicenseCheckResult.Valid -> {
                            session.saveLicense(key, check.license)
                            isLocked = false
                        }
                        is LicenseCheckResult.Expired -> {
                            isLocked = true
                            session.markLocked(true)
                        }
                        is LicenseCheckResult.SchoolInactive -> {
                            isLocked = true
                            session.markLocked(true)
                        }
                        is LicenseCheckResult.Invalid -> {
                            session.clearLicense()
                            isLocked = false
                        }
                        is LicenseCheckResult.NetworkError -> {
                            if (session.isLicenseExpiredLocally()) {
                                isLocked = true
                            }
                        }
                    }
                }
                val fresh = session.snapshot()
                teacher = fresh.teacher
                schoolName = fresh.schoolName
                classroomName = fresh.classroomName
                classroomId = fresh.classroomId
                start = when {
                    isLocked || fresh.isLocked -> "locked"
                    !fresh.isLicensed -> "license"
                    fresh.teacher == null -> "qr"
                    incoming != null -> {
                        pendingImport = null
                        val note = Note(
                            id = UUID.randomUUID().toString(),
                            title = "Imported Lesson"
                        )
                        withContext(Dispatchers.IO) { noteRepository.insertNote(note) }
                        "canvas/${note.id}?importUri=${android.net.Uri.encode(incoming)}"
                    }
                    else -> "classhome"
                }
            }
            
            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext,
                LocalAppLanguage provides currentLanguage
            ) {
                AvitriSphareTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                LaunchedEffect(start) {
                    if (start == null || start == "locked" || start == "license") return@LaunchedEffect
                    while (isActive) {
                        delay(3 * 60 * 1000L)
                        val currentKey = session.snapshot().licenseKey ?: continue
                        when (val check = panelApi.checkLicense(currentKey, session.panelId())) {
                            is LicenseCheckResult.Expired, is LicenseCheckResult.SchoolInactive -> {
                                isLocked = true
                                session.markLocked(true)
                                navController.navigate("locked") {
                                    popUpTo(0) { inclusive = true }
                                }
                                break
                            }
                            is LicenseCheckResult.Valid -> {
                                session.saveLicense(currentKey, check.license)
                            }
                            is LicenseCheckResult.Invalid -> {
                                session.clearLicense()
                                navController.navigate("license") {
                                    popUpTo(0) { inclusive = true }
                                }
                                break
                            }
                            is LicenseCheckResult.NetworkError -> {
                                if (session.isLicenseExpiredLocally()) {
                                    isLocked = true
                                    session.markLocked(true)
                                    navController.navigate("locked") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAvitriSphareColors.current.background
                ) {
                    val destination = start
                    if (destination == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Checking this panel with the school server…",
                                color = Color(0xFF8B95A8)
                            )
                        }
                    } else NavHost(
                        navController = navController,
                        startDestination = destination
                    ) {
                        composable("license") {
                            LicenseActivationScreen(
                                panelApi = panelApi,
                                panelId = session.panelId(),
                                deviceUuid = session.formattedDeviceId(),
                                onLicenseExpired = { expired ->
                                    schoolName = expired.schoolName
                                    classroomName = expired.classroomName
                                    isLocked = true
                                    session.markLocked(true)
                                    navController.navigate("locked") {
                                        popUpTo("license") { inclusive = true }
                                    }
                                }
                            ) { key, license ->
                                session.saveLicense(key, license)
                                schoolName = license.schoolName
                                classroomName = license.classroomName
                                classroomId = license.classroomId
                                isLocked = false
                                navController.navigate("qr") {
                                    popUpTo("license") { inclusive = true }
                                }
                            }
                        }
                        composable("locked") {
                            val snap = session.snapshot()
                            PanelLockedScreen(
                                schoolName = schoolName.ifBlank { snap.schoolName },
                                deviceUuid = session.formattedDeviceId(),
                                supportEmail = snap.schoolEmail ?: "support@gyoash.com",
                                supportPhone = snap.schoolPhone ?: "+91 98220 11223",
                                onCheckAgain = { onDone ->
                                    val key = session.snapshot().licenseKey
                                    if (key != null) {
                                        scope.launch {
                                            when (val res = panelApi.checkLicense(key, session.panelId())) {
                                                is LicenseCheckResult.Valid -> {
                                                    session.saveLicense(key, res.license)
                                                    schoolName = res.license.schoolName
                                                    classroomName = res.license.classroomName
                                                    classroomId = res.license.classroomId
                                                    isLocked = false
                                                    onDone(true, "Licence is active!")
                                                    navController.navigate("qr") {
                                                        popUpTo("locked") { inclusive = true }
                                                    }
                                                }
                                                is LicenseCheckResult.Expired -> {
                                                    onDone(false, "Licence is still expired. Contact your administrator.")
                                                }
                                                is LicenseCheckResult.SchoolInactive -> {
                                                    onDone(false, "School subscription is inactive.")
                                                }
                                                is LicenseCheckResult.Invalid -> {
                                                    session.clearLicense()
                                                    isLocked = false
                                                    onDone(false, "Licence is invalid.")
                                                    navController.navigate("license") {
                                                        popUpTo("locked") { inclusive = true }
                                                    }
                                                }
                                                is LicenseCheckResult.NetworkError -> {
                                                    onDone(false, "Unable to reach server. Please check network.")
                                                }
                                            }
                                        }
                                    } else {
                                        navController.navigate("license") {
                                            popUpTo("locked") { inclusive = true }
                                        }
                                    }
                                },
                                onEnterNewKey = {
                                    session.clearLicense()
                                    teacher = null
                                    isLocked = false
                                    navController.navigate("license") {
                                        popUpTo("locked") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("qr") {
                            QrLoginScreen(
                                panelApi = panelApi,
                                schoolName = schoolName,
                                onLoggedIn = { signedIn ->
                                    session.login(signedIn)
                                    teacher = signedIn
                                    val import = pendingImport
                                    if (import != null) {
                                        pendingImport = null
                                        scope.launch {
                                            val note = Note(
                                                id = UUID.randomUUID().toString(),
                                                title = "Imported Lesson"
                                            )
                                            noteRepository.insertNote(note)
                                            navController.navigate(
                                                "canvas/${note.id}?importUri=${android.net.Uri.encode(import)}"
                                            ) {
                                                popUpTo("qr") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate("classhome") {
                                            popUpTo("qr") { inclusive = true }
                                        }
                                    }
                                },
                                onChangeLicense = {
                                    session.clearLicense()
                                    teacher = null
                                    isLocked = false
                                    navController.navigate("license") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("classhome") {
                            val signedIn = teacher
                            if (signedIn == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("qr") { popUpTo(0) }
                                }
                            } else {
                                val notes by noteRepository.getAllNotes().collectAsState(initial = emptyList())
                                var briefing by remember { mutableStateOf(ClassBriefing.waiting()) }
                                val subject = briefing.subject?.takeIf { it.isNotBlank() } ?: signedIn.subject
                                val boardTitle = listOf(classroomName, subject)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                LaunchedEffect(signedIn.accessToken, classroomId, classroomName) {
                                    briefing = panelApi.loadClassBriefing(
                                        signedIn.accessToken,
                                        classroomId,
                                        classroomName
                                    )
                                }
                                ClassHomeScreen(
                                    teacher = signedIn,
                                    schoolName = schoolName,
                                    classroomName = classroomName,
                                    briefing = briefing,
                                    boards = notes.sortedByDescending { it.updatedAt },
                                    onOpenWhiteboard = {
                                        val latest = notes
                                            .filter { it.title == boardTitle }
                                            .maxByOrNull { it.updatedAt }
                                        if (latest != null) {
                                            navController.navigate("canvas/${latest.id}")
                                        } else {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                val note = Note(
                                                    id = UUID.randomUUID().toString(),
                                                    title = boardTitle.ifBlank { "Untitled" }
                                                )
                                                noteRepository.insertNote(note)
                                                navController.navigate("canvas/${note.id}")
                                            }
                                        }
                                    },
                                    onOpenBoard = { noteId ->
                                        navController.navigate("canvas/$noteId")
                                    },
                                    onNewBoard = {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val note = Note(
                                                id = UUID.randomUUID().toString(),
                                                title = boardTitle.ifBlank { "Untitled" }
                                            )
                                            noteRepository.insertNote(note)
                                            navController.navigate("canvas/${note.id}")
                                        }
                                    },
                                    onOpenBoards = { navController.navigate("dashboard") },
                                    onLogout = {
                                        val token = signedIn.accessToken
                                        scope.launch { panelApi.logout(token) }
                                        session.logout()
                                        teacher = null
                                        navController.navigate("qr") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                        composable("dashboard") {
                            var showNewNoteDialog by remember { mutableStateOf(false) }
                            
                            DashboardScreen(
                                onNoteClick = { noteId ->
                                    navController.navigate("canvas/$noteId")
                                },
                                onNewNote = {
                                    showNewNoteDialog = true
                                },
                                onToggleTheme = { isDarkMode = !isDarkMode },
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChange
                            )

                            if (showNewNoteDialog) {
                                NewNoteDialog(
                                    onDismiss = { showNewNoteDialog = false },
                                    onCreate = { title ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val note = Note(
                                                id = UUID.randomUUID().toString(),
                                                title = title.ifBlank { "Untitled" }
                                            )
                                            noteRepository.insertNote(note)
                                            showNewNoteDialog = false
                                            navController.navigate("canvas/${note.id}")
                                        }
                                    }
                                )
                            }
                        }

                        composable("canvas") {
                            CanvasScreen(
                                noteId = "default",
                                noteTitle = "",
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChange
                            )
                        }

                        composable(
                            route = "canvas/{noteId}?importUri={importUri}",
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.StringType },
                                navArgument("importUri") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
                            val importUriStr = backStackEntry.arguments?.getString("importUri")
                            CanvasScreen(
                                noteId = noteId,
                                noteTitle = "",
                                onNavigateBack = {
                                    if (!navController.popBackStack()) {
                                        if (BuildConfig.DEMO_MODE) {
                                            finish()
                                        } else {
                                            navController.navigate("classhome") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                demoMode = BuildConfig.DEMO_MODE,
                                panelSession = session,
                                initialImportUri = importUriStr?.let { android.net.Uri.parse(it) },
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChange
                            )
                        }
                    }
                }
            }
        }
        }
    }
}