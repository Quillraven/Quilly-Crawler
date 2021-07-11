package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.game.AbstractScreen
import ktx.ashley.get
import ktx.ashley.mapperFor
import kotlin.reflect.KClass

class SetScreenComponent : Component, Pool.Poolable {
  var screenType: KClass<out AbstractScreen> = AbstractScreen::class
  var screenData: Any? = null

  override fun reset() {
    screenType = AbstractScreen::class
    screenData = null
  }

  companion object {
    val MAPPER = mapperFor<SetScreenComponent>()
  }
}

val Entity.setScreenCmp: SetScreenComponent
  get() = this[SetScreenComponent.MAPPER]
    ?: throw GdxRuntimeException("SetScreenComponent for entity '$this' is null")
