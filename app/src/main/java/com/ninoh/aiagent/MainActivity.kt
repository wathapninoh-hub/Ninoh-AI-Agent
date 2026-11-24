package com.ninoh.aiagent

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private external fun nativeGenerate(modelPath: String, prompt: String, maxTokens: Int): String
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var modelStatus: TextView
    private var mode = "Chat"
    private var modelPath = ""
    private val prefs by lazy { getSharedPreferences("ninoh", MODE_PRIVATE) }

    companion object {
        private const val PICK_MODEL = 41
        init { System.loadLibrary("ninoh-native") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelPath = prefs.getString("model_path", "") ?: ""
        buildScreen()
        append("NINOH AI AGENT", "Tongasoa! App AI offline ity. Safidio aloha ny modely GGUF ao amin'ny Model.")
        refreshModel()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 18, 16, 12)
            setBackgroundColor(Color.rgb(12, 17, 29))
        }
        val title = TextView(this).apply {
            text = "NINOH AI AGENT"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 8, 4, 12)
        }
        root.addView(title)
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Chat", "Coding", "Rules", "Model").forEach { label ->
            nav.addView(Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener {
                    when (label) {
                        "Rules" -> editRules()
                        "Model" -> pickModel()
                        else -> { mode = label; append("MODE", "Mode $mode no voafidy.") }
                    }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        modelStatus = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(160, 190, 220))
            setPadding(4, 8, 4, 8)
        }
        root.addView(modelStatus)
        val scroll = ScrollView(this)
        transcript = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 18)
            text = ""
        }
        scroll.addView(transcript)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        input = EditText(this).apply {
            hint = "Soraty eto ny hafatra..."
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(31, 40, 57))
            minLines = 2
            maxLines = 5
            setPadding(12, 10, 12, 10)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2))
        val send = Button(this).apply {
            text = "Send"
            isAllCaps = false
            setOnClickListener { sendMessage() }
        }
        root.addView(send, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun append(who: String, message: String) {
        runOnUiThread {
            transcript.append("\n\n$who\n$message")
        }
    }

    private fun refreshModel() {
        modelStatus.text = if (modelPath.isBlank()) "Model: mbola tsy misy — tsindrio Model" else "Model: ${File(modelPath).name}"
    }

    private fun pickModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_MODEL)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_MODEL && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            worker.execute {
                try {
                    val name = "local-model.gguf"
                    val dest = File(filesDir, name)
                    contentResolver.openInputStream(uri).use { stream ->
                        requireNotNull(stream) { "Tsy afaka namaky ilay rakitra." }.copyTo(dest.outputStream())
                    }
                    if (dest.length() > 800L * 1024L * 1024L) {
                        dest.delete()
                        runOnUiThread {
                            Toast.makeText(this, "Mihoatra ny 800 MB ilay modely. Safidio ny GGUF kely kokoa.", Toast.LENGTH_LONG).show()
                        }
                        return@execute
                    }
                    modelPath = dest.absolutePath
                    prefs.edit().putString("model_path", modelPath).apply()
                    runOnUiThread {
                        refreshModel()
                        Toast.makeText(this, "Voaray ny modely: ${dest.length() / (1024 * 1024)} MB", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "Tsy tafiditra ilay modely: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun editRules() {
        val editor = EditText(this).apply {
            setText(prefs.getString("rules", "Valio amin'ny fomba mazava sy marina."))
            minLines = 4
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Custom Rules")
            .setMessage("Ireo toromarika ampidirinao dia ampidirina amin'ny prompt rehetra.")
            .setView(editor)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("rules", editor.text.toString()).apply()
                Toast.makeText(this, "Voatahiry ny règles.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        if (modelPath.isBlank() || !File(modelPath).exists()) {
            Toast.makeText(this, "Ampidiro aloha ny modely GGUF ao amin'ny Model.", Toast.LENGTH_LONG).show()
            return
        }
        input.setText("")
        append("IANAO", message)
        val rules = prefs.getString("rules", "") ?: ""
        val role = if (mode == "Coding") "You are a careful programming assistant. Explain code clearly and provide runnable examples." else "You are a helpful conversational assistant."
        val prompt = "$role\nUser rules: $rules\nUser: $message\nAssistant:"
        append("NINOH", "Miasa offline... mety haharitra arakaraka ny haben'ny modely sy ny finday.")
        worker.execute {
            val answer = try { nativeGenerate(modelPath, prompt, 256).ifBlank { "Tsy nisy valiny navoakan'ny modely." } }
            catch (e: Exception) { "Nisy olana tamin'ny modely: ${e.message}" }
            append("NINOH", answer)
        }
    }
}
