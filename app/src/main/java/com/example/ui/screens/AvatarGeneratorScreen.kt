package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun AvatarRenderer(avatarId: String, modifier: Modifier = Modifier) {
    if (avatarId.startsWith("gen_base64_")) {
        val base64String = avatarId.removePrefix("gen_base64_")
        val bitmap = remember(base64String) {
            try {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
            return
        }
    }
    
    FantasyAvatarCanvas(avatarId = avatarId, modifier = modifier)
}

@Composable
fun FantasyAvatarCanvas(avatarId: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val size = this.size
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w.coerceAtMost(h) / 2f

        // Determine colors based on style matching
        val colors = when {
            avatarId.contains("fairy") || avatarId == "preset1" -> listOf(Color(0xFFFFB0B0), Color(0xFFFFD3B6), Color(0xFFFFE3E3))
            avatarId.contains("galaxy") || avatarId == "preset2" -> listOf(Color(0xFF3F51B5), Color(0xFF673AB7), Color(0xFFE040FB))
            avatarId.contains("nature") || avatarId == "preset3" -> listOf(Color(0xFF00BFA5), Color(0xFF00E676), Color(0xFFB9F6CA))
            avatarId.contains("watercolor") || avatarId == "preset4" -> listOf(Color(0xFF00B0FF), Color(0xFF00E5FF), Color(0xFFF1F8FF))
            avatarId.contains("cyber") || avatarId == "preset5" -> listOf(Color(0xFFFF007F), Color(0xFF00F3FF), Color(0xFFFFFFFF))
            else -> listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFECDD3)) // Rose gold soft pastel
        }

        // Draw radial background
        drawCircle(
            brush = Brush.radialGradient(
                colors = colors,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                radius = radius
            ),
            radius = radius
        )

        // Draw style specific artwork
        when {
            avatarId.contains("fairy") || avatarId == "preset1" -> {
                // Flower petals / Fairy wings
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (Math.PI / 180f).toFloat()
                    val px = cx + (radius * 0.38f) * kotlin.math.cos(angle)
                    val py = cy + (radius * 0.38f) * kotlin.math.sin(angle)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = radius * 0.3f,
                        center = androidx.compose.ui.geometry.Offset(px, py)
                    )
                }
                // Core glow
                drawCircle(
                    color = Color(0xFFFF6D80),
                    radius = radius * 0.22f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                // Celestial specs
                drawCircle(color = Color.White, radius = 5f, center = androidx.compose.ui.geometry.Offset(cx - radius * 0.45f, cy - radius * 0.3f))
                drawCircle(color = Color.White, radius = 5f, center = androidx.compose.ui.geometry.Offset(cx + radius * 0.4f, cy + radius * 0.45f))
            }
            avatarId.contains("galaxy") || avatarId == "preset2" -> {
                // Galaxy rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 6f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius * 0.4f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                // crescent moon
                val moonPath = Path().apply {
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(cx - radius * 0.28f, cy - radius * 0.28f, cx + radius * 0.28f, cy + radius * 0.28f),
                        startAngleDegrees = -90f, sweepAngleDegrees = 180f
                    )
                    quadraticTo(cx, cy, cx, cy - radius * 0.28f)
                    close()
                }
                drawPath(path = moonPath, color = Color(0xFFFFD54F))
                // magic stars
                drawCircle(color = Color.White, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx + radius * 0.45f, cy - radius * 0.5f))
                drawCircle(color = Color.White, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx - radius * 0.45f, cy + radius * 0.35f))
            }
            avatarId.contains("nature") || avatarId == "preset3" -> {
                // Nature Leaf representation
                val leafPath1 = Path().apply {
                    moveTo(cx, cy - radius * 0.6f)
                    cubicTo(cx - radius * 0.4f, cy - radius * 0.2f, cx - radius * 0.2f, cy + radius * 0.4f, cx, cy + radius * 0.6f)
                    cubicTo(cx + radius * 0.2f, cy + radius * 0.4f, cx + radius * 0.4f, cy - radius * 0.2f, cx, cy - radius * 0.6f)
                    close()
                }
                drawPath(path = leafPath1, color = Color(0xFF0F9D58).copy(alpha = 0.75f))
                drawCircle(
                    color = Color(0xFFA3E635),
                    radius = radius * 0.22f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                // white stars
                drawCircle(color = Color.White, radius = 5f, center = androidx.compose.ui.geometry.Offset(cx - radius * 0.35f, cy - radius * 0.2f))
                drawCircle(color = Color.White, radius = 5f, center = androidx.compose.ui.geometry.Offset(cx + radius * 0.35f, cy - radius * 0.3f))
            }
            avatarId.contains("watercolor") || avatarId == "preset4" -> {
                // organic fluid blobs
                drawCircle(color = Color(0x7F22D3EE), radius = radius * 0.52f, center = androidx.compose.ui.geometry.Offset(cx - radius * 0.15f, cy - radius * 0.1f))
                drawCircle(color = Color(0x7F6366F1), radius = radius * 0.45f, center = androidx.compose.ui.geometry.Offset(cx + radius * 0.2f, cy + radius * 0.15f))
                drawCircle(color = Color(0x7FF43F5E), radius = radius * 0.38f, center = androidx.compose.ui.geometry.Offset(cx - radius * 0.05f, cy + radius * 0.25f))
                drawCircle(color = Color.White, radius = radius * 0.14f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
            avatarId.contains("cyber") || avatarId == "preset5" -> {
                // Futuristic cyber circles
                drawCircle(color = Color(0xFF38BDF8), radius = radius * 0.75f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(width = 5f))
                drawRect(color = Color(0xFFEC4899).copy(alpha = 0.65f), topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 0.4f, cy - radius * 0.08f), size = androidx.compose.ui.geometry.Size(radius * 0.8f, radius * 0.16f))
                drawCircle(color = Color.White, radius = radius * 0.18f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
            else -> {
                // Default Profile head icon (Aesthetic silhouette)
                drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = radius * 0.45f, center = androidx.compose.ui.geometry.Offset(cx, cy - radius * 0.12f))
                val bodyPath = Path().apply {
                    moveTo(cx - radius * 0.65f, cy + radius * 0.62f)
                    quadraticTo(cx, cy + radius * 0.22f, cx + radius * 0.65f, cy + radius * 0.62f)
                    lineTo(cx + radius * 0.65f, cy + radius * 1.0f)
                    lineTo(cx - radius * 0.65f, cy + radius * 1.0f)
                    close()
                }
                drawPath(path = bodyPath, color = primaryColor.copy(alpha = 0.42f))
            }
        }
    }
}

// Data class representing an avatar option
data class AvatarOption(
    val id: String,
    val name: String,
    val isGenerated: Boolean = false,
    val styleKey: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarGeneratorScreen(
    viewModel: MahitaViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val currentAvatar by viewModel.userAvatar.collectAsState()
    var snackbarHostState = remember { SnackbarHostState() }

    var userPrompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("fairy") } // fairy, galaxy, nature, watercolor, cyber
    var isGenerating by remember { mutableStateOf(false) }
    var generationStep by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var selectedResolution by remember { mutableStateOf("1K") }

    // List of artistic styles to influence the generator
    val fantasyStyles = listOf(
        Triple("fairy", "🌸 پری رویایی", "رایحه‌ای از گل‌های بهاری، بال‌های شب‌تاب و گلبرگ‌های درخشان"),
        Triple("galaxy", "🔮 ملکه کهکشانی", "حلقه‌های کیهانی، هلال ماه طلایی، ذرات ستاره‌ای بنفش"),
        Triple("nature", "🍃 طبیعت شفابخش", "خلوص برگ‌های جنگلی، طرح گل لوتوس و سبز نعنایی"),
        Triple("watercolor", "🎨 آبرنگ آبگون", "تلفیق سیال و لطیف رنگ‌های فانتزی آسمانی و صورتی ملایم"),
        Triple("cyber", "⚡ فانتزی سایبرپانک", "المان‌های مدرن نئون، مدارهای سحرآمیز صورتی ملایم و فیروزه‌ای")
    )

    // Store custom generated items locally in a list during the session
    // Initialized with standard presets
    val avatarOptionsList = remember {
        mutableStateListOf(
            AvatarOption("avatar_default", "آواتار پیش‌فرض"),
            AvatarOption("preset1", "گلبرگ خیال (پری)", false, "fairy"),
            AvatarOption("preset2", "نگین شب (کهکشان)", false, "galaxy"),
            AvatarOption("preset3", "آرامش دشت (طبیعت)", false, "nature"),
            AvatarOption("preset4", "ابر و باد (آبرنگ)", false, "watercolor"),
            AvatarOption("preset5", "مدار الکترون (سایبر)", false, "cyber")
        )
    }

    // Select avatar
    var selectedAvatarId by remember { mutableStateOf(currentAvatar) }

    // Generates the avatar through the view model's image model, with live progress.
    val startAIGenerator: () -> Unit = {
        if (userPrompt.isBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("عزیزم! لطفاً ابتدا ویژگی یا حسی که دوست داری در آواتارت باشه رو بنویس. 💕")
            }
        } else {
            isGenerating = true
            coroutineScope.launch {
                // Step 1: Initialize connection
                generationStep = "در حال تولید تصویر با استفاده از Gemini 3 Pro Image..."
                progress = 0.3f
                
                val styleString = fantasyStyles.firstOrNull { it.first == selectedStyle }?.second ?: ""
                val fullPrompt = "$userPrompt. The style should be: $styleString"
                
                val base64Image = viewModel.generateAvatarImageWithAI(fullPrompt, selectedResolution)

                if (base64Image != null) {
                    val newId = "gen_base64_$base64Image"
                    val styleLabel = fantasyStyles.firstOrNull { it.first == selectedStyle }?.second ?: "سفارشی"
                    val newOption = AvatarOption(newId, "طرح $styleLabel (AI - $selectedResolution)", true, "custom")
                    
                    avatarOptionsList.add(1, newOption)
                    selectedAvatarId = newId
                    progress = 1.0f
                    generationStep = "آواتار شما تولید شد! 🎉"
                    snackbarHostState.showSnackbar("آواتار جادویی آماده ثبت است!")
                } else {
                    progress = 1.0f
                    generationStep = "خطا در تولید تصویر! سهمیه مجاز محدودیت دارد یا ارتباط شبکه قطع است."
                    snackbarHostState.showSnackbar("تولید آواتار با خطا مواجه شد.")
                }
                
                delay(1000)
                isGenerating = false
                // Keep the description after a failure so it can be retried without retyping.
                if (base64Image != null) userPrompt = ""
                focusManager.clearFocus()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("استودیو تولید آواتار فانتزی 🔮", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("طراحی آواتارهای رویایی و کاهش اضطراب با هنر هوشمند", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview of Current / Selected Avatar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "تصویر آواتار انتخابی شما",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(contentAlignment = Alignment.Center) {
                        // Background glow ring
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFFEC4899))
                                    ),
                                    CircleShape
                                )
                        )
                        // Inner circle masking the drawing
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AvatarRenderer(
                                avatarId = selectedAvatarId,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.setUserAvatar(selectedAvatarId)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("آواتار شما با موفقیت تغییر کرد و ذخیره شد! ✨")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(min = 180.dp),
                        enabled = selectedAvatarId != currentAvatar
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت نهایی آواتار", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // AI Image Generator Space (Imagen based)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(18.dp))
                        Text("تولید آواتار فانتزی با هوش مصنوعی (Imagen 3)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    Text(
                        "سبک رویایی و جزئیات مورد نظر خود را ذکر نمایید تا هوش مصنوعی تصویر پرتره را به صورت هنری خلق کند.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                    )

                    // Text Field for dynamic prompt description
                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        label = { Text("پیشنهاد یا توصیف جزئیات (مثال: دختری آرام با چشمان کهکشانی)", fontSize = 11.sp) },
                        placeholder = { Text("هر چیزی مایلید ترسیم شود بنویسید...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    // Flow of selecting custom fantasy art style
                    Text("سبک هنری را برگزینید:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fantasyStyles.forEach { (key, label, desc) ->
                            val active = selectedStyle == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { selectedStyle = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.split(" ").last(), // Icon + main word
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val resolutions = listOf("1K", "2K", "4K")
                    
                    Text("کیفیت تصویر را انتخاب کنید:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        resolutions.forEach { res ->
                            val active = selectedResolution == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) Color(0xFF6366F1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { selectedResolution = res }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Loading State with live procedural metrics
                    AnimatedVisibility(
                        visible = isGenerating,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.25f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(generationStep, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }

                    if (!isGenerating) {
                        Button(
                            onClick = startAIGenerator,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)) // Vivid Red/Pink button
                        ) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تولید آواتار با هوش مصنوعی (Gemini 3 Pro)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Edit Image Button
                        val selectedImageOption = avatarOptionsList.firstOrNull { it.id == selectedAvatarId }
                        if (selectedImageOption != null && userPrompt.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    isGenerating = true
                                    coroutineScope.launch {
                                        generationStep = "در حال ویرایش تصویر با Gemini 3.1 Flash Image..."
                                        progress = 0.5f
                                        val base64Image = viewModel.createOrEditImageFlash(userPrompt, selectedResolution)
                                        if (base64Image != null) {
                                            val newId = "gen_base64_$base64Image"
                                            val newOption = AvatarOption(newId, "ویرایش شده: $userPrompt", true, "custom")
                                            avatarOptionsList.add(1, newOption)
                                            selectedAvatarId = newId
                                            progress = 1.0f
                                            generationStep = "آواتار شما ویرایش شد! 🎉"
                                            snackbarHostState.showSnackbar("آواتار جادویی آماده ثبت است!")
                                        } else {
                                            progress = 1.0f
                                            generationStep = "خطا در ویرایش تصویر!"
                                            snackbarHostState.showSnackbar("ویرایش آواتار با خطا مواجه شد.")
                                        }
                                        delay(1000)
                                        isGenerating = false
                                        userPrompt = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ویرایش تصویر فعلی بر اساس متن بالا (Gemini 3.1 Flash Image)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List of available / generated options
            Text("گالری آواتارهای در دسترس", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(avatarOptionsList, key = { it.id }) { item ->
                    val isSelected = selectedAvatarId == item.id
                    val isCurrentApplied = currentAvatar == item.id
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAvatarId = item.id }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                            ) {
                                AvatarRenderer(
                                    avatarId = item.id,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (isCurrentApplied) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
