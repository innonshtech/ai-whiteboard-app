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
import com.google.gson.Gson
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.core.theme.AvitriSphareColors
import com.avitrisphare.whiteboard.core.theme.AvitriSphareTheme
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AvitriSphareActivity : ComponentActivity() {

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var panelApi: PanelApi

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        enableEdgeToEdge()

        setContent {
            val session = remember { PanelSessionStore(this@AvitriSphareActivity) }
            var teacher by remember { mutableStateOf<PanelTeacher?>(null) }
            var schoolName by remember { mutableStateOf("School") }
            var classroomName by remember { mutableStateOf("Classroom") }
            var classroomId by remember { mutableStateOf<String?>(null) }
            var isDarkMode by remember { mutableStateOf(true) }
            var start by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val panelId = session.panelId()
                val stored = session.snapshot()
                val key = stored.licenseKey
                if (key != null) {
                    when (val checked = panelApi.verifyLicense(key, panelId)) {
                        is ApiCall.Ok -> session.saveLicense(key, checked.value)
                        is ApiCall.Err -> if (checked.licenseRejected) session.clearLicense()
                    }
                }
                val fresh = session.snapshot()
                teacher = fresh.teacher
                schoolName = fresh.schoolName
                classroomName = fresh.classroomName
                classroomId = fresh.classroomId
                start = when {
                    !fresh.isLicensed -> "license"
                    fresh.teacher == null -> "qr"
                    else -> "classhome"
                }
            }
            
            AvitriSphareTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAvitriSphareColors.current.background
                ) {
                    val destination = start
                    if (destination == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Checking this panel with the school server…", color = Color(0xFF8B95A8))
                        }
                    } else NavHost(
                        navController = navController,
                        startDestination = destination
                    ) {
                        composable("license") {
                            LicenseActivationScreen(
                                panelApi = panelApi,
                                panelId = session.panelId()
                            ) { key, license ->
                                session.saveLicense(key, license)
                                schoolName = license.schoolName
                                classroomName = license.classroomName
                                classroomId = license.classroomId
                                navController.navigate("qr") {
                                    popUpTo("license") { inclusive = true }
                                }
                            }
                        }
                        composable("qr") {
                            QrLoginScreen(
                                panelApi = panelApi,
                                schoolName = schoolName,
                                onLoggedIn = { signedIn ->
                                    session.login(signedIn)
                                    teacher = signedIn
                                    navController.navigate("classhome") {
                                        popUpTo("qr") { inclusive = true }
                                    }
                                },
                                onChangeLicense = {
                                    session.clearLicense()
                                    teacher = null
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
                                        scope.launch {
                                            panelApi.logout(token)
                                        }
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
                                onToggleTheme = { isDarkMode = !isDarkMode }
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

                        composable(
                            route = "canvas/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
                            CanvasScreen(
                                noteId = noteId,
                                noteTitle = "",
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}