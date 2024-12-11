package com.example.gemininanosample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemininanosample.ui.theme.GeminiNanoSampleTheme
import com.google.ai.edge.aicore.Candidate
import com.google.ai.edge.aicore.Content
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerateContentResponse
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.asTextOrNull
import com.google.ai.edge.aicore.content
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiNanoSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        initGenerativeModel()

        lifecycleScope.launch {
            viewModel.isGenerating.collect {
                if (it) {
                    generateContentStream(viewModel.inputText.value)
//                val generatedText = generateText(viewModel.inputText.value)
//                viewModel.addGeneratedText(generatedText)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        generativeModel.close()
    }

    private fun initGenerativeModel() {
        val generationConfig = generationConfig {
            context = applicationContext
            temperature = 0.2f
            topK = 16
            maxOutputTokens = Int.MAX_VALUE
        }
        val downloadCallback = object : DownloadCallback {
            override fun onDownloadDidNotStart(e: GenerativeAIException) {
                Log.d(TAG, "onDownloadDidNotStart: $e")
                super.onDownloadDidNotStart(e)
            }

            override fun onDownloadPending() {
                Log.d(TAG, "onDownloadPending")
                super.onDownloadPending()
            }

            override fun onDownloadStarted(bytesToDownload: Long) {
                Log.d(TAG, "onDownloadStarted: $bytesToDownload")
                super.onDownloadStarted(bytesToDownload)
            }

            override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
                Log.d(TAG, "onDownloadFailed: $failureStatus", e)
                super.onDownloadFailed(failureStatus, e)
            }

            override fun onDownloadProgress(totalBytesDownloaded: Long) {
                Log.d(TAG, "onDownloadProgress: $totalBytesDownloaded")
                super.onDownloadProgress(totalBytesDownloaded)
            }

            override fun onDownloadCompleted() {
                Log.d(TAG, "onDownloadCompleted")
                super.onDownloadCompleted()
            }
        }
        val downloadConfig = DownloadConfig(downloadCallback)
        generativeModel = GenerativeModel(
            generationConfig = generationConfig,
            downloadConfig = downloadConfig // optional
        )
    }

    private suspend fun generateText(input: String): String {

        Log.d(TAG, "Start generating. input text: $input")
        val response: GenerateContentResponse = generativeModel.generateContent(input)
        Log.d(TAG, "End generating. response text: ${response.text}")
        logResponse(response)

        return response.text ?: ""
    }

    private suspend fun generateContentStream(input: String) {
        Log.d(TAG, "Start Stream generating. input text: $input")
        generativeModel.generateContentStream(input)
            .onCompletion {
                viewModel.updateIsGenerating(false)
            }
            .collect { response ->
                viewModel.addGeneratedText(response.text)
//                logResponse(response)
            }
    }

    private fun logResponse(response: GenerateContentResponse) {
        Log.d(TAG, "response text: ${response.text}")
        response.candidates.forEach {
            val roleStr = when(it.content.role) {
                Content.Role.USER -> "USER"
                Content.Role.MODEL -> "MODEL"
                else -> it.content.role.toString()
            }
            val partStr = it.content.parts.joinToString(",") { part -> part.asTextOrNull() ?: ""}
            val reasonStr = when(it.finishReason) {
                Candidate.FinishReason.STOP -> "STOP"
                Candidate.FinishReason.MAX_TOKENS -> "MAX_TOKENS"
                Candidate.FinishReason.CUSTOM_STOP_TOKENS -> "CUSTOM_STOP_TOKENS"
                else -> it.finishReason.toString()
            }
            Log.d(TAG, """
                Candidate Object:
                  role=$roleStr
                  partStr=$partStr
                  reason=$reasonStr
            """.trimIndent())
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
) {
    val generatedText by mainViewModel.generatedText.collectAsState()
    val isGenerating by mainViewModel.isGenerating.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("日本の歴代首相は？") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Spacer(
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.systemBars
                )
            )
            Text(
                text = "文章を入力してください",
            )
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    mainViewModel.updateInputText(inputText)
                    mainViewModel.updateIsGenerating(true)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating
            ) {
                Text("生成する")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("回答")
                IconButton(onClick = {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("proofread_text", generatedText)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy", color = Color.Blue)
                }
            }
            Text(
                text = generatedText,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.systemBars
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GeminiNanoSampleTheme {
        Greeting("Android")
    }
}
