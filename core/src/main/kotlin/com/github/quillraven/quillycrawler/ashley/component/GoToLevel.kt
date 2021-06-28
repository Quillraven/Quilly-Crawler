package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class GoToLevel : Pool.Poolable, Component {
  var targetLevel: Int = 1

  override fun reset() {
    targetLevel = 1
  }

  companion object {
    val MAPPER = mapperFor<GoToLevel>()
  }
}

val Entity.goToLevelCmp: GoToLevel
  get() = this[GoToLevel.MAPPER]
    ?: throw GdxRuntimeException("GoToLevel for entity '$this' is null")
