package com.github.quillraven.quillycrawler.input

class KeyboardInputService : InputService {
    override fun keyDown(keycode: Int): Boolean {
        println("keyboard")
        return true
    }
}