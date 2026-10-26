package com.nebula.executor

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import java.util.regex.Pattern

class SyntaxHighlighter(private val editText: EditText) {

    companion object {
        // Colors
        private const val COLOR_KEYWORD = 0xFFBB86FC.toInt()    // purple
        private const val COLOR_STRING = 0xFF00E676.toInt()     // green
        private const val COLOR_NUMBER = 0xFFFFAB40.toInt()     // orange
        private const val COLOR_COMMENT = 0xFF6A6A8A.toInt()    // gray
        private const val COLOR_ROBLOX = 0xFF18FFFF.toInt()     // cyan
        private const val COLOR_BUILTIN = 0xFF64B5F6.toInt()    // blue
        private const val COLOR_DEFAULT = 0xFF00FF41.toInt()    // green (default text)
        private const val COLOR_BOOLEAN = 0xFFFF8A65.toInt()    // warm orange

        private const val DEBOUNCE_MS = 300L

        // Lua keywords
        private val KEYWORDS = listOf(
            "local", "function", "end", "if", "then", "else", "elseif",
            "for", "while", "do", "return", "repeat", "until", "in",
            "not", "and", "or", "break", "goto"
        )

        // Lua boolean/nil literals
        private val LITERALS = listOf("true", "false", "nil")

        // Roblox globals and common APIs
        private val ROBLOX_GLOBALS = listOf(
            "game", "workspace", "Instance", "Players", "LocalPlayer",
            "Character", "Humanoid", "CFrame", "Vector3", "Vector2",
            "Color3", "UDim2", "UDim", "BrickColor", "Enum",
            "HumanoidRootPart", "GetService", "FindFirstChild",
            "WaitForChild", "GetChildren", "GetDescendants",
            "IsA", "Clone", "Destroy", "Parent", "Name",
            "RunService", "UserInputService", "ReplicatedStorage",
            "ServerStorage", "ServerScriptService", "StarterGui",
            "CoreGui", "TweenService", "Debris", "HttpService",
            "MarketplaceService", "DataStoreService",
            "math", "string", "table", "pairs", "ipairs",
            "print", "warn", "error", "wait", "spawn", "delay",
            "require", "typeof", "type", "tostring", "tonumber",
            "pcall", "xpcall", "select", "unpack", "rawget", "rawset",
            "setmetatable", "getmetatable", "coroutine", "tick", "time",
            "loadstring", "newproxy"
        )

        // Patterns
        private val PATTERN_MULTILINE_COMMENT = Pattern.compile("--\\[\\[.*?\\]\\]", Pattern.DOTALL)
        private val PATTERN_SINGLE_COMMENT = Pattern.compile("--[^\n]*")
        private val PATTERN_DOUBLE_STRING = Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
        private val PATTERN_SINGLE_STRING = Pattern.compile("'[^'\\\\]*(\\\\.[^'\\\\]*)*'")
        private val PATTERN_MULTILINE_STRING = Pattern.compile("\\[\\[.*?\\]\\]", Pattern.DOTALL)
        private val PATTERN_NUMBER = Pattern.compile("\\b\\d+\\.?\\d*\\b")
        private val PATTERN_HEX = Pattern.compile("\\b0x[0-9a-fA-F]+\\b")

        private fun buildWordPattern(words: List<String>): Pattern {
            val joined = words.joinToString("|") { Pattern.quote(it) }
            return Pattern.compile("\\b($joined)\\b")
        }

        private val PATTERN_KEYWORDS = buildWordPattern(KEYWORDS)
        private val PATTERN_LITERALS = buildWordPattern(LITERALS)
        private val PATTERN_ROBLOX = buildWordPattern(ROBLOX_GLOBALS)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null
    private var isHighlighting = false

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isHighlighting) return
            scheduleHighlight()
        }
    }

    fun attach() {
        editText.addTextChangedListener(textWatcher)
    }

    fun detach() {
        editText.removeTextChangedListener(textWatcher)
        highlightRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun scheduleHighlight() {
        highlightRunnable?.let { handler.removeCallbacks(it) }
        highlightRunnable = Runnable { applyHighlighting() }
        handler.postDelayed(highlightRunnable!!, DEBOUNCE_MS)
    }

    private fun applyHighlighting() {
        val editable = editText.text ?: return
        if (editable.isEmpty()) return

        isHighlighting = true

        try {
            val text = editable.toString()

            // Clear existing color spans
            val existingSpans = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
            for (span in existingSpans) {
                editable.removeSpan(span)
            }

            // Set default color for entire text
            editable.setSpan(
                ForegroundColorSpan(COLOR_DEFAULT),
                0, editable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Track highlighted regions so we don't highlight inside strings/comments
            val highlighted = BooleanArray(text.length)

            // 1. Multi-line comments --[[ ]]
            applyPattern(editable, text, PATTERN_MULTILINE_COMMENT, COLOR_COMMENT, highlighted, true)

            // 2. Single-line comments --
            applyPattern(editable, text, PATTERN_SINGLE_COMMENT, COLOR_COMMENT, highlighted, true)

            // 3. Multi-line strings [[ ]]
            applyPattern(editable, text, PATTERN_MULTILINE_STRING, COLOR_STRING, highlighted, true)

            // 4. Double-quoted strings
            applyPattern(editable, text, PATTERN_DOUBLE_STRING, COLOR_STRING, highlighted, true)

            // 5. Single-quoted strings
            applyPattern(editable, text, PATTERN_SINGLE_STRING, COLOR_STRING, highlighted, true)

            // 6. Hex numbers
            applyPattern(editable, text, PATTERN_HEX, COLOR_NUMBER, highlighted, false)

            // 7. Numbers
            applyPattern(editable, text, PATTERN_NUMBER, COLOR_NUMBER, highlighted, false)

            // 8. Keywords
            applyPattern(editable, text, PATTERN_KEYWORDS, COLOR_KEYWORD, highlighted, false)

            // 9. Booleans / nil
            applyPattern(editable, text, PATTERN_LITERALS, COLOR_BOOLEAN, highlighted, false)

            // 10. Roblox globals
            applyPattern(editable, text, PATTERN_ROBLOX, COLOR_ROBLOX, highlighted, false)

        } finally {
            isHighlighting = false
        }
    }

    private fun applyPattern(
        editable: Editable,
        text: String,
        pattern: Pattern,
        color: Int,
        highlighted: BooleanArray,
        markRegion: Boolean
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Skip if any part of this match is already highlighted (inside string/comment)
            if (!markRegion) {
                var overlaps = false
                for (i in start until end) {
                    if (highlighted[i]) {
                        overlaps = true
                        break
                    }
                }
                if (overlaps) continue
            }

            editable.setSpan(
                ForegroundColorSpan(color),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (markRegion) {
                for (i in start until end) {
                    if (i < highlighted.size) {
                        highlighted[i] = true
                    }
                }
            }
        }
    }
}
