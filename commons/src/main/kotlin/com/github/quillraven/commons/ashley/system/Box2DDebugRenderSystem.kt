package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.Viewport

class Box2DDebugRenderSystem(
    private val world: World,
    private val viewport: Viewport,
    private val renderer: Box2DDebugRenderer
) : EntitySystem() {
    override fun update(deltaTime: Float) {
        viewport.apply()
        renderer.render(world, viewport.camera.combined)
    }
}