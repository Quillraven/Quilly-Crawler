package com.github.quillraven.quillycrawler.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.ashley.*
import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.circle
import ktx.log.error
import ktx.tiled.x
import ktx.tiled.y

private val playerFamily = allOf(PlayerComponent::class).exclude(RemoveComponent::class).get()

private fun EngineEntity.withBox2DComponents(
  world: World,
  bodyType: BodyType,
  x: Float,
  y: Float,
  width: Float = 1f,
  height: Float = 1f,
  boundingBoxHeightPercentage: Float = 1f,
  onlySensor: Boolean = false,
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
        isSensor = onlySensor
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

fun EngineEntity.configureTiledMapEntity(mapObject: MapObject, world: World?): Boolean {
  if (world == null) {
    throw GdxRuntimeException("Box2D world must not be null")
  }

  val x = mapObject.x * QuillyCrawler.UNIT_SCALE
  val y = mapObject.y * QuillyCrawler.UNIT_SCALE

  when (mapObject.name) {
    "PLAYER" -> {
      val playerEntities = engine.getEntitiesFor(playerFamily)
      if (playerEntities.size() <= 0) {
        // player entity does not exist yet -> create it
        engine.createPlayerEntity(world, x, y)
      } else {
        // player already existing -> move it to new position
        playerEntities.forEach { player ->
          with(player.box2dCmp) {
            body.setTransform(x, y, body.angle)
            body.isAwake = true
            renderPosition.set(x, y)
          }
          with(player.transformCmp) {
            position.set(x, y, position.z)
          }
        }
      }
      return false
    }
    "CHEST_COMMON", "CHEST_RARE", "CHEST_EPIC" -> {
      withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "chest")
      withBox2DComponents(world, BodyType.StaticBody, x, y)
      with<StateComponent> { state = ChestState.IDLE }
      with<ActionableComponent> { type = ActionType.CHEST }
      when (mapObject.name) {
        "CHEST_COMMON" -> {
          with<LootComponent> { lootType = LootType.COMMON }
        }
        "CHEST_RARE" -> {
          this.entity.renderCmp.sprite.setColor(0.75f, 0.7f, 1f, 1f)
          with<LootComponent> { lootType = LootType.RARE }
        }
        else -> {
          this.entity.renderCmp.sprite.setColor(0.5f, 0.3f, 1f, 1f)
          with<LootComponent> { lootType = LootType.EPIC }
        }
      }
    }
    "BIG_DEMON" -> {
      withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "big-demon")
      with<TransformComponent> { position.set(x, y, position.z) }
      with<StateComponent> { state = BigDemonState.RUN }
    }
    "EXIT" -> {
      withBox2DComponents(world, BodyType.StaticBody, x, y, onlySensor = true)
      with<ActionableComponent> { type = ActionType.EXIT }
    }
    else -> {
      MapService.LOG.error { "Unsupported MapObject name '${mapObject.name}'" }
      return false
    }
  }

  return true
}

fun Engine.createPlayerEntity(world: World, x: Float, y: Float): Entity {
  return this.entity {
    withAnimationComponents(TextureAtlasAssets.CHARACTERS_AND_PROPS, "wizard-m")
    withBox2DComponents(world, BodyType.DynamicBody, x, y, boundingBoxHeightPercentage = 0.2f) {
      circle(1f) {
        isSensor = true
      }
    }
    with<StateComponent> { state = com.github.quillraven.quillycrawler.ai.PlayerState.IDLE }
    with<PlayerComponent>()
    with<PlayerControlComponent>()
    with<BagComponent>()
    with<InteractComponent>()
    with<MoveComponent> { maxSpeed = 5f }
    with<CameraLockComponent>()
    with<GearComponent>()
    with<StatsComponent> {
      stats[StatsType.LIFE] = 30f
      stats[StatsType.MAX_LIFE] = 30f
      stats[StatsType.MANA] = 10f
      stats[StatsType.MAX_MANA] = 10f
      stats[StatsType.STRENGTH] = 5f
      stats[StatsType.AGILITY] = 5f
      stats[StatsType.INTELLIGENCE] = 5f
      stats[StatsType.PHYSICAL_DAMAGE] = 7f
      stats[StatsType.MAGIC_DAMAGE] = 4f
      stats[StatsType.PHYSICAL_ARMOR] = 3f
      stats[StatsType.MAGIC_ARMOR] = 1f
    }
  }
}

fun Engine.createItemEntity(type: ItemType, numItems: Int = 1): Entity {
  return this.entity {
    with<ItemComponent> {
      itemType = type
      gearType = type.gearType
      amount = numItems
    }

    when (type) {
      ItemType.HAT -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 1f
          stats[StatsType.INTELLIGENCE] = 1f
        }
      }
      ItemType.ROBE -> {
        with<StatsComponent> {
          stats[StatsType.PHYSICAL_ARMOR] = 3f
          stats[StatsType.INTELLIGENCE] = 3f
        }
      }
      ItemType.UNDEFINED -> {
      }
    }
  }
}
