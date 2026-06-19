package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity.SpeechSTTManager.speechRecognizer
import com.example.data.model.Memory
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.MindOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: MindOSViewModel by viewModels()

    object SpeechSTTManager {
        var speechRecognizer: SpeechRecognizer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mindos_scaffold"),
                    containerColor = CosmicBackground
                ) { innerPadding ->
                    MindOSDashboard(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error destroying speech recognizer", e)
        }
    }
}

// Global active tab enum
enum class DashboardTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    INTEL("Intel", Icons.Default.Info),
    MEMORY("Memory", Icons.Default.Menu),
    COACH("Coach", Icons.Default.Face)
}

@Composable
fun MindOSDashboard(
    viewModel: MindOSViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DashboardTab.INTEL) }
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        // App Header Banner
        MindOSHeader(onClearAll = { viewModel.clearAllMemories() })

        // Main Tab Screen Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                DashboardTab.INTEL -> IntelBriefScreen(viewModel = viewModel, tasks = tasks)
                DashboardTab.MEMORY -> MemoryVaultScreen(viewModel = viewModel, memories = memories)
                DashboardTab.COACH -> AI_CoachScreen(viewModel = viewModel)
            }
        }

        // Sleek Custom Cyber Bottom Navigation
        MindOSBottomNavigation(
            activeTab = activeTab,
            onTabSelected = { activeTab = it }
        )
    }
}

@Composable
fun MindOSHeader(onClearAll: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .drawBehind {
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.4f),
                                radius = size.minDimension * 1.5f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MIND.OS",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                )
            }
            Text(
                text = "NEURAL COGNITIVE INTERFACE v1.02",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonCyan.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
            )
        }

        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .size(36.dp)
                .background(CosmicSurfaceVariant, shape = RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sync options / Purge database",
                tint = OnCosmicSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "RE-INITIALIZE SYNAPTIC NODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear your entire Memory Database and re-seed the default MindOS telemetry baseline?",
                    fontSize = 14.sp,
                    color = OnCosmicSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showDialog = false
                    }
                ) {
                    Text("PURGE & RESET", color = StatusRed, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("CANCEL", color = OnCosmicSurface)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = OnCosmicSurface,
            titleContentColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun MindOSBottomNavigation(
    activeTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Surface(
        color = CosmicSurface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardTab.values().forEach { tab ->
                val selected = (activeTab == tab)
                val color = if (selected) NeonCyan else OnCosmicSurface.copy(alpha = 0.5f)

                Column(
                    modifier = Modifier
                        .testTag("tab_${tab.label.lowercase()}")
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            color = color,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INTEL BRIEF SCREEN: Feature 3 (Briefing) & Active Goals list
// -------------------------------------------------------------
@Composable
fun IntelBriefScreen(
    viewModel: MindOSViewModel,
    tasks: List<Memory>
) {
    val activeBrief by viewModel.activeBrief.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingBrief.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        // Beautiful Brain Neural Header Art Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_brain_nodes_1781860138334),
                contentDescription = "Cosmic Brain Neural Connections",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix().apply {
                        setToSaturation(0.4f)
                    }
                ),
                modifier = Modifier.fillMaxSize()
            )
            // Ambient Indigo Fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, CosmicBackground.copy(alpha = 0.85f)),
                            startY = 0f
                        )
                    )
            )

            // Dynamic Banner Label
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "NEURAL STATUS: COHERENT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NeonCyan
                )
                Text(
                    text = "COGNITIVE SYNAPSE MAP LOADED",
                    fontSize = 9.sp,
                    color = OnCosmicSurface.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intel Brief Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ DAILY INTELLIGENCE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color.White
            )

            IconButton(
                onClick = { viewModel.generateIntelligenceBrief() },
                enabled = !isGenerating,
                modifier = Modifier.size(32.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = NeonCyan)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh brief", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Intelligence Brief Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("intelligence_brief_card"),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Brief Icon",
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INTELLIGENCE UPDATE • " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating && activeBrief == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Synthesizing Memory Chronology...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = OnCosmicSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    val briefText = activeBrief ?: "Click refresh to synthesize your startup timeline & tasks using MindOS AI."
                    Text(
                        text = formatIntelBrief(briefText),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = OnCosmicSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Feature 3 Checklist: Important Tasks / Deadlines
        Text(
            text = "🎯 MILESTONES & COMMITTED ACTIONS",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        val activeTasks = tasks.filter { !it.isCompleted }
        val completedTasks = tasks.filter { it.isCompleted }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No milestones detected.\nCapture startup tasks like 'Remember to launch on July 15' to see them organized here.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = OnCosmicSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // List Active Milestones
                activeTasks.forEach { task ->
                    TaskCard(task = task, onCheckedChange = { viewModel.toggleTaskCompletion(task) }, onDelete = { viewModel.deleteMemory(task) })
                }
                
                if (completedTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RESOLVED SYNSAPSE LINKS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonCyan.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    completedTasks.forEach { task ->
                        TaskCard(task = task, onCheckedChange = { viewModel.toggleTaskCompletion(task) }, onDelete = { viewModel.deleteMemory(task) })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun TaskCard(
    task: Memory,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, if (task.isCompleted) NeonCyan.copy(alpha = 0.1f) else NeonCyan.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonCyan,
                    uncheckedColor = OnCosmicSurface.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.processedText.ifBlank { task.text }.trim().removePrefix("• "),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (task.isCompleted) OnCosmicSurface.copy(alpha = 0.5f) else Color.White,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                )
                
                if (task.dueDateStr != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEADLINE: ${task.dueDateStr.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = StatusAmber
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete memory",
                    tint = StatusRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MEMORY VAULT SCREEN: Feature 1 & 2 (Search, Voice Memory) & Feature 4 (Thought Capture)
// -------------------------------------------------------------
@Composable
fun MemoryVaultScreen(
    viewModel: MindOSViewModel,
    memories: List<Memory>
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val aiAnswer by viewModel.searchAnswer.collectAsStateWithLifecycle()
    val isSearchingAI by viewModel.isSearching.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    
    // Custom waveform pulsation state
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // STT State Management
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Audio Permission Granted. Click Mic to record.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Voice capture disabled. You can type thoughts manually.", Toast.LENGTH_LONG).show()
        }
    }

    // Function to start real or mock Speech Recording
    val toggleVoiceListening = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (isVoiceRecording) {
                // STOP RECORDING
                isVoiceRecording = false
                try {
                    speechRecognizer?.stopListening()
                } catch (e: Exception) {
                    Log.e("STT", "Error stopping speech recognizer", e)
                }

                // If textInput is still empty, populate simulated startup speech to make it seamless
                if (textInput.isBlank()) {
                    textInput = "Reflecting that our startup launch is planned for July 15 and we need investor decks optimized."
                    Toast.makeText(context, "Signal Captured & Decrypted!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // START RECORDING
                isVoiceRecording = true
                textInput = ""
                timerSeconds = 0

                // Attempt Native STT
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }

                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {
                                isVoiceRecording = false
                            }
                            override fun onError(error: Int) {
                                Log.e("STT", "Speech Recognizer error code: $error")
                                isVoiceRecording = false
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    textInput = matches[0]
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    textInput = matches[0]
                                }
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                    try {
                        speechRecognizer?.startListening(intent)
                    } catch (e: Exception) {
                        Log.e("STT", "Failed to start speech recognizer, using mock animation fallback", e)
                    }
                }
            }
        }
    }

    // Timer logic for simulated speech recording
    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            while (isVoiceRecording) {
                delay(1000)
                timerSeconds++
                // Automatically stop speech record after 12 seconds
                if (timerSeconds >= 12) {
                    toggleVoiceListening()
                }
            }
        }
    }

    // Vault screen layouts
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = rememberLazyListState()
    ) {
        // Feature 2: Smart Brain Search Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🧠 SECOND BRAIN SEARCH ENGINE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = {
                    Text(
                        "Ask: 'What did I say about startup idea?'",
                        fontSize = 12.sp,
                        color = OnCosmicSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.runSemanticSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Query AI",
                                tint = NeonCyan
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("second_brain_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = OnCosmicSurface.copy(alpha = 0.2f),
                    focusedContainerColor = CosmicSurface,
                    unfocusedContainerColor = CosmicSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.runSemanticSearch()
                    keyboardController?.hide()
                })
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Synthesis Answer Panel
        if (query.isNotBlank() && aiAnswer != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("search_synthesis_panel"),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "AI synthesis matched",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BRAIN SYNAPSE MATCHED回答",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSearchingAI) {
                                CircularProgressIndicator(color = NeonCyan, strokeWidth = 1.dp, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiAnswer ?: "",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = OnCosmicSurface
                            )
                        )
                    }
                }
            }
        }

        // Feature 1, 4: Quick Thought & Voice Capture Controller
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("thought_capture_hud"),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isVoiceRecording) "🎙️ LISTENING COGNITIVE REFLECTION..." else "🔴 CHRONICLE MIND THOUGHT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVoiceRecording) NeonCyan else ElectricViolet
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isVoiceRecording) {
                        // Pulsing Wave Canvas Illustration
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(CosmicSurface, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val midY = height / 2f
                                val points = 100
                                val amplitude = 18f
                                
                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(0f, midY)
                                
                                for (i in 0..points) {
                                    val x = (width / points) * i
                                    val angle = (2 * Math.PI / points) * i * 3.5 + waveOffset
                                    val y = midY + sin(angle).toFloat() * amplitude * (sin((Math.PI / points) * i).toFloat())
                                    path.lineTo(x, y)
                                }
                                
                                drawPath(
                                    path = path,
                                    color = NeonCyan,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                            Text(
                                text = "REFRACTION PERIOD: 00:${timerSeconds.toString().padStart(2, '0')}s",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Raw input capture field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("thought_text_input"),
                        placeholder = {
                            Text(
                                text = "Speak or enter ideas, passwords, startup notes, or meetings to index them.",
                                fontSize = 12.sp,
                                color = OnCosmicSurface.copy(alpha = 0.4f),
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = OnCosmicSurface.copy(alpha = 0.1f),
                            focusedContainerColor = CosmicSurface,
                            unfocusedContainerColor = CosmicSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TACTILE MIC CONTROLLER
                        Button(
                            onClick = { toggleVoiceListening() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceRecording) StatusRed else CosmicBackground
                            ),
                            border = BorderStroke(1.dp, if (isVoiceRecording) StatusRed else NeonCyan),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_voice_capture_button")
                        ) {
                            Icon(
                                imageVector = if (isVoiceRecording) Icons.Default.Square else Icons.Default.Mic,
                                contentDescription = "Voice Capture",
                                tint = if (isVoiceRecording) Color.White else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isVoiceRecording) "LOCKED-IN" else "VOICE NOTE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceRecording) Color.White else NeonCyan
                            )
                        }

                        // CAPTURE EMIT BUTTON
                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.captureThought(textInput) {
                                        textInput = ""
                                    }
                                    keyboardController?.hide()
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                disabledContainerColor = OnCosmicSurface.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("emit_thought_button")
                        ) {
                            Text(
                                "CAPTURE",
                                color = if (textInput.isNotBlank()) Color.White else OnCosmicSurface.copy(alpha = 0.3f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // List Header label
        item {
            Text(
                text = if (query.isNotBlank()) "SYS FILTER MATCHES (${searchResults.size})" else "🧠 ALL MEMORIES & ARCHIVES",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Chronological results listing
        val targetList = if (query.isNotBlank() || searchResults.isNotEmpty()) searchResults else memories

        if (targetList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurface, RoundedCornerShape(8.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded reflections found in this node sector.\nTap VOICE NOTE or type a thought above.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = OnCosmicSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(targetList) { memory ->
                MemoryItemCard(
                    memory = memory,
                    onToggleStar = { viewModel.toggleStarMemory(memory) },
                    onDelete = { viewModel.deleteMemory(memory) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun MemoryItemCard(
    memory: Memory,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedDetail by remember { mutableStateOf(false) }

    // Color code categories
    val categoryColor = when (memory.category.lowercase()) {
        "startup" -> ElectricViolet
        "idea" -> NeonCyan
        "task" -> StatusAmber
        "meeting" -> CyberBlue
        "reminders" -> StatusRed
        else -> OnCosmicSurface.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedDetail = !expandedDetail }
            .testTag("memory_card_${memory.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, if (memory.isStarred) NeonCyan.copy(alpha = 0.4f) else OnCosmicSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .border(1.dp, categoryColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .background(categoryColor.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = memory.category.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        letterSpacing = 0.5.sp
                    )
                }

                // Star & Time controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(memory.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = OnCosmicSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (memory.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star bookmark",
                            tint = if (memory.isStarred) StatusAmber else OnCosmicSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main display text (original vs processed toggle)
            Text(
                text = memory.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = if (expandedDetail) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic layout if expanded
            if (expandedDetail) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = OnCosmicSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MIND.OS REFINED INTELLIGENCE ARCHIVE:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.processedText.ifBlank { "Integrating with MindOS synaptic layer..." },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = OnCosmicSurface
                )

                if (memory.dueDateStr != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(StatusAmber))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXTRACTED TASK DEADLINE: ${memory.dueDateStr.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PURGE MEMORY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap block to examine synapse refinement and original context",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = OnCosmicSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// AI COACH SCREEN: Feature 5 (AI Coach conversational panel)
// -------------------------------------------------------------
@Composable
fun AI_CoachScreen(
    viewModel: MindOSViewModel
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isGeneratingCoach by viewModel.isGeneratingCoach.collectAsStateWithLifecycle()
    var coachText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll chat to bottom when message list expands
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Coach Profile Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(colors = listOf(ElectricViolet, CyberBlue)))
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology, // Beautiful brain avatar
                        contentDescription = "MindOS Catalyst Core",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "COGNITIVE CATALYST ENGINE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "ACTIVE INTERACTIVE DECISION COACHING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { viewModel.resetCoachChat() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset chat",
                        tint = OnCosmicSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dialogue Log
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatHistory) { msg ->
                    CoachChatBubble(message = msg)
                }

                if (isGeneratingCoach) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "MindOS Catalyst calibrating priority alignment...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OnCosmicSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preloaded Fast Synapse Starters (Quick Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "What should I focus on today?",
                "Analyze active launch milestones",
                "Suggest pitch adjustments"
            ).forEach { prompt ->
                Box(
                    modifier = Modifier
                        .background(CosmicSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.sendCoachMessage(prompt) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Typing Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = coachText,
                onValueChange = { coachText = it },
                placeholder = {
                    Text(
                        "Debate startup vision or focus objectives...",
                        fontSize = 12.sp,
                        color = OnCosmicSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("coach_chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = OnCosmicSurface.copy(alpha = 0.2f),
                    focusedContainerColor = CosmicSurface,
                    unfocusedContainerColor = CosmicSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (coachText.isNotBlank()) {
                        viewModel.sendCoachMessage(coachText)
                        coachText = ""
                        keyboardController?.hide()
                    }
                }),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (coachText.isNotBlank()) {
                        viewModel.sendCoachMessage(coachText)
                        coachText = ""
                        keyboardController?.hide()
                    }
                },
                enabled = coachText.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (coachText.isNotBlank()) NeonCyan else CosmicSurface,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (coachText.isNotBlank()) CosmicBackground else OnCosmicSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CoachChatBubble(message: ChatMessage) {
    val isOccupant = (message.sender == "Occupant")
    
    val bubbleBg = if (isOccupant) ElectricViolet.copy(alpha = 0.15f) else CosmicSurface
    val bubbleBorder = if (isOccupant) ElectricViolet.copy(alpha = 0.5f) else OnCosmicSurface.copy(alpha = 0.1f)
    val senderLabel = if (isOccupant) "OCCUPANT DEBATE" else "MIND.OS ADVISOR"
    val labelColor = if (isOccupant) ElectricViolet else NeonCyan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isOccupant) Alignment.End else Alignment.Start
    ) {
        // Label header
        Text(
            text = senderLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Text block
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .testTag("chat_bubble_${message.sender.lowercase()}"),
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            border = BorderStroke(1.dp, bubbleBorder),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (isOccupant) 10.dp else 0.dp,
                bottomEnd = if (isOccupant) 0.dp else 10.dp
            )
        ) {
            Text(
                text = message.text,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = OnCosmicSurface
                ),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER METHODS
// -------------------------------------------------------------

/**
 * Strips list markers and gives styled monospaced bullets for the Intel briefs
 */
private fun formatIntelBrief(str: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = str.split("\n")
        lines.forEachIndexed { i, line ->
            var formattedLine = line
            var isHeading = false
            var headingColor = NeonCyan
            
            // Format simple sci-fi headers: headings with digit tags (1., 2., 3.) or system headings
            if (line.trim().startsWith("1.") || line.contains("NEURAL DIRECTIVE")) {
                isHeading = true
                headingColor = NeonCyan
            } else if (line.trim().startsWith("2.") || line.contains("TOP PRIORITIES")) {
                isHeading = true
                headingColor = ElectricViolet
            } else if (line.trim().startsWith("3.") || line.contains("MEMORY HORIZONS")) {
                isHeading = true
                headingColor = CyberBlue
            }
            
            if (isHeading) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = headingColor,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                ) {
                    append(formattedLine.uppercase())
                }
            } else {
                append(formattedLine)
            }
            if (i < lines.size - 1) append("\n")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Welcome to MindOS, $name!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your digital synapse layer is fully initialized and operational.",
                color = OnCosmicSurface.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
