package com.example.kaimera.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.kaimera.notes.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    viewModel: NoteViewModel,
    noteId: Int,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Color.White.toArgb()) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(noteId) {
        if (noteId != -1) {
            viewModel.getNoteById(noteId) { note ->
                note?.let {
                    currentNote = it
                    title = it.title
                    content = it.content
                    color = it.color
                    isPinned = it.isPinned
                    isArchived = it.isArchived
                }
            }
        }
    }

    val colors = listOf(
        Color.White, Color(0xFFEF9A9A), Color(0xFFF48FB1), Color(0xFFCE93D8),
        Color(0xFFB39DDB), Color(0xFF9FA8DA), Color(0xFF90CAF9), Color(0xFF81D4FA),
        Color(0xFF80DEEA), Color(0xFF80CBC4), Color(0xFFA5D6A7), Color(0xFFC5E1A5),
        Color(0xFFE6EE9C), Color(0xFFFFF59D), Color(0xFFFFE082), Color(0xFFFFCC80)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin"
                        )
                    }
                    IconButton(onClick = { isArchived = !isArchived }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive")
                    }
                    if (noteId != -1) {
                        IconButton(onClick = {
                            currentNote?.let { viewModel.deleteNote(it) }
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = {
                        if (noteId == -1) {
                            viewModel.addNote(title, content, color)
                        } else {
                            currentNote?.let {
                                viewModel.updateNote(
                                    it.copy(
                                        title = title,
                                        content = content,
                                        color = color,
                                        isPinned = isPinned,
                                        isArchived = isArchived,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(color)
                )
            )
        },
        containerColor = Color(color)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Note") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            // Color Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colors.take(8).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (color == c.toArgb()) 2.dp else 0.dp,
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .clickable { color = c.toArgb() }
                    )
                }
            }
        }
    }
}
