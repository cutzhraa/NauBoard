package com.cutzhraa.nauboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button

class NauKeyboardService : InputMethodService(), View.OnClickListener {

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.main_keyboard, null)
        setupKeyListeners(view)
        return view
    }

    private fun setupKeyListeners(view: View) {
        val keyIds = intArrayOf(
            R.id.btn_q, R.id.btn_w, R.id.btn_e, R.id.btn_r, R.id.btn_t,
            R.id.btn_y, R.id.btn_u, R.id.btn_i, R.id.btn_o, R.id.btn_p,
            R.id.btn_a, R.id.btn_s, R.id.btn_d, R.id.btn_f, R.id.btn_g,
            R.id.btn_h, R.id.btn_j, R.id.btn_k, R.id.btn_l, R.id.btn_z,
            R.id.btn_x, R.id.btn_c, R.id.btn_v, R.id.btn_b, R.id.btn_n,
            R.id.btn_m, R.id.btn_space, R.id.btn_delete, R.id.btn_enter
        )

        for (id in keyIds) {
            view.findViewById<Button>(id)?.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        val inputConnection = currentInputConnection ?: return

        when (v?.id) {
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
                }
            }
        }
    }
}