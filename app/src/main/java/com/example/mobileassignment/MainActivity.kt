package com.example.mobileassignment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.mobileassignment.ui.theme.MobileAssignmentTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileAssignmentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VietnameseAppScreen()
                }
            }
        }
    }
}

// Helper function to create a temporary image file URI
fun Context.createImageUri(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(cacheDir, "images").apply { mkdirs() } // Use cache dir + /images subdirectory
    val imageFile = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
    // Make sure the authority matches the one declared in AndroidManifest.xml
    return FileProvider.getUriForFile(
        Objects.requireNonNull(this),
        "${packageName}.provider", // Use your app's package name + .provider
        imageFile
    )
}

@Composable
fun VietnameseAppScreen() {
    val context = LocalContext.current

    // State for the captured image URI (for each card if needed, or one shared state)
    // Let's use separate states for clarity, assuming each card might show a different image
    var imageUriPlane by remember { mutableStateOf<Uri?>(null) }
    var imageUriImage by remember { mutableStateOf<Uri?>(null) }

    // Temporary URI holder for the camera intent
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    // State to track which button's image should be updated
    var targetImageSetter by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

    // Launcher for taking a picture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // Image captured successfully, update the correct state
                targetImageSetter?.invoke(tempUri)
            } else {
                // Handle failure or cancellation (optional)
                // tempUri = null // Reset if needed
                targetImageSetter?.invoke(null) // Clear the image on failure/cancel
            }
            // Reset target setter and tempUri after use
            targetImageSetter = null
            tempUri = null
        }
    )

    // Launcher for requesting camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // Permission granted, launch the camera now
                val newUri = context.createImageUri()
                tempUri = newUri // Store the URI temporarily
                cameraLauncher.launch(newUri)
            } else {
                // Handle permission denial (e.g., show a message to the user)
                println("Camera permission denied")
                targetImageSetter = null // Reset target if permission denied
            }
        }
    )

    // Function to check permission and launch camera
    fun checkAndLaunchCamera(setter: (Uri?) -> Unit) {
        targetImageSetter = setter // Set which state to update on result
        val permission = Manifest.permission.CAMERA
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, permission) -> {
                // Permission already granted, create URI and launch camera
                val newUri = context.createImageUri()
                tempUri = newUri
                cameraLauncher.launch(newUri)
            }
            else -> {
                // Request permission
                permissionLauncher.launch(permission)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // First option: Find Flat/Room
            OptionCard(
                buttonText = "TÌM MẶT PHẲNG", // Corrected typo: MẤT -> MẶT
                imageUri = imageUriPlane,      // Pass the state URI
                onButtonClick = {              // Pass the launch function
                    checkAndLaunchCamera { uri -> imageUriPlane = uri }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Second option: 3D Object (replaced "TÌM HÌNH ẢNH")
            ThreeDObjectCard(
                onButtonClick = {
                    // Launch the 3D object activity
                    val intent = Intent(context, Object3DActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }

        // Bottom button: Choose Display Method
        Button(
            onClick = { /* TODO: Add action */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)), // Dark blue color
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(56.dp)
        ) {
            Text(
                text = "« CHỌN CÁCH HIỂN THỊ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun OptionCard(
    buttonText: String,
    imageUri: Uri?, // Accept nullable Uri
    onButtonClick: () -> Unit // Accept onClick lambda
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Display image if URI exists, otherwise show placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUri),
                    contentDescription = buttonText, // Better description
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Adjust as needed
                )
            } else {
                Text(
                    text = "Image Placeholder",
                    color = Color.Gray
                )
            }
        }

        // Button - triggers the passed lambda
        Button(
            onClick = onButtonClick, // Use the passed lambda
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), // Blue color
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ThreeDObjectCard(
    onButtonClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 3D Object preview placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color(0xFF90CAF9)), // Light blue background
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "3D Vase Preview",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        // Button - triggers the 3D activity
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), // Blue color
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "3D OBJECT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VietnameseAppPreview() {
    MobileAssignmentTheme {
        // Preview won't show camera functionality, but UI layout
        VietnameseAppScreen()
    }
}