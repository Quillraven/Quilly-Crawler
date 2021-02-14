package com.github.quillraven.commons.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controllers

interface XboxInputProcessor : ControllerListener {
    override fun connected(controller: Controller?) = Unit

    override fun disconnected(controller: Controller?) = Unit

    fun addXboxControllerListener() {
        Controllers.getControllers()
            .firstOrNull { CONTROLLER_NAME == it.name }
            ?.run {
                // to avoid adding the same listener twice, we will first remove it.
                // unfortunately, there is no "contains" method that is why we do it like this.
                removeListener(this@XboxInputProcessor)
                addListener(this@XboxInputProcessor)
            }
    }

    fun removeXboxControllerListener() {
        Controllers.getControllers()
            .firstOrNull { CONTROLLER_NAME == it.name }
            ?.removeListener(this)
    }

    companion object {
        private const val CONTROLLER_NAME = "X360 Controller"

        const val BUTTON_A = 0
        const val BUTTON_B = 1
        const val BUTTON_X = 2
        const val BUTTON_Y = 3
        const val BUTTON_SELECT = 4
        const val BUTTON_START = 6
        const val BUTTON_L = 9
        const val BUTTON_R = 10
        const val BUTTON_UP = 11
        const val BUTTON_RIGHT = 14
        const val BUTTON_DOWN = 12
        const val BUTTON_LEFT = 13
        const val BUTTON_TRIGGER_L = 7
        const val BUTTON_TRIGGER_R = 8

        const val AXIS_X_LEFT = 0
        const val AXIS_Y_LEFT = 1
        const val AXIS_X_RIGHT = 2
        const val AXIS_Y_RIGHT = 3
        const val AXIS_L2 = 4
        const val AXIS_R2 = 5
    }
}