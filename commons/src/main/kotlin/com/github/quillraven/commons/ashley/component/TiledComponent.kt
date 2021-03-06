package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.map.TiledMapService

/**
 * Component to mark an [Entity] that is created by the [TiledMapService]. It is used
 * to remove such entities when the map is changing.
 *
 * The [id] represents the id of the object in the Tiled editor.
 */
class TiledComponent : Component, Pool.Poolable {
  var id: Int = -1

  override fun reset() {
    id = -1
  }
}
