package com.cutzhraa.nauboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class NauKeyboardService : InputMethodService(), View.OnClickListener {

    private var isCaps = false
    private var isSymbolMode = false

    private var rootView: View? = null
    private var shiftBtn: Button? = null
    private val letterButtons = mutableListOf<Button>()
    
    private val letters: Array<String> = arrayOf(
        "q","w","e","r","t","y","u","i","o","p",
        "a","s","d","f","g","h","j","k","l",
        "z","x","c","v","b","n","m"
    )
    
    private val symbols: Array<String> = arrayOf(
        "1","2","3","4","5","6","7","8","9","0",
        "@","#","$","%","&","-","+","(",")","*",
        "\"", "'", ":", ";", "!", "?"
    )

    // Handler untuk Hold Backspace (Delete Beruntun)
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
        rootView?.let { setupKeyListeners(it) }
        updateKeyLabels()
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

        // Suggestion Click Listeners
        view.findViewById<TextView>(R.id.suggest_1)?.setOnClickListener { insertSuggestedWord("aku ") }
        view.findViewById<TextView>(R.id.suggest_2)?.setOnClickListener { insertSuggestedWord("saya ") }
        view.findViewById<TextView>(R.id.suggest_3)?.setOnClickListener { insertSuggestedWord("itu ") }

        // Setup Touch Listener untuk Hold Backspace
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

    private fun doDelete() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            inputConnection.commitText("", 1)
        }
    }

    private fun insertSuggestedWord(word: String) {
        currentInputConnection?.commitText(word, 1)
    }

    override fun onClick(v: View?) {
        val inputConnection = currentInputConnection ?: return

        when (v?.id) {
            R.id.btn_shift -> {
                if (!isSymbolMode) {
                    isCaps = !isCaps
                    updateKeyLabels()
                }
            }
            R.id.btn_mode -> {
                isSymbolMode = !isSymbolMode
                if (isSymbolMode) {
                    isCaps = false
                }
                val modeBtn = v as? Button
                modeBtn?.text = if (isSymbolMode) "ABC" else "123"
                updateKeyLabels()
            }
            R.id.btn_space -> {
                inputConnection.commitText(" ", 1)
            }
            R.id.btn_enter -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
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
                }
            }
        }
    }

    private fun updateKeyLabels() {
        if (isSymbolMode) {
            shiftBtn?.text = "#+="
        } else {
            shiftBtn?.text = "⇧"
        }

        for (i in letterButtons.indices) {
            val btn = letterButtons[i]
            if (isSymbolMode) {
                btn.text = symbols[i]
            } else {
                btn.text = if (isCaps) letters[i].uppercase() else letters[i]
            }
        }
    }
}