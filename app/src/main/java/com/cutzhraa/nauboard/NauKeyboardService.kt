package com.cutzhraa.nauboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class NauKeyboardService : InputMethodService(), View.OnClickListener {

    private var isCaps = false
    private var isSymbolMode = false
    private var isSecondarySymbolMode = false

    private var rootView: View? = null
    private var mainKeyboardLayout: LinearLayout? = null
    private var emojiPanelLayout: View? = null

    private var shiftBtn: Button? = null
    private val letterButtons = mutableListOf<Button>()
    
    private var suggest1: TextView? = null
    private var suggest2: TextView? = null
    private var suggest3: TextView? = null

    private val dictionary = listOf(
        "ada", "adalah", "akan", "aku", "atau", "bisa", "baik", "bukan",
        "dengan", "dan", "dia", "dari", "harus", "ini", "itu", "jika",
        "jadi", "juga", "karena", "kita", "kamu", "ke", "kalau", "lagi",
        "mereka", "makan", "mau", "masih", "oleh", "pada", "saya", "sudah",
        "sampai", "tidak", "tapi", "tentang", "untuk", "yang"
    )

    private val iosEmojis = listOf(
        "😂", "❤️", "😍", "😭", "😊", "🔥", "👍",
        "🥺", "🙏", "✨", "🥰", "💀", "😎", "🤣",
        "🙈", "🎉", "🤔", "😴", "💩", "🥳", "💯",
        "👋", "✌️", "🤍", "👀", "🙌", "💬", "📌"
    )
    
    private val letters: Array<String> = arrayOf(
        "q","w","e","r","t","y","u","i","o","p",
        "a","s","d","f","g","h","j","k","l",
        "z","x","c","v","b","n","m"
    )
    
    private val symbolsPage1: Array<String> = arrayOf(
        "1","2","3","4","5","6","7","8","9","0",
        "-","/",":",";","(",")","$","&","@",
        "\"", ".", ",", "?", "!", "'", " "
    )

    private val symbolsPage2: Array<String> = arrayOf(
        "[","]","{","}","#","%","^","*","+","=",
        "_","\\","|","~","<",">","€","£","¥","•",
        ".",",","?","!","'", " "
    )

    private val deleteHandler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                doDelete()
                deleteHandler.postDelayed(this, 50)
            }
        }
    }

    override fun onCreateInputView(): View {
        rootView = layoutInflater.inflate(R.layout.main_keyboard, null)
        rootView?.let {
            mainKeyboardLayout = it.findViewById(R.id.layout_main_keyboard)
            emojiPanelLayout = it.findViewById(R.id.layout_emoji_panel)
            setupKeyListeners(it)
            setupEmojiPanel(it)
        }
        updateKeyLabels()
        updateSuggestions("")
        return rootView!!
    }

    private fun setupKeyListeners(view: View) {
        letterButtons.clear()
        val letterIds = intArrayOf(
            R.id.btn_q, R.id.btn_w, R.id.btn_e, R.id.btn_r, R.id.btn_t,
            R.id.btn_y, R.id.btn_u, R.id.btn_i, R.id.btn_o, R.id.btn_p,
            R.id.btn_a, R.id.btn_s, R.id.btn_d, R.id.btn_f, R.id.btn_g,
            R.id.btn_h, R.id.btn_j, R.id.btn_k, R.id.btn_l, R.id.btn_z,
            R.id.btn_x, R.id.btn_c, R.id.btn_v, R.id.btn_b, R.id.btn_n, R.id.btn_m
        )

        for (id in letterIds) {
            view.findViewById<Button>(id)?.let {
                it.setOnClickListener(this)
                letterButtons.add(it)
            }
        }

        shiftBtn = view.findViewById(R.id.btn_shift)
        shiftBtn?.setOnClickListener(this)
        
        view.findViewById<Button>(R.id.btn_mode)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_space)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_enter)?.setOnClickListener(this)

        suggest1 = view.findViewById(R.id.suggest_1)
        suggest2 = view.findViewById(R.id.suggest_2)
        suggest3 = view.findViewById(R.id.suggest_3)

        suggest1?.setOnClickListener { insertSuggestedWord(suggest1?.text.toString()) }
        suggest2?.setOnClickListener { insertSuggestedWord(suggest2?.text.toString()) }
        suggest3?.setOnClickListener { insertSuggestedWord(suggest3?.text.toString()) }

        // Buka Panel Emoji Native
        view.findViewById<ImageView>(R.id.btn_emoji)?.setOnClickListener {
            mainKeyboardLayout?.visibility = View.GONE
            emojiPanelLayout?.visibility = View.VISIBLE
        }

        // Voice Input Action
        view.findViewById<ImageView>(R.id.btn_mic)?.setOnClickListener {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Layanan pengetikan suara tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        val deleteBtn = view.findViewById<Button>(R.id.btn_delete)
        deleteBtn?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDeleting = true
                    doDelete()
                    deleteHandler.postDelayed(deleteRunnable, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDeleting = false
                    deleteHandler.removeCallbacks(deleteRunnable)
                }
            }
            true
        }
    }

    private fun setupEmojiPanel(view: View) {
        val gridLayout = view.findViewById<GridLayout>(R.id.grid_emojis)
        view.findViewById<Button>(R.id.btn_close_emoji)?.setOnClickListener {
            emojiPanelLayout?.visibility = View.GONE
            mainKeyboardLayout?.visibility = View.VISIBLE
        }

        gridLayout?.removeAllViews()
        for (emoji in iosEmojis) {
            val tv = TextView(this).apply {
                text = emoji
                textSize = 24f // Menggunakan float 24f (bukan 24sp)
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                setOnClickListener {
                    currentInputConnection?.commitText(emoji, 1)
                }
            }
            gridLayout?.addView(tv)
        }
    }

    private fun getCurrentWord(): String {
        val inputConnection = currentInputConnection ?: return ""
        val textBeforeCursor = inputConnection.getTextBeforeCursor(30, 0) ?: return ""
        val words = textBeforeCursor.toString().split("\\s+".toRegex())
        return words.lastOrNull() ?: ""
    }

    private fun updateSuggestions(query: String) {
        val filtered = if (query.isEmpty()) {
            listOf("yang", "aku", "ini")
        } else {
            dictionary.filter { it.startsWith(query, ignoreCase = true) }
        }

        suggest1?.text = filtered.getOrNull(0) ?: ""
        suggest2?.text = filtered.getOrNull(1) ?: ""
        suggest3?.text = filtered.getOrNull(2) ?: ""
    }

    private fun doDelete() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            inputConnection.commitText("", 1)
        }
        updateSuggestions(getCurrentWord())
    }

    private fun insertSuggestedWord(word: String) {
        if (word.isEmpty()) return
        val inputConnection = currentInputConnection ?: return
        val current = getCurrentWord()
        if (current.isNotEmpty()) {
            inputConnection.deleteSurroundingText(current.length, 0)
        }
        inputConnection.commitText("$word ", 1)
        updateSuggestions("")
    }

    override fun onClick(v: View?) {
        val inputConnection = currentInputConnection ?: return

        when (v?.id) {
            R.id.btn_shift -> {
                if (isSymbolMode) {
                    isSecondarySymbolMode = !isSecondarySymbolMode
                    updateKeyLabels()
                } else {
                    isCaps = !isCaps
                    updateKeyLabels()
                }
            }
            R.id.btn_mode -> {
                isSymbolMode = !isSymbolMode
                isSecondarySymbolMode = false
                if (isSymbolMode) {
                    isCaps = false
                }
                val modeBtn = v as? Button
                modeBtn?.text = if (isSymbolMode) "ABC" else "123"
                updateKeyLabels()
            }
            R.id.btn_space -> {
                inputConnection.commitText(" ", 1)
                updateSuggestions("")
            }
            R.id.btn_enter -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                updateSuggestions("")
            }
            else -> {
                val button = v as? Button
                val text = button?.text?.toString()
                if (!text.isNullOrEmpty()) {
                    inputConnection.commitText(text, 1)
                    
                    if (isCaps && !isSymbolMode) {
                        isCaps = false
                        updateKeyLabels()
                    }
                    updateSuggestions(getCurrentWord())
                }
            }
        }
    }

    private fun updateKeyLabels() {
        if (isSymbolMode) {
            shiftBtn?.text = if (isSecondarySymbolMode) "123" else "#+="
        } else {
            shiftBtn?.text = "⇧"
        }

        for (i in letterButtons.indices) {
            val btn = letterButtons[i]
            if (isSymbolMode) {
                btn.text = if (isSecondarySymbolMode) symbolsPage2[i] else symbolsPage1[i]
            } else {
                btn.text = if (isCaps) letters[i].uppercase() else letters[i]
            }
        }
    }
}