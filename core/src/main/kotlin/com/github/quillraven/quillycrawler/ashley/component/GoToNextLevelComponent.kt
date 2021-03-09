package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class GoToNextLevelComponent : Pool.Poolable, Component {
  override fun reset() = Unit

  companion object {
    val MAPPER = mapperFor<GoToNextLevelComponent>()
  }
}
