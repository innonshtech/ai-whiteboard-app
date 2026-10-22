package com.avitrisphare.whiteboard.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.core.theme.AvitriSphareTheme
import com.avitrisphare.whiteboard.data.remote.ClassBriefing
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.domain.model.Note
import com.avitrisphare.whiteboard.domain.repository.NoteRepository
import com.avitrisphare.whiteboard.presentation.canvas.CanvasScreen
import com.avitrisphare.whiteboard.presentation.dashboard.DashboardScreen
import com.avitrisphare.whiteboard.presentation.entry.ClassHomeScreen
import com.avitrisphare.whiteboard.presentation.entry.PanelSessionStore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        enableEdgeToEdge()

        setContent {
            val session = remember { PanelSessionStore(this@AvitriSphareActivity) }
            val defaultTeacher = remember {
                PanelTeacher(
                    id = "teacher_1",
                    name = "Teacher",
                    email = "teacher@school.local",
                    subject = "Class",
                    accessToken = "offline_token",
                    refreshToken = null
                )
            }
            var teacher by remember { mutableStateOf<PanelTeacher?>(defaultTeacher) }
            var schoolName by remember { mutableStateOf("Smart Classroom") }
            var classroomName by remember { mutableStateOf("Interactive Panel") }
            var classroomId by remember { mutableStateOf<String?>("room_1") }
            var isDarkMode by remember { mutableStateOf(true) }
            var start by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val stored = session.snapshot()
                teacher = stored.teacher
                schoolName = stored.schoolName
                classroomName = stored.classroomName
                classroomId = stored.classroomId
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
            }
            
            AvitriSphareTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAvitriSphareColors.current.background
                ) {
                    val destination = start
                    if (destination == null) {
                        Box(Modifier.fillMaxSize())
                    } else NavHost(
                        navController = navController,
                        startDestination = destination
                    ) {
                        composable("classhome") {
                            val signedIn = teacher
                            if (signedIn == null) {
                                LaunchedEffect(Unit) {
                                    if (!navController.popBackStack()) finish()
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
                                        session.logout()
                                        teacher = null
                                        if (!navController.popBackStack()) finish()
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
                                    if (!navController.popBackStack()) finish()
                                },
                                demoMode = true,
                                panelSession = session
                            )
                        }
                    }
                }
            }
        }
    }
}