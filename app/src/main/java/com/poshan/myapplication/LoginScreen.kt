package com.poshan.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FlutterLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val lightRed = Color(0xFFEF9A9A)
        val mediumRed = Color(0xFFE57373)
        val darkRed = Color(0xFFC62828)
        
        val topPath = Path().apply {
            moveTo(width * 0.53f, 0f)
            lineTo(width, 0.47f * height)
            lineTo(width * 0.73f, 0.74f * height)
            lineTo(width * 0.26f, 0.27f * height)
            close()
        }
        drawPath(topPath, lightRed)

        val midPath = Path().apply {
            moveTo(width * 0.52f, height * 0.52f)
            lineTo(width * 0.73f, height * 0.74f)
            lineTo(width * 0.47f, height)
            lineTo(width * 0.26f, height * 0.79f)
            close()
        }
        drawPath(midPath, mediumRed)

        val bottomPath = Path().apply {
            moveTo(width * 0.73f, height * 0.74f)
            lineTo(width, height * 0.47f)
            lineTo(width * 0.81f, height * 0.28f)
            lineTo(width * 0.52f, height * 0.52f)
            close()
        }
        drawPath(bottomPath, darkRed)
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var showToastSuccess by remember { mutableStateOf(false) }
    var authenticatedUser by remember { mutableStateOf<User?>(null) }

    val darkRed = Color(0xFFC62828)
    val softBackground = Color(0xFFF8F9FA)
    val softGrey = Color(0xFF6C757D)

    fun performLogin() {
        val uName = username.trim()
        val uPass = password.trim()

        if (uName.isEmpty() || uPass.isEmpty()) {
            message = "Harap isi semua field"
            return
        }

        isLoading = true
        scope.launch(Dispatchers.IO) {
            val user = db.transactionDao().getUserByUsername(uName)
            withContext(Dispatchers.Main) {
                isLoading = false
                if (user != null && user.password == uPass) {
                    authenticatedUser = user
                    showToastSuccess = true
                } else {
                    message = "Username atau Password salah"
                }
            }
        }
    }

    LaunchedEffect(showToastSuccess) {
        if (showToastSuccess && authenticatedUser != null) {
            delay(1000)
            onLoginSuccess(authenticatedUser!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFEBEE), softBackground)
                )
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                    FlutterLogo(modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Login POS System",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF343A40),
                    letterSpacing = 1.sp
                )
            )
            
            Text(
                text = "Selamat Datang Kembali",
                style = MaterialTheme.typography.bodyMedium.copy(color = softGrey)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; if(message.isNotEmpty()) message = "" },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = darkRed) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = darkRed,
                    focusedLabelColor = darkRed,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; if(message.isNotEmpty()) message = "" },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = darkRed) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = softGrey
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color.Black),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = darkRed,
                    focusedLabelColor = darkRed,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { performLogin() })
            )

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { performLogin() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = darkRed,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        Text(
            text = "POS 2026",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = softGrey.copy(alpha = 0.5f)
        )
    }
}
