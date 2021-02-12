package com.github.quillraven.commons.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage

abstract class AbstractScreen(
    game: AbstractGame,
    val batch: Batch = game.batch,
    val assetStorage: AssetStorage = game.assetStorage
) : KtxScreen {
    abstract val inputProcessor: InputProcessor

    override fun show() {
        super.show()
        if (inputProcessor is ControllerListener) {
            Controllers.addListener(inputProcessor as ControllerListener)
        }
        Gdx.input.inputProcessor = inputProcessor
    }

    override fun hide() {
        super.hide()
        if (inputProcessor is ControllerListener) {
            Controllers.removeListener(inputProcessor as ControllerListener)
        }
        Gdx.input.inputProcessor = null
    }
}