package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.component.AnimationComponent
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.system.AnimationSystem
import com.github.quillraven.commons.ashley.system.RenderSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.AnimationType
import ktx.ashley.entity
import ktx.ashley.with

class PlayGroundScreen(
    private val game: QuillyCrawler
) : AbstractScreen(game) {
    private val viewport = FitViewport(16f, 9f)
    private val engine = PooledEngine().apply {
        addSystem(AnimationSystem(assetStorage, QuillyCrawler.UNIT_SCALE, 1 / 10f))
        addSystem(RenderSystem(batch, viewport))
    }

    override fun inputProcessor(): InputProcessor {
        return game.inputServiceProvider.inputService
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun show() {
        super.show()
        engine.entity {
            with<TransformComponent>()
            with<AnimationComponent> {
                type = AnimationType.BIG_DEMON_IDLE
            }
            with<RenderComponent>()
        }
    }

    override fun hide() {
        super.hide()
        engine.removeAllEntities()
    }

    override fun render(delta: Float) {
        engine.update(delta)
    }
}