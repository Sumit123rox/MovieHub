package com.moviehub.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviehub.core.model.Profile
import com.moviehub.core.ui.components.GlassyBox

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        if (activeProfile != null) {
            onProfileSelected()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F0F),
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Text(
                text = "Who's Watching?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    items(uiState.profiles) { profile ->
                        ProfileItem(
                            profile = profile,
                            onClick = { viewModel.selectProfile(profile) }
                        )
                    }

                    item {
                        AddProfileItem(onClick = { showCreateDialog = true })
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, clone ->
                    viewModel.createProfile(name, clone)
                    showCreateDialog = false
                },
                canClone = uiState.profiles.isNotEmpty()
            )
        }
    }
}

@Composable
fun ProfileItem(
    profile: Profile,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        GlassyBox(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // For now, use initials if no avatar
                Text(
                    text = profile.name.take(1).uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun AddProfileItem(
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Profile",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Add Profile",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit,
    canClone: Boolean
) {
    var name by remember { mutableStateOf("") }
    var cloneAddons by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (canClone) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { cloneAddons = !cloneAddons }
                    ) {

                        Checkbox(
                            checked = cloneAddons,
                            onCheckedChange = { cloneAddons = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inherit Addons from active profile")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, cloneAddons) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
