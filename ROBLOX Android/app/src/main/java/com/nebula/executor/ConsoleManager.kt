package com.nebula.executor

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConsoleManager {

    private val lines = mutableListOf<ConsoleLine>()
    private const val MAX_LINES = 500
    private var showTimestamps = true

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    // Colors (ARGB)
    private const val COLOR_INFO = 0xFF00FF41.toInt()    // green
    private const val COLOR_ERROR = 0xFFFF5252.toInt()   // red
    private const val COLOR_WARN = 0xFFFFAB40.toInt()    // yellow
    private const val COLOR_EXEC = 0xFFCE93D8.toInt()    // purple
    private const val COLOR_DEFAULT = 0xFFCCCCCC.toInt() // light gray

    data class ConsoleLine(
        val text: String,
        val level: Level,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class Level { INFO, ERROR, WARN, EXEC }

    fun appendInfo(message: String) {
        addLine(message, Level.INFO)
    }

    fun appendError(message: String) {
        addLine(message, Level.ERROR)
    }

    fun appendWarn(message: String) {
        addLine(message, Level.WARN)
    }

    fun appendExec(message: String) {
        addLine(message, Level.EXEC)
    }

    private fun addLine(message: String, level: Level) {
        lines.add(ConsoleLine(message, level))
        while (lines.size > MAX_LINES) {
            lines.removeAt(0)
        }
    }

    fun clear() {
        lines.clear()
    }

    fun setShowTimestamps(show: Boolean) {
        showTimestamps = show
    }

    fun getFormattedOutput(): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        for ((index, line) in lines.withIndex()) {
            val prefix = when (line.level) {
                Level.INFO -> "[INFO]"
                Level.ERROR -> "[ERROR]"
                Level.WARN -> "[WARN]"
                Level.EXEC -> "[EXEC]"
            }

            val timestamp = if (showTimestamps) {
                "[${dateFormat.format(Date(line.timestamp))}] "
            } else {
                ""
            }

            val fullLine = "$timestamp$prefix ${line.text}"
            val start = builder.length
            builder.append(fullLine)

            val color = when (line.level) {
                Level.INFO -> COLOR_INFO
                Level.ERROR -> COLOR_ERROR
                Level.WARN -> COLOR_WARN
                Level.EXEC -> COLOR_EXEC
            }

            builder.setSpan(
                ForegroundColorSpan(color),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }

        if (builder.isEmpty()) {
            builder.append("[INFO] Nebula Executor v1.0.0 initialized\n[INFO] Awaiting script input…")
            builder.setSpan(
                ForegroundColorSpan(COLOR_INFO),
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    fun getLineCount(): Int = lines.size
}
