package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.route.AppView
import com.mario.tanamin.ui.viewmodel.ProfileUiState
import com.mario.tanamin.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileView(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Root container (Use Box to avoid constraints issues, fillMaxSize)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchProfile() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile
                
                var isEditing by remember { mutableStateOf(false) }
                var editName by remember { mutableStateOf(profile.name) }
                var editEmail by remember { mutableStateOf(profile.email) }
                var editPassword by remember { mutableStateOf("") }

                // Reset fields when profile changes
                LaunchedEffect(profile) {
                     editName = profile.name
                     editEmail = profile.email
                     editPassword = ""
                }

                // Top Gradient
                val topGradient = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 600f
                )

                // Content Box
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // Fixed Background Decoration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(topGradient)
                            .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                    ) {
                        // Deco Circle
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .offset(x = (-80).dp, y = (-80).dp)
                                .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                        )
                    }

                    // Scrollable Column
                    // IMPORTANT: Only ONE scrollable container here.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        // Header Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                         
                        // Profile Image Card
                        Card(
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(8.dp),
                             modifier = Modifier.size(110.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                         
                        Spacer(modifier = Modifier.height(16.dp))
                         
                        // Profile Name & Username
                        Text(
                            text = profile.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "@${profile.username}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Information / Edit Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Profile Information",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(onClick = { 
                                        if(isEditing) {
                                            isEditing = false 
                                            editName = profile.name
                                            editEmail = profile.email
                                            editPassword = ""
                                        } else {
                                            isEditing = true
                                        }
                                    }) {
                                         Text(
                                            text = if (isEditing) "Cancel" else "Edit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                ProfileField(
                                    label = "Name", 
                                    value = if (isEditing) editName else profile.name,
                                    isEditing = isEditing,
                                    onValueChange = { editName = it },
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ProfileField(
                                    label = "Email", 
                                    value = if (isEditing) editEmail else profile.email,
                                    isEditing = isEditing,
                                    onValueChange = { editEmail = it },
                                     textColor = MaterialTheme.colorScheme.onSurface
                                )
                                 if (isEditing) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                     ProfileField(
                                        label = "New Password", 
                                        value = editPassword,
                                        isEditing = true, 
                                        isPassword = true,
                                        onValueChange = { editPassword = it },
                                         textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateProfile(editName, editEmail, editPassword.ifBlank { null })
                                            isEditing = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                         Text("Save Changes", modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Theme Shop Banner
                        // Fixed syntax here
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                    )
                                )
                                .clickable { 
                                    navController.navigate(AppView.ThemeShop.name) 
                                }
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingBag,
                                        contentDescription = "Theme Shop",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Theme Shop",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go to Shop",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        // Bottom spacer to avoid navigation bar overlap if needed
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileField(
    label: String, 
    value: String,
    isEditing: Boolean = false,
    isPassword: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    onValueChange: (String) -> Unit = {}
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = if (isPassword) "••••••••" else value,
                    fontSize = 14.sp,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileViewPreview() {
    MaterialTheme {
        ProfileView(navController = rememberNavController())
    }
}
