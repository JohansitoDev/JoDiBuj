package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.AvatarCatalog
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCleanInput
import com.example.ui.components.DuoOutlinedButton
import com.example.ui.components.iconForAvatarKey
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CardinalRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FeatherGreenDark
import com.example.ui.theme.FoxOrange
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.MacawBlueDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PendingRegistrationData(
    val username: String = "",
    val bio: String = "",
    val avatarIcon: String = "brush",
    val avatarColor: String = "#1CB0F6",
)

@Composable
fun AuthScreen(
    onAuthSuccess: (PendingRegistrationData) -> Unit,
    onGuestClick: (PendingRegistrationData) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCreateAccountMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var bioInput by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("brush") }
    var selectedColor by remember { mutableStateOf(AvatarCatalog.colors.first()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.jodibuj_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(108.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCreateAccountMode) "Crear tu Cuenta" else "Iniciar Sesión",
                style = MaterialTheme.typography.headlineMedium,
                color = DeepNavy,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "¡Únete a JoDiBuj y diviértete dibujando!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selector Modo Iniciar Sesión / Crear Cuenta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftSurface)
                    .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(4.dp),
            ) {
                ModePill(
                    label = "Iniciar Sesión",
                    selected = !isCreateAccountMode,
                    onClick = {
                        isCreateAccountMode = false
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_login"),
                )
                ModePill(
                    label = "Crear Cuenta",
                    selected = isCreateAccountMode,
                    onClick = {
                        isCreateAccountMode = true
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_register"),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isCreateAccountMode) {
                DuoCleanInput(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.take(24) },
                    label = "Apodo / Usuario",
                    placeholder = "Ej: ArtistaSuper",
                    leadingIcon = Icons.Rounded.AlternateEmail,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    testTag = "register_username_input",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DuoCleanInput(
                    value = emailInput,
                    onValueChange = { emailInput = it.trim().take(80) },
                    label = "Correo Electrónico",
                    placeholder = "ejemplo@correo.com",
                    leadingIcon = Icons.Rounded.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    testTag = "register_email_input",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DuoCleanInput(
                    value = passwordInput,
                    onValueChange = { passwordInput = it.take(40) },
                    label = "Contraseña (mínimo 6 caracteres)",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Rounded.Lock,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Mostrar u ocultar contraseña",
                                tint = TextSecondary,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    testTag = "register_password_input",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DuoCleanInput(
                    value = bioInput,
                    onValueChange = { bioInput = it.take(120) },
                    label = "Biografía (Opcional)",
                    placeholder = "¡Listo para ganar en JoDiBuj!",
                    leadingIcon = Icons.Rounded.Edit,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    testTag = "register_bio_input",
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Elige tu Avatar de Perfil:",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AvatarCatalog.icons.take(6).forEach { (key, label) ->
                        val isSel = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isSel) parseColorSafe(selectedColor) else SoftSurface)
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) DeepNavy else BorderGray,
                                    shape = CircleShape,
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconForAvatarKey(key),
                                contentDescription = label,
                                tint = if (isSel) PureWhite else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AvatarCatalog.colors.take(6).forEach { hex ->
                        val color = parseColorSafe(hex)
                        val isSel = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSel) 3.dp else 0.dp,
                                    color = if (isSel) DeepNavy else color,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSel) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                DuoCleanInput(
                    value = emailInput,
                    onValueChange = { emailInput = it.trim().take(80) },
                    label = "Correo Electrónico o Usuario",
                    placeholder = "ejemplo@correo.com",
                    leadingIcon = Icons.Rounded.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    testTag = "login_email_input",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DuoCleanInput(
                    value = passwordInput,
                    onValueChange = { passwordInput = it.take(40) },
                    label = "Contraseña",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Rounded.Lock,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Mostrar u ocultar contraseña",
                                tint = TextSecondary,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    testTag = "login_password_input",
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = CardinalRed,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón principal (Crear Cuenta / Iniciar Sesión)
            DuoButton(
                onClick = {
                    val cleanUsername = usernameInput.trim()
                    val cleanEmail = emailInput.trim()
                    val cleanPassword = passwordInput.trim()

                    if (isCreateAccountMode) {
                        if (cleanUsername.length < 3) {
                            errorMessage = "El apodo debe tener al menos 3 caracteres."
                            return@DuoButton
                        }
                        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                            errorMessage = "Ingresa un correo electrónico válido."
                            return@DuoButton
                        }
                        if (cleanPassword.length < 6) {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                            return@DuoButton
                        }

                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                Firebase.auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
                                isLoading = false
                                onAuthSuccess(
                                    PendingRegistrationData(
                                        username = cleanUsername,
                                        bio = bioInput.trim(),
                                        avatarIcon = selectedIcon,
                                        avatarColor = selectedColor,
                                    )
                                )
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = when (e) {
                                    is FirebaseAuthException -> when (e.errorCode) {
                                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo electrónico ya está registrado."
                                        "ERROR_INVALID_EMAIL" -> "Formato de correo electrónico inválido."
                                        "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres."
                                        else -> e.localizedMessage ?: "Error al registrar la cuenta."
                                    }
                                    else -> e.localizedMessage ?: "No se pudo crear la cuenta."
                                }
                            }
                        }
                    } else {
                        if (cleanEmail.isBlank()) {
                            errorMessage = "Ingresa tu correo electrónico."
                            return@DuoButton
                        }
                        if (cleanPassword.isBlank()) {
                            errorMessage = "Ingresa tu contraseña."
                            return@DuoButton
                        }

                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val targetEmail = if (cleanEmail.contains("@")) cleanEmail else "$cleanEmail@jodibuj.com"
                                Firebase.auth.signInWithEmailAndPassword(targetEmail, cleanPassword).await()
                                isLoading = false
                                onAuthSuccess(
                                    PendingRegistrationData(
                                        username = cleanUsername.ifBlank { cleanEmail.substringBefore("@") },
                                        bio = bioInput.trim(),
                                        avatarIcon = selectedIcon,
                                        avatarColor = selectedColor,
                                    )
                                )
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = when (e) {
                                    is FirebaseAuthException -> when (e.errorCode) {
                                        "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo."
                                        "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta."
                                        "ERROR_INVALID_EMAIL" -> "Formato de correo inválido."
                                        else -> e.localizedMessage ?: "Error al iniciar sesión."
                                    }
                                    else -> e.localizedMessage ?: "Error al iniciar sesión. Revisa tus datos."
                                }
                            }
                        }
                    }
                },
                containerColor = if (isCreateAccountMode) FeatherGreen else MacawBlue,
                lipColor = if (isCreateAccountMode) FeatherGreenDark else MacawBlueDark,
                isLoading = isLoading,
                testTag = if (isCreateAccountMode) "create_account_button" else "login_button",
            ) {
                Text(
                    text = if (isCreateAccountMode) "✨ Crear Cuenta" else "🔑 Iniciar Sesión",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opción alternativa: Iniciar Sesión con Google
            DuoOutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    launchGoogleSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onSuccess = {
                            isLoading = false
                            onAuthSuccess(
                                PendingRegistrationData(
                                    username = usernameInput.trim(),
                                    bio = bioInput.trim(),
                                    avatarIcon = selectedIcon,
                                    avatarColor = selectedColor,
                                )
                            )
                        },
                        onCancelled = {
                            isLoading = false
                            errorMessage = "Inicio de sesión cancelado."
                        },
                        onError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                    )
                },
                accentColor = MacawBlue,
                enabled = !isLoading,
                height = 48.dp,
                testTag = "google_sign_in_button",
            ) {
                Text(
                    text = "🌐 Continuar con Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opción alternativa: Entrar como Invitado
            DuoOutlinedButton(
                onClick = {
                    onGuestClick(
                        PendingRegistrationData(
                            username = usernameInput.trim().ifBlank { "Invitado" },
                            bio = bioInput.trim(),
                            avatarIcon = selectedIcon,
                            avatarColor = selectedColor,
                        )
                    )
                },
                accentColor = DeepNavy,
                enabled = !isLoading,
                height = 48.dp,
                testTag = "guest_login_button",
            ) {
                Text(
                    text = "👤 Entrar como Invitado",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Puedes crear tu cuenta directamente o entrar sin registro como invitado.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "JoDiBuj · Versión 1.0 (Build 1)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PureWhite else SoftSurface)
            .then(
                if (selected) Modifier.border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MacawBlue else TextSecondary,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun launchGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onError("Falta configuración de Google Sign-In (default_web_client_id).")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onSuccess()
            } else {
                onError("Tipo de credencial inesperado.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            val msg = e.message.orEmpty()
            val userMsg = when {
                msg.contains("10") || msg.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                    "Inicio de sesión rechazado: El certificado SHA-1 de la app no coincide con la configuración de Firebase."
                msg.contains("12500") ->
                    "Inicio de sesión rechazado: Configuración de inicio de sesión de Google no válida en Firebase."
                else -> e.localizedMessage ?: "No se pudo iniciar sesión con Google."
            }
            onError(userMsg)
        }
    }
}
