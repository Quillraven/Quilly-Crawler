package com.github.quillraven.quillycrawler.ashley

import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ai.PlayerState
import com.github.quillraven.quillycrawler.ashley.component.CollectableComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.MoveComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.ashley.EngineEntity
import ktx.ashley.with
import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.circle
import ktx.log.error
import ktx.tiled.x
import ktx.tiled.y

private fun EngineEntity.withBox2DComponents(
  world: World,
  bodyType: BodyType,
  x: Float,
  y: Float,
  width: Float = 1f,
  height: Float = 1f,
  boundingBoxHeightPercentage: Float = 1f,
  optionalInit: BodyDefinition.() -> Unit = {}
) {
  val transformCmp = with<TransformComponent> {
    position.set(x, y, position.z)
    size.set(width, height)
  }
  with<Box2DComponent> {
    body = world.body(bodyType) {
      position.set(
        transformCmp.position.x + transformCmp.size.x * 0.5f,
        transformCmp.position.y + transformCmp.size.y * 0.5f
      )
      fixedRotation = true
      allowSleep = false
      val boundingBoxHeight = transformCmp.size.y * boundingBoxHeightPercentage
      box(
        transformCmp.size.x,
        boundingBoxHeight,
        Box2DComponent.TMP_VECTOR2.set(0f, -transformCmp.size.y * 0.5f + boundingBoxHeight * 0.5f)
      ) {
        friction = 0f
      }

      this.apply(optionalInit)

      userData = this@withBox2DComponents.entity
    }
  }
}

private fun EngineEntity.withAnimationComponents(atlas: TextureAtlasAssets, regionKey: String) {
  with<AnimationComponent> {
    this.atlasFilePath = atlas.descriptor.fileName
    this.regionKey = regionKey
  }
  with<RenderComponent>()
}

fun EngineEntity.configureEntity(mapObject: MapObject, world: World?): Boolean {
  if (world == null) {
    throw GdxRuntimeException("Box2D world must not be null")
  }

  val x = mapObject.x * QuillyCrawler.UNIT_SCALE
  val y = mapObject.y * QuillyCrawler.UNIT_SCALE

  when (mapObject.name) {
    "PLAYER" -> {
      withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "wizard-m")
      withBox2DComponents(world, BodyType.DynamicBody, x, y, boundingBoxHeightPercentage = 0.2f) {
        circle(1f) {
          isSensor = true
        }
      }
      with<StateComponent> { state = PlayerState.IDLE }
      with<PlayerComponent>()
      with<PlayerControlComponent>()
      with<CollectingComponent>()
      with<MoveComponent> { maxSpeed = 5f }
      with<CameraLockComponent>()
    }
    "CHEST" -> {
      withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "chest")
      withBox2DComponents(world, BodyType.StaticBody, x, y)
      with<StateComponent> { state = ChestState.IDLE }
      with<CollectableComponent>()
    }
    "BIG_DEMON" -> {
      withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "big-demon")
      with<TransformComponent> { position.set(x, y, position.z) }
      with<StateComponent> { state = BigDemonState.RUN }
    }
    else -> {
      MapService.LOG.error { "Unsupported MapObject name '${mapObject.name}'" }
      return false
    }
  }

  return true
}
