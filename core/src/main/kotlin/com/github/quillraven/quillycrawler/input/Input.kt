package com.github.quillraven.quillycrawler.input

import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controllers

interface InputService : InputProcessor {
    override fun keyDown(keycode: Int) = false

    override fun keyUp(keycode: Int) = false

    override fun keyTyped(character: Char) = false

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false

    override fun mouseMoved(screenX: Int, screenY: Int) = false

    override fun scrolled(amountX: Float, amountY: Float) = false
}

object InputServiceProvider {
    val inputService: InputProcessor by lazy {
        if (Controllers.getControllers().isEmpty) {
            KeyboardInputService()
        } else {
            InputMultiplexer(KeyboardInputService(), ControllerInputService(Controllers.getControllers().first()))
        }
    }
}
