package com.cutzhraa.nauboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button

class NauKeyboardService : InputMethodService(), View.OnClickListener {

    private var isCaps = true
    private var isSymbolMode = false

    private val letterButtons = mutableListOf<Button>()
    
    // Explicitly typed as Array<String>
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

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.main_keyboard, null)
        setupKeyListeners(view)
        updateKeyLabels()
        return view
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

        view.findViewById<Button>(R.id.btn_shift)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_mode)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_space)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_delete)?.setOnClickListener(this)
        view.findViewById<Button>(R.id.btn_enter)?.setOnClickListener(this)
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
                val modeBtn = v as? Button
                modeBtn?.text = if (isSymbolMode) "ABC" else "123"
                updateKeyLabels()
            }
            R.id.btn_delete -> {
                val selectedText = inputConnection.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    inputConnection.deleteSurroundingText(1, 0)
                } else {
                    inputConnection.commitText("", 1)
                }
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
                    
                    // Auto-reset Shift setelah mengetik 1 huruf kapital
                    if (isCaps && !isSymbolMode) {
                        isCaps = false
                        updateKeyLabels()
                    }
                }
            }
        }
    }

    private fun updateKeyLabels() {
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