package com.ims.app.ui.screens.auth

import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.theme.*

/**
 * Combined Login + Sign-Up screen.
 *
 * FIX: [onLoginSuccess] now receives the destination route so the nav host
 * can send admins to [Screen.Dashboard] and students to
 * [Screen.Dashboard] (same composable, but the composable itself now renders
 * role-appropriate content).  If you later create a dedicated StudentDashboard
 * screen just change the route returned here.
 */
@Composable
fun LoginScreen(
    viewModel: IMSViewModel,
    onLoginSuccess: (route: String) -> Unit,   // FIX: carries the destination route
    onNavigateToSignUp: () -> Unit              // kept for nav-graph back-compat; unused internally
) {
    // ── Tab state ────────────────────────────────────────────────────────
    var isLoginTab by remember { mutableStateOf(true) }

    // ── Login fields ─────────────────────────────────────────────────────
    var loginEmail    by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPass by remember { mutableStateOf(false) }
    val loginError = viewModel.loginError

    // ── Sign-up fields ───────────────────────────────────────────────────
    var name             by remember { mutableStateOf("") }
    var signUpEmail      by remember { mutableStateOf("") }
    var signUpPass       by remember { mutableStateOf("") }
    var confirmPass      by remember { mutableStateOf("") }
    var showSignUpPass   by remember { mutableStateOf(false) }
    var showConfirmPass  by remember { mutableStateOf(false) }
    var signUpError      by remember { mutableStateOf<String?>(null) }

    // FIX: Navigate to the correct destination based on the user's role,
    // not blindly to Dashboard every time.
    val user = viewModel.currentUser
    LaunchedEffect(user) {
        if (user != null) {
            // All roles currently land on Dashboard, but the Dashboard
            // composable now branches its UI by role.  If you add a dedicated
            // StudentDashboard route in Navigation.kt, swap it in here:
            //   Screen.StudentDashboard.route for students
            val destination = Screen.Dashboard.route
            onLoginSuccess(destination)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Globe / language icon ─────────────────────────────────────────
        IconButton(
            onClick = { /* language picker placeholder */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = "Language",
                tint = OnSurface.copy(alpha = 0.6f)
            )
        }

        // ── Centred body ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = Color(0xFF76D6D5),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = Color(0xFF003737),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "IMS",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Institute Management System",
                color = OnSurface,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── Tab switcher ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(SurfaceVar),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(true to "Login", false to "Sign Up").forEach { (tabIsLogin, label) ->
                    val selected = (isLoginTab == tabIsLogin)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (selected) Color(0xFF76D6D5) else Color.Transparent
                            )
                    ) {
                        TextButton(
                            onClick = { isLoginTab = tabIsLogin },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color(0xFF003737) else OnSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Teal card ─────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0F8584),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                    if (isLoginTab) {
                        // ── LOGIN CONTENT ─────────────────────────────────

                        // Quick-login chips
                        Text(
                            text = "Quick Login",
                            color = OnSurface.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            listOf("admin", "student").forEach { role ->
                                AssistChip(
                                    onClick = {
                                        loginEmail = "$role@ims.ac.in"
                                        loginPassword = "${role}123"
                                    },
                                    label = { Text(role, fontSize = 10.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = SurfaceVar,
                                        labelColor = Primary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Email field
                        CardFieldLabel("EMAIL")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            placeholder = {
                                Text("x@y.iit.ac.in", color = OnSurface.copy(alpha = 0.35f))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(32.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        // Password field
                        CardFieldLabel("PASSWORD")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showLoginPass = !showLoginPass }) {
                                    Icon(
                                        imageVector = if (showLoginPass) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (showLoginPass) "Hide password"
                                        else "Show password",
                                        tint = OnSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (showLoginPass) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(32.dp)
                        )

                        if (loginError != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = loginError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { /* TODO: forgot password */ }) {
                                Text(
                                    text = "Forgot Password?",
                                    color = Color(0xFF76D6D5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                    } else {
                        // ── SIGN-UP CONTENT ───────────────────────────────

                        // Full Name
                        CardFieldLabel("FULL NAME")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Email
                        CardFieldLabel("EMAIL")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = signUpEmail,
                            onValueChange = { signUpEmail = it },
                            placeholder = {
                                Text("x@y.iit.ac.in", color = OnSurface.copy(alpha = 0.35f))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Password
                        CardFieldLabel("PASSWORD")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = signUpPass,
                            onValueChange = { signUpPass = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSignUpPass = !showSignUpPass }) {
                                    Icon(
                                        imageVector = if (showSignUpPass) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = OnSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (showSignUpPass) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Confirm Password
                        CardFieldLabel("CONFIRM PASSWORD")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OnSurface.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                                    Icon(
                                        imageVector = if (showConfirmPass) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = OnSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (showConfirmPass) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = cardFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (signUpError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = signUpError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── CTA button ────────────────────────────────────────────────
            Button(
                onClick = {
                    if (isLoginTab) {
                        viewModel.login(loginEmail, loginPassword)
                    } else {
                        signUpError = null
                        when {
                            name.isBlank() || signUpEmail.isBlank() || signUpPass.isBlank() ->
                                signUpError = "Please fill in all fields"
                            signUpPass != confirmPass ->
                                signUpError = "Passwords do not match"
                            else -> {
                                if (viewModel.registerUser(name, signUpEmail, signUpPass)) {
                                    isLoginTab = true
                                } else {
                                    signUpError = "Registration failed. Please try again."
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76D6D5)),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(
                    text = if (isLoginTab) "Login" else "Create Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF003737)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Footer ────────────────────────────────────────────────────
            Text(
                text = "By continuing you agree to our\nTerms & Privacy Policy",
                color = OnSurface.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun CardFieldLabel(text: String) {
    Text(
        text = text,
        color = OnSurface.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun cardFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Color(0xFF76D6D5),
    unfocusedBorderColor    = Color(0xFF2A6060),
    focusedLabelColor       = Color(0xFF76D6D5),
    cursorColor             = Color(0xFF76D6D5),
    focusedTextColor        = Color.White,
    unfocusedTextColor      = Color.White,
    focusedContainerColor   = Color(0xFF071325),
    unfocusedContainerColor = Color(0xFF071325)
)