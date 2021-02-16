package com.github.quillraven.commons.game

import com.badlogic.gdx.graphics.g2d.Batch
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage

abstract class AbstractScreen(
    game: AbstractGame,
    val batch: Batch = game.batch,
    val assetStorage: AssetStorage = game.assetStorage
) : KtxScreen