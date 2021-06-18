package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.transformCmp
import kotlinx.coroutines.launch
import ktx.ashley.allOf
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.graphics.use

/**
 * System for debugging bounding rectangles of entities. It creates a [ShapeRenderer] and adds it to the
 * [AssetStorage]. Uses the [TransformComponent] information to draw a rect using the created [ShapeRenderer].
 */
class DebugBoundingAreaSystem(
  assetStorage: AssetStorage,
  private val viewport: Viewport
) : IteratingSystem(allOf(TransformComponent::class).get()) {
  private val shapeRenderer: ShapeRenderer = ShapeRenderer()

  init {
    if (!assetStorage.isLoaded<ShapeRenderer>("debugShapeRenderer")) {
      KtxAsync.launch {
        // add ShapeRenderer to dispose it at the end of the game
        assetStorage.add("debugShapeRenderer", shapeRenderer)
      }
    }
  }

  /**
   * Draws a bounding rectangle of the [entity] using its [TransformComponent]
   */
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val transformCmp = entity.transformCmp
    val pos = transformCmp.position
    val size = transformCmp.size

    viewport.apply()
    shapeRenderer.use(ShapeRenderer.ShapeType.Line, viewport.camera) {
      it.color = Color.CYAN
      it.projectionMatrix = viewport.camera.combined
      it.rect(pos.x, pos.y, size.x, size.y)
    }
  }
}
