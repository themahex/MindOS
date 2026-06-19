package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.local.AppDatabase
import com.example.data.model.Memory
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val sender: String, // "Occupant" or "MindOS"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MindOSViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MindOSViewModel"
    private val repository: MemoryRepository

    // Database state flows
    val memories: StateFlow<List<Memory>>
    val tasks: StateFlow<List<Memory>>

    // Search States
    val searchQuery = MutableStateFlow("")
    val searchAnswer = MutableStateFlow<String?>(null)
    val isSearching = MutableStateFlow(false)

    private val _searchResults = MutableStateFlow<List<Memory>>(emptyList())
    val searchResults: StateFlow<List<Memory>> = _searchResults.asStateFlow()

    // Daily Intelligence Brief
    val activeBrief = MutableStateFlow<String?>(null)
    val isGeneratingBrief = MutableStateFlow(false)

    // Chat / AI Coach
    val chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isGeneratingCoach = MutableStateFlow(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MemoryRepository(database.memoryDao())

        // Connect room flow directly to UI state flows
        memories = repository.allMemories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Populate database with high-fidelity template entries if empty
        viewModelScope.launch {
            repository.allMemories.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedBaselineMemories()
                }
            }
            // Generate morning brief on start
            generateIntelligenceBrief()
            // Reset Chat History with welcome
            resetCoachChat()
        }

        // Setup reactive database search when query text changes
        viewModelScope.launch {
            searchQuery.debounce(300).collectLatest { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    searchAnswer.value = null
                } else {
                    repository.searchMemories(query).collect { results ->
                        _searchResults.value = results
                    }
                }
            }
        }
    }

    private suspend fun seedBaselineMemories() {
        val seedData = listOf(
            Memory(
                text = "Remember that our MindOS startup official launch date is on July 15, 2026.",
                processedText = "• MindOS System Launch officially planned for July 15, 2026.\n• Need to coordinate code consolidation and marketing collateral.",
                category = "Startup",
                isTask = true,
                dueDateStr = "July 15, 2026"
            ),
            Memory(
                text = "Idea: Position MindOS as 'Google for your own brain'. That is a highly marketable message that makes intuitive sense for BCI and general users alike, rather than a futuristic surgical device.",
                processedText = "Positioning Strategy:\n• Package MindOS as an accessible software extension ('Google for your own brain').\n• Focus heavily on cognitive backup, instant visual timeline recall, and AI synthesis.",
                category = "Idea"
            ),
            Memory(
                text = "Finalize the core high-throughput BCI signal processing specifications by next Monday.",
                processedText = "• Complete high-throughput BCI signal-relay protocol definition.\n• Deadline: Next Monday.",
                category = "Task",
                isTask = true,
                dueDateStr = "Next Monday"
            ),
            Memory(
                text = "Pitch presentation schedule: Meeting with Lead Investor Clara on June 24 at 10:00 AM. Bring updated MVP usage telemetry screens.",
                processedText = "• Venture Presentation: Clara (Lead Investor)\n• Time: June 24, 10:00 AM\n• Deliverables: High-fidelity telemetry and usage feedback logs.",
                category = "Meeting"
            )
        )
        for (memory in seedData) {
            repository.insertMemory(memory)
        }
    }

    /**
     * Captures a raw thought, processes it using Gemini, and saves it to Room.
     */
    fun captureThought(rawText: String, onComplete: () -> Unit = {}) {
        if (rawText.isBlank()) return

        viewModelScope.launch {
            try {
                // Generate a placeholder first to show instantaneous local UI updates
                val tempMemory = Memory(
                    text = rawText,
                    processedText = "Analyzing thought patterns via MindOS system...",
                    category = "General"
                )
                val insertedId = repository.insertMemory(tempMemory)

                // Call Gemini for structured AI analysis
                val jsonResult = GeminiClient.parseThought(rawText)

                val category = jsonResult.optString("category", "General")
                val processedText = jsonResult.optString("processedText", rawText)
                val isTask = jsonResult.optBoolean("isTask", false)
                val dueDateStr = jsonResult.optString("dueDateStr", "").let {
                    if (it == "null" || it.isBlank()) null else it
                }

                val finalMemory = Memory(
                    id = insertedId,
                    text = rawText,
                    processedText = processedText,
                    category = category,
                    isTask = isTask,
                    dueDateStr = dueDateStr,
                    timestamp = System.currentTimeMillis()
                )

                repository.updateMemory(finalMemory)

                // Regenerate Daily Brief to include the new thought if relevant
                if (isTask) {
                    generateIntelligenceBrief()
                }
                
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed capturing thought: ${e.message}", e)
            }
        }
    }

    /**
     * Executes AI Semantic Search querying all database memories
     */
    fun runSemanticSearch() {
        val query = searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            isSearching.value = true
            searchAnswer.value = "Scanning neural synapses and searching your Second Brain memory bank..."

            try {
                val activeMemories = memories.value
                if (activeMemories.isEmpty()) {
                    searchAnswer.value = "Your memory bank is empty. MindOS has no data to search. Capture text or voice notes first!"
                    isSearching.value = false
                    return@launch
                }

                // Compile memories string
                val memoriesContext = activeMemories.joinToString("\n---\n") { mem ->
                    "ID: ${mem.id}\nCategory: ${mem.category}\nDate: ${formatDate(mem.timestamp)}\nOriginal Speech/Text: ${mem.text}\nAI Summary: ${mem.processedText}\nTask? ${if (mem.isTask) "Yes, Due: " + (mem.dueDateStr ?: "N/A") else "No"}"
                }

                val prompt = """
                    The user is asking: "$query"
                    
                    Search and analyze all user memories stored in their MindOS database below. 
                    Synthesize an intelligent response addressing their question directly. 
                    Be accurate and specify what date or memory you found this from.
                    If the answer isn't in their memories, state that honestly based on the database content, but suggest relevant information.
                    
                    Memory Database:
                    $memoriesContext
                """.trimIndent()

                val systemPrompt = "You are MindOS, the neural software layer of the user's brain extension. Keep your answer highly intelligent, sci-fi concise, and incredibly supportive."
                val response = GeminiClient.generate(prompt = prompt, systemInstruction = systemPrompt)
                searchAnswer.value = response
            } catch (e: Exception) {
                searchAnswer.value = "Synaptic connection timed out: ${e.message}"
            } finally {
                isSearching.value = false
            }
        }
    }

    /**
     * Compiles and generates the Daily Intelligence Brief using Room contents + Gemini
     */
    fun generateIntelligenceBrief() {
        viewModelScope.launch {
            isGeneratingBrief.value = true
            try {
                val activeMemories = memories.value
                val activeTasks = tasks.value.filter { !it.isCompleted }

                if (activeMemories.isEmpty()) {
                    activeBrief.value = "🔴 SYSTEM INITIALIZED\nMemory database is empty. Feed MindOS voice reflections or capture thoughts to construct your morning intelligence briefing."
                    isGeneratingBrief.value = false
                    return@launch
                }

                val tasksStr = activeTasks.joinToString("\n") {
                    "• ${it.processedText.replace("\n", " ").trim()} (Due: ${it.dueDateStr ?: "General"})"
                }

                val memoriesStr = activeMemories.take(10).joinToString("\n") {
                    "[${it.category}] ${it.text}"
                }

                val prompt = """
                    Compile a customized 'Daily Intelligence Brief' for the user of MindOS.
                    Here is their current state:
                    
                    Active Tasks & Deadlines:
                    $tasksStr
                    
                    Recent Brain Dumps & Reflections:
                    $memoriesStr
                    
                    Compose a highly stimulating, sleek morning directive. Group it into 3 clear headings:
                    1. 🔋 NEURAL DIRECTIVE (Sleek headline motivation based on goals and deadlines)
                    2. 🎯 TOP PRIORITIES (2 or 3 critical items that need focus today, tying startup milestones)
                    3. 🔮 MEMORY HORIZONS (An encouraging tip or reminder extracted from their older reflections)
                    
                    Keep the style extremely executive, smart (BCI software theme), encouraging, and clear. Do not use markdown bullet blocks * or -, use high-density professional prose.
                """.trimIndent()

                val systemInstruct = "You are MindOS Intelligence Directive Suite. You analyze cognitive timelines and give futuristic daily briefings."
                val result = GeminiClient.generate(prompt = prompt, systemInstruction = systemInstruct)
                activeBrief.value = result
            } catch (e: Exception) {
                activeBrief.value = "Brief Generation Timeout: ${e.message}"
            } finally {
                isGeneratingBrief.value = false
            }
        }
    }

    /**
     * Appends a message and queries the AI Coach for priorities
     */
    fun sendCoachMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(sender = "Occupant", text = text)
        chatHistory.value = chatHistory.value + userMsg

        viewModelScope.launch {
            isGeneratingCoach.value = true
            try {
                val activeMemories = memories.value
                val fullChatHistory = chatHistory.value

                // Inject database context into the prompt
                val databaseBrief = activeMemories.joinToString("\n") { mem ->
                    "-[Category: ${mem.category}] ${mem.text} (${if (mem.isTask) "Task, Due: " + (mem.dueDateStr ?: "N/A") else "Ref" })"
                }

                // Compile conversations
                val conversationStr = fullChatHistory.joinToString("\n") {
                    "${it.sender}: ${it.text}"
                }

                val prompt = """
                    You are the AI Mind Coach inside MindOS, helping the occupant prioritize their day, synthesize business strategies, and avoid forgetting startup goals.
                    
                    User's Entire Memory Database context:
                    $databaseBrief
                    
                    Active Chat Dialogue:
                    $conversationStr
                    
                    Give a highly engaging, cybernetic coaching response. Direct the occupant's attention to their active commitments, challenge them to execute startup milestones, and provide creative strategies.
                    Keep responses professional, inspiring, and concise (under 2-3 paragraphs). Avoid generic advice.
                """.trimIndent()

                val systemPrompt = "You are the MindOS AI Cognitive Catalyst (AI Coach). You use precise cybernetic and tactical analogies. Help the occupant achieve greatness."
                val responseText = GeminiClient.generate(prompt = prompt, systemInstruction = systemPrompt)

                val coachMsg = ChatMessage(sender = "MindOS", text = responseText)
                chatHistory.value = chatHistory.value + coachMsg
            } catch (e: Exception) {
                ToastMessage("Coaching communication drop-out: ${e.message}")
            } finally {
                isGeneratingCoach.value = false
            }
        }
    }

    fun resetCoachChat() {
        chatHistory.value = listOf(
            ChatMessage(
                sender = "MindOS",
                text = "⚡ Cognitive Catalyst Active.\n\nWelcome to your AI Coach portal, Occupant. I have analyzed your 30-day milestones and active targets. Ask me where to focalize your mind today, or let us debate startup launch strategies."
            )
        )
    }

    private fun ToastMessage(text: String) {
        chatHistory.value = chatHistory.value + ChatMessage(sender = "MindOS", text = "⚠️ $text")
    }

    // Toggle Checkboxes for tasks
    fun toggleTaskCompletion(memory: Memory) {
        viewModelScope.launch {
            val updated = memory.copy(isCompleted = !memory.isCompleted)
            repository.updateMemory(updated)
            // Gently regenerate parameters
            generateIntelligenceBrief()
        }
    }

    // Highlight or star a memory
    fun toggleStarMemory(memory: Memory) {
        viewModelScope.launch {
            val updated = memory.copy(isStarred = !memory.isStarred)
            repository.updateMemory(updated)
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
            generateIntelligenceBrief()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAll()
            seedBaselineMemories()
            generateIntelligenceBrief()
            resetCoachChat()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}
