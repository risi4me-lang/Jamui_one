package com.example.jamuione.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jamuione.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTermsOfService: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val passwordResetState by viewModel.passwordResetState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var ageAcknowledged by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf("EN") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(passwordResetState) {
        if (passwordResetState is Resource.Success) {
            showForgotPasswordDialog = false
            viewModel.resetPasswordResetState()
            scope.launch {
                snackbarHostState.showSnackbar("If an account exists for this email, a reset link is on its way. Check Spam too.")
            }
            resendCooldown = 60
        }
    }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    if (showForgotPasswordDialog) {
        val isResetEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false 
                viewModel.resetPasswordResetState()
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address and we'll send you a link to reset your password.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = resetEmail.isNotBlank() && !isResetEmailValid
                    )
                    if (resetEmail.isNotBlank() && !isResetEmailValid) {
                        Text(
                            text = "Enter a valid email address",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    if (passwordResetState is Resource.Error) {
                        Text(
                            text = (passwordResetState as Resource.Error).message ?: "Error sending reset email",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(resetEmail) },
                    enabled = isResetEmailValid && passwordResetState !is Resource.Loading && resendCooldown == 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (passwordResetState is Resource.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else if (resendCooldown > 0) {
                        Text("Resend in ${resendCooldown}s", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    viewModel.resetPasswordResetState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "District One",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect with your community",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = { language = if (language == "EN") "हिन्दी" else "EN" },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (language == "EN") "EN | हिन्दी" else "हिन्दी | EN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Segmented control 'Login | Sign up'
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = !isSignUp,
                    onClick = { isSignUp = false; viewModel.resetAuthState() },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Login")
                }
                SegmentedButton(
                    selected = isSignUp,
                    onClick = { isSignUp = true; viewModel.resetAuthState() },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Sign up")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1) Continue with Google (primary top button)
            OutlinedButton(
                onClick = { viewModel.signIn(context, ageAcknowledged) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = authState !is AuthState.Loading && (!isSignUp || ageAcknowledged),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.jamuione.R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider 'or use email'
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(" or use email ", modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelMedium)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                text = "Signed up with Google? Use Continue with Google instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (!isSignUp) {
                TextButton(
                    onClick = {
                        resetEmail = email
                        showForgotPasswordDialog = true
                    },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Text("Forgot Password?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Sign up checklist & T&C
            AnimatedVisibility(visible = isSignUp) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp)) {
                    Text("Security Requirements:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    RequirementItem("8+ characters", password.length >= 8)
                    RequirementItem("Uppercase & Lowercase", password.any { it.isUpperCase() } && password.any { it.isLowerCase() })
                    RequirementItem("Numbers & Special", password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() })

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = ageAcknowledged,
                            onCheckedChange = { ageAcknowledged = it },
                            modifier = Modifier.offset(y = (-11).dp)
                        )
                        Text(
                            text = "I am 18 or older and agree to the Terms of Service and Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isInputValid = email.isNotBlank() && password.isNotBlank() && (!isSignUp || (password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isLowerCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } && ageAcknowledged))

            // Main Action Button
            Button(
                onClick = {
                    if (isSignUp) {
                        viewModel.signUpEmail(email, password, ageAcknowledged)
                    } else {
                        viewModel.signInEmail(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = authState !is AuthState.Loading && isInputValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isSignUp) "Get Started" else "Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Disabled button helper text
            if (!isInputValid && authState !is AuthState.Loading) {
                val helperMsg = when {
                    email.isBlank() -> "Enter a valid email"
                    password.isBlank() -> "Enter your password"
                    isSignUp && password.length < 8 -> "Password must be at least 8 characters"
                    isSignUp && (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() }) -> "Include uppercase and lowercase letters"
                    isSignUp && (!password.any { it.isDigit() } || !password.any { !it.isLetterOrDigit() }) -> "Include numbers and special characters"
                    isSignUp && !ageAcknowledged -> "Tick the box to continue"
                    else -> "Complete all fields to continue"
                }
                Text(
                    text = helperMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 'Explore as Guest' outlined text button
            OutlinedButton(
                onClick = onSkipLogin,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("Explore as Guest", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer (on Login keep one small footer line; remove duplicate on Sign up)
            if (!isSignUp) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onViewTermsOfService) {
                        Text("Terms of Service", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = onViewPrivacyPolicy) {
                        Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text = "District One Version 1.0",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )

            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
}
