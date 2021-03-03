package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.CameraLockSystem

/**
 * Component to mark an [Entity] so that the [Camera] is centered on its position every frame.
 * It is used for the [CameraLockSystem].
 */
class CameraLockComponent : Component, Pool.Poolable {
  override fun reset() = Unit
}
