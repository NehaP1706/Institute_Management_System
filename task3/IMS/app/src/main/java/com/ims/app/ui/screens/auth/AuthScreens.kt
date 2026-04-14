package com.ims.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.theme.*

/** Matches "Login Screen" in Figma */
@Composable
fun LoginScreen(
    viewModel: IMSViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var username    by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    val error       = viewModel.loginError

    // Trigger nav when user is set
    val user = viewModel.currentUser
    LaunchedEffect(user) { if (user != null) onLoginSuccess() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("IMS", color = Primary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("Institute Management System", color = OnSurface, fontSize = 13.sp)
        Spacer(Modifier.height(40.dp))

        // Role hint chips (easy role switching per assignment note)
        Text("Quick Login", color = OnSurface.copy(0.6f), fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("admin","student","faculty").forEach { role ->
                AssistChip(
                    onClick = { username = role; password = "${role}123" },
                    label   = { Text(role, fontSize = 10.sp) },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = SurfaceVar, labelColor = Primary)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = Primary) },
            modifier = Modifier.fillMaxWidth(),
            colors = imsFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon  = { Icon(Icons.Default.Lock, null, tint = Primary) },
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Primary
                    )
                }
            },
            visualTransformation = if (showPass) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = imsFieldColors()
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {}) {
            Text("Forgot Password?", color = Primary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary),
            shape    = RoundedCornerShape(12.dp)
        ) { Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnPrimary) }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign Up", color = Primary)
        }
    }
}

// ── Sign Up Screen ────────────────────────────────────────────────────────────
/** Matches "Sign Up Screen" in Figma */
@Composable
fun SignUpScreen(
    viewModel: IMSViewModel,
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var error    by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", color = Primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        listOf(
            Triple(name,    "Full Name",        { v: String -> name = v }),
            Triple(email,   "Email",            { v: String -> email = v }),
            Triple(password,"Password",         { v: String -> password = v }),
            Triple(confirm, "Confirm Password", { v: String -> confirm = v }),
        ).forEachIndexed { idx, (value, label, onChange) ->
            OutlinedTextField(
                value = value, onValueChange = onChange,
                label = { Text(label) },
                visualTransformation = if (idx >= 2) PasswordVisualTransformation()
                                       else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
                colors = imsFieldColors()
            )
            Spacer(Modifier.height(10.dp))
        }
        if (error != null)
            Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (password != confirm) { error = "Passwords do not match"; return@Button }
                if (viewModel.registerUser(name, email, password)) onSignUpSuccess()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary),
            shape    = RoundedCornerShape(12.dp)
        ) { Text("Create Account", fontWeight = FontWeight.Bold, color = OnPrimary) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login", color = Primary)
        }
    }
}

@Composable
private fun imsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Primary,
    unfocusedBorderColor = Divider,
    focusedLabelColor    = Primary,
    cursorColor          = Primary,
    focusedTextColor     = OnBackground,
    unfocusedTextColor   = OnBackground
)
