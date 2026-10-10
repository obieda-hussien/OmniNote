package com.omninote.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Direction changes reset accumulated travel; tiny deltas do not flicker the chrome. */
class LibraryChromeState {
    var expanded by mutableStateOf(true)
        private set
    private var travel = 0f
    fun show() { expanded = true; travel = 0f }
    fun scroll(deltaY: Float, atTop: Boolean, threshold: Float) {
        if (atTop && deltaY >= 0f) { show(); return }
        if (deltaY == 0f) return
        if (travel * deltaY < 0f) travel = 0f
        travel += deltaY
        if (travel <= -threshold) { expanded = false; travel = 0f }
        else if (travel >= threshold) show()
    }
}
