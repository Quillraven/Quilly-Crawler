package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.map.TiledMapService
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to mark an [Entity] that is created by the [TiledMapService]. It is used
 * to remove such entities when the map is changing.
 *
 * The [id] represents the id of the object in the Tiled editor.
 *
 * [name] and [type] represent the name and type of the object in Tiled. The default value is an empty string.
 */
class TiledComponent : Component, Pool.Poolable {
  var id: Int = -1
  var type: String = ""
  var name: String = ""

  override fun reset() {
    id = -1
    type = ""
    name = ""
  }

  companion object {
    val MAPPER = mapperFor<TiledComponent>()
  }
}

/**
 * Returns a [TiledComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.tiledCmp: TiledComponent
  get() = this[TiledComponent.MAPPER]
    ?: throw GdxRuntimeException("TiledComponent for entity '$this' is null")
