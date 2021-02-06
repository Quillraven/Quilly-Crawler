package com.github.quillraven.quillycrawler.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener

class ControllerInputService(
    controller: Controller
) : InputService, ControllerListener {
    init {
        controller.addListener(this)
    }

    override fun connected(controller: Controller?) {

    }

    override fun disconnected(controller: Controller?) {

    }

    override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
        println("controller")
        return false
    }

    override fun buttonUp(controller: Controller?, buttonCode: Int) = false

    override fun axisMoved(controller: Controller?, axisCode: Int, value: Float) = false
}