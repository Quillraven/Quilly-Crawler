package com.github.quillraven.commons.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage

abstract class AbstractScreen(
    game: AbstractGame,
    val batch: Batch = game.batch,
    val assetStorage: AssetStorage = game.assetStorage
) : KtxScreen {
    abstract fun inputProcessor(): InputProcessor

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = inputProcessor()
    }

    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null
    }
}