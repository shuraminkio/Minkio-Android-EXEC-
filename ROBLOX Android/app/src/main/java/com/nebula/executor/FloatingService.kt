package com.nebula.executor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var scriptManager: ScriptManager
    private lateinit var syntaxHighlighter: SyntaxHighlighter

    private var fabView: View? = null
    private var panelView: View? = null
    private var isPanelVisible = false

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val CHANNEL_ID = "nebula_executor_channel"
        private const val NOTIFICATION_ID = 9001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)
        scriptManager = ScriptManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        createFab()

        // Attempt bridge connection
        ScriptEngine.connect { msg ->
            handler.post {
                ConsoleManager.appendInfo(msg)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syntaxHighlighter.detach()
        removeFab()
        removePanel()
        ScriptEngine.disconnect()
    }

    // ─────────────────────────────────────────────────
    // NOTIFICATION
    // ─────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Nebula Executor")
                .setContentText("Overlay is active")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Nebula Executor")
                .setContentText("Overlay is active")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    // ─────────────────────────────────────────────────
    // FLOATING ACTION BUTTON (FAB)
    // ─────────────────────────────────────────────────

    private fun createFab() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        fabView = inflater.inflate(R.layout.layout_floating_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        setupFabTouchListener(params)
        windowManager.addView(fabView, params)
    }

    private fun setupFabTouchListener(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        fabView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 25) {
                        isMoved = true
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(fabView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        togglePanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFab() {
        fabView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        fabView = null
    }

    // ─────────────────────────────────────────────────
    // EXECUTOR PANEL
    // ─────────────────────────────────────────────────

    private fun togglePanel() {
        if (isPanelVisible) {
            removePanel()
        } else {
            createPanel()
        }
    }

    private fun createPanel() {
        if (panelView != null) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        panelView = inflater.inflate(R.layout.layout_executor_panel, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(340),
            dpToPx(480),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        setupPanel(params)
        windowManager.addView(panelView, params)
        isPanelVisible = true
    }

    private fun removePanel() {
        panelView?.let {
            if (::syntaxHighlighter.isInitialized) {
                syntaxHighlighter.detach()
            }
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
        isPanelVisible = false
    }

    private fun setupPanel(params: WindowManager.LayoutParams) {
        val panel = panelView ?: return

        // ── Drag bar ──
        setupPanelDrag(panel.findViewById(R.id.topBar), params)

        // ── Close / Minimize ──
        panel.findViewById<TextView>(R.id.btnClose).setOnClickListener {
            removePanel()
            removeFab()
            stopSelf()
        }

        panel.findViewById<TextView>(R.id.btnMinimize).setOnClickListener {
            removePanel()
        }

        // ── Tabs ──
        val tabEditor = panel.findViewById<TextView>(R.id.tabEditor)
        val tabScripts = panel.findViewById<TextView>(R.id.tabScripts)
        val tabConsole = panel.findViewById<TextView>(R.id.tabConsole)
        val tabSettings = panel.findViewById<TextView>(R.id.tabSettings)

        val editorPanel = panel.findViewById<View>(R.id.editorPanel)
        val scriptsPanel = panel.findViewById<View>(R.id.scriptsPanel)
        val consolePanel = panel.findViewById<View>(R.id.consolePanel)
        val settingsPanel = panel.findViewById<View>(R.id.settingsPanel)

        val tabs = listOf(tabEditor, tabScripts, tabConsole, tabSettings)
        val panels = listOf(editorPanel, scriptsPanel, consolePanel, settingsPanel)

        fun selectTab(index: Int) {
            tabs.forEachIndexed { i, tab ->
                tab.isSelected = i == index
            }
            panels.forEachIndexed { i, p ->
                p.visibility = if (i == index) View.VISIBLE else View.GONE
            }

            // Refresh console when switching to it
            if (index == 2) {
                refreshConsole(panel)
            }
            // Refresh scripts when switching to scripts tab
            if (index == 1) {
                refreshScriptList(panel)
            }
        }

        tabEditor.setOnClickListener { selectTab(0) }
        tabScripts.setOnClickListener { selectTab(1) }
        tabConsole.setOnClickListener { selectTab(2) }
        tabSettings.setOnClickListener { selectTab(3) }

        // Start on Editor tab
        selectTab(0)

        // ── Editor ──
        val etScript = panel.findViewById<EditText>(R.id.etScript)

        // Syntax highlighting
        syntaxHighlighter = SyntaxHighlighter(etScript)
        syntaxHighlighter.attach()

        // Apply font size from settings
        etScript.textSize = settingsManager.fontSize.toFloat()

        // ── Action buttons ──
        panel.findViewById<TextView>(R.id.btnExecute).setOnClickListener {
            val script = etScript.text.toString()
            if (script.isBlank()) {
                Toast.makeText(this, getString(R.string.no_script), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ConsoleManager.appendExec("Executing script…")
            ScriptEngine.execute(script) { result ->
                handler.post {
                    ConsoleManager.appendInfo(result)
                    refreshConsole(panel)
                }
            }
            Toast.makeText(this, "Script executed", Toast.LENGTH_SHORT).show()
        }

        panel.findViewById<TextView>(R.id.btnClear).setOnClickListener {
            etScript.text.clear()
            Toast.makeText(this, getString(R.string.cleared), Toast.LENGTH_SHORT).show()
        }

        panel.findViewById<TextView>(R.id.btnCopy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("script", etScript.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }

        panel.findViewById<TextView>(R.id.btnPaste).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val pasteData = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (pasteData.isNotEmpty()) {
                etScript.append(pasteData)
                Toast.makeText(this, getString(R.string.pasted), Toast.LENGTH_SHORT).show()
            }
        }

        // ── Console ──
        panel.findViewById<TextView>(R.id.btnClearConsole).setOnClickListener {
            ConsoleManager.clear()
            refreshConsole(panel)
        }

        // ── Scripts RecyclerView ──
        setupScriptsList(panel, etScript)

        // ── Settings ──
        setupSettings(panel, etScript)

        // Refresh console with initial data
        refreshConsole(panel)
    }

    private fun setupPanelDrag(dragView: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(panelView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshConsole(panel: View) {
        val tvConsole = panel.findViewById<TextView>(R.id.tvConsole)
        val scrollView = panel.findViewById<ScrollView>(R.id.consoleScroll)
        ConsoleManager.setShowTimestamps(settingsManager.showTimestamps)
        tvConsole.text = ConsoleManager.getFormattedOutput()
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setupScriptsList(panel: View, etScript: EditText) {
        val rvScripts = panel.findViewById<RecyclerView>(R.id.rvScripts)
        rvScripts.layoutManager = LinearLayoutManager(this)

        val adapter = ScriptAdapter(
            scripts = scriptManager.getAllScripts(),
            onLoadClick = { script ->
                etScript.setText(script.code)
                // Switch to editor tab
                panel.findViewById<TextView>(R.id.tabEditor).performClick()
                Toast.makeText(this, getString(R.string.script_loaded), Toast.LENGTH_SHORT).show()
            },
            onExecuteClick = { script ->
                ConsoleManager.appendExec("Executing: ${script.name}")
                ScriptEngine.execute(script.code) { result ->
                    handler.post {
                        ConsoleManager.appendInfo(result)
                        refreshConsole(panel)
                    }
                }
                Toast.makeText(this, "Executing: ${script.name}", Toast.LENGTH_SHORT).show()
            }
        )
        rvScripts.adapter = adapter
    }

    private fun refreshScriptList(panel: View) {
        val rvScripts = panel.findViewById<RecyclerView>(R.id.rvScripts)
        (rvScripts.adapter as? ScriptAdapter)?.updateScripts(scriptManager.getAllScripts())
    }

    private fun setupSettings(panel: View, etScript: EditText) {
        val switchAutoExec = panel.findViewById<SwitchMaterial>(R.id.switchAutoExec)
        val switchTimestamps = panel.findViewById<SwitchMaterial>(R.id.switchTimestamps)
        val switchAutoInject = panel.findViewById<SwitchMaterial>(R.id.switchAutoInject)
        val switchTopmost = panel.findViewById<SwitchMaterial>(R.id.switchTopmost)
        val tvFontSize = panel.findViewById<TextView>(R.id.tvFontSize)

        // Load current settings
        switchAutoExec.isChecked = settingsManager.autoExecute
        switchTimestamps.isChecked = settingsManager.showTimestamps
        switchAutoInject.isChecked = settingsManager.autoInject
        switchTopmost.isChecked = settingsManager.topmost
        tvFontSize.text = settingsManager.fontSize.toString()

        switchAutoExec.setOnCheckedChangeListener { _, checked ->
            settingsManager.autoExecute = checked
        }

        switchTimestamps.setOnCheckedChangeListener { _, checked ->
            settingsManager.showTimestamps = checked
            ConsoleManager.setShowTimestamps(checked)
        }

        switchAutoInject.setOnCheckedChangeListener { _, checked ->
            settingsManager.autoInject = checked
        }

        switchTopmost.setOnCheckedChangeListener { _, checked ->
            settingsManager.topmost = checked
        }

        // Font size tap to cycle: 11 -> 13 -> 15 -> 17 -> 11
        tvFontSize.setOnClickListener {
            val sizes = listOf(11, 13, 15, 17)
            val current = settingsManager.fontSize
            val nextIndex = (sizes.indexOf(current) + 1) % sizes.size
            val newSize = sizes[nextIndex]
            settingsManager.fontSize = newSize
            tvFontSize.text = newSize.toString()
            etScript.textSize = newSize.toFloat()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
