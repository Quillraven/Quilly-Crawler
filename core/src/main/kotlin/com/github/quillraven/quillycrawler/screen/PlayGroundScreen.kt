package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.ashley.system.AnimationSystem
import com.github.quillraven.commons.ashley.system.EntityTypeStateAnimationSystem
import com.github.quillraven.commons.ashley.system.RenderSystem
import com.github.quillraven.commons.ashley.system.StateSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.EntityType
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.PlayerState
import ktx.ashley.entity
import ktx.ashley.with

class PlayGroundScreen(
    private val game: QuillyCrawler
) : AbstractScreen(game) {
    private val viewport = FitViewport(16f, 9f)
    private val engine = PooledEngine().apply {
        addSystem(EntityTypeStateAnimationSystem())
        addSystem(StateSystem())
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
        engine.run {
            entity {
                with<EntityTypeComponent> {
                    type = EntityType.WIZARD_MALE
                }
                with<TransformComponent>()
                with<AnimationComponent>()
                with<RenderComponent>()
                with<StateComponent> {
                    state = PlayerState.IDLE
                }
            }
            entity {
                with<EntityTypeComponent> {
                    type = EntityType.BIG_DEMON
                }
                with<TransformComponent> {
                    position.x = 1f
                    size.x = 0.75f
                    size.y = 0.75f
                }
                with<AnimationComponent>()
                with<RenderComponent>()
                with<StateComponent> {
                    state = BigDemonState.RUN
                }
            }
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