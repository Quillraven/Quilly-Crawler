package com.github.quillraven.quillycrawler.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.ashley.component.Box2DComponent.Companion.TMP_VECTOR2
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ai.EnemyState
import com.github.quillraven.quillycrawler.ai.PlayerState
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.combat.command.CommandAttack
import com.github.quillraven.quillycrawler.combat.command.CommandDeath
import com.github.quillraven.quillycrawler.combat.command.CommandUseItem
import ktx.ashley.*
import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.circle
import ktx.collections.set
import ktx.log.error
import ktx.tiled.x
import ktx.tiled.y
import kotlin.math.min

private val playerFamily = allOf(PlayerComponent::class).exclude(RemoveComponent::class).get()

private fun EngineEntity.withBox2DComponents(
  world: World,
  bodyType: BodyType,
  x: Float,
  y: Float,
  z: Int = Z_DEFAULT,
  width: Float = 1f,
  height: Float = 1f,
  boundingBoxWidthPercentage: Float = 1f,
  boundingBoxHeightPercentage: Float = 1f,
  onlySensor: Boolean = false,
  optionalInit: BodyDefinition.() -> Unit = {}
) {
  val transformCmp = with<TransformComponent> {
    position.set(x, y, z.toFloat())
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
      val boundingBoxWidth = transformCmp.size.x * boundingBoxWidthPercentage
      val boundingBoxHeight = transformCmp.size.y * boundingBoxHeightPercentage
      box(
        boundingBoxWidth,
        boundingBoxHeight,
        TMP_VECTOR2.set(0f, -transformCmp.size.y * 0.5f + boundingBoxHeight * 0.5f)
      ) {
        friction = 0f
        isSensor = onlySensor
      }

      this.apply(optionalInit)

      userData = this@withBox2DComponents.entity
    }
  }
}

fun EngineEntity.withAnimationComponents(
  atlas: TextureAtlasAssets,
  regionKey: String,
  stateKey: String = "",
  animationSpeed: Float = 1f
) {
  with<AnimationComponent> {
    this.atlasFilePath = atlas.descriptor.fileName
    this.regionKey = regionKey
    this.stateKey = stateKey
    this.animationSpeed = animationSpeed
  }
  with<RenderComponent>()
}

fun EngineEntity.configureTiledMapEntity(layer: MapLayer, mapObject: MapObject, world: World?): Boolean {
  if (world == null) {
    throw GdxRuntimeException("Box2D world must not be null")
  }

  val x = mapObject.x * QuillyCrawler.UNIT_SCALE
  val y = mapObject.y * QuillyCrawler.UNIT_SCALE
  val name = mapObject.name

  when {
    name == "PLAYER" -> {
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
    name.startsWith("CHEST_") -> {
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "chest")
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
    name == "SHOP" -> {
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "WITCH", "idle", 0.35f)
      withBox2DComponents(
        world,
        BodyType.StaticBody,
        x,
        y,
        Z_DEFAULT,
        1.25f,
        1.875f,
        boundingBoxWidthPercentage = 0.75f,
        boundingBoxHeightPercentage = 0.35f
      )
      with<ActionableComponent> { type = ActionType.SHOP }
      with<BagComponent>()
    }
    name == "REAPER" -> {
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "REAPER", "idle", 0.4f)
      withBox2DComponents(
        world,
        BodyType.StaticBody,
        x,
        y,
        Z_DEFAULT,
        1.25f,
        1.40625f,
        boundingBoxWidthPercentage = 0.75f,
        boundingBoxHeightPercentage = 0.35f
      )
      with<ActionableComponent> { type = ActionType.REAPER }
    }
    name == "EXIT" -> {
      withAnimationComponents(TextureAtlasAssets.ENTITIES, "exit", "idle")
      withBox2DComponents(world, BodyType.StaticBody, x, y, Z_DEFAULT - 1, onlySensor = true)
      with<ActionableComponent> { type = ActionType.EXIT }
    }
    layer.name == "enemies" -> {
      withAnimationComponents(TextureAtlasAssets.ENTITIES, name)
      when (name) {
        "BIG_DEMON" -> TMP_VECTOR2.set(2f, 2.25f)
        "CHORT" -> TMP_VECTOR2.set(1f, 1.5f)
        else -> TMP_VECTOR2.set(1f, 1f)
      }
      withBox2DComponents(
        world,
        BodyType.StaticBody,
        x - 0.5f,
        y,
        Z_DEFAULT,
        TMP_VECTOR2.x,
        TMP_VECTOR2.y
      )
      with<StateComponent> {
        state = when (name) {
          "DUMMY" -> EnemyState.IDLE
          else -> EnemyState.RUN
        }
      }
      with<ActionableComponent> { type = ActionType.ENEMY }
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
    withAnimationComponents(TextureAtlasAssets.ENTITIES, "wizard-m")
    withBox2DComponents(world, BodyType.DynamicBody, x, y, Z_DEFAULT, 1f, 1.75f, 0.75f, 0.2f) {
      circle(1f) {
        isSensor = true
      }
    }
    with<StateComponent> { state = PlayerState.IDLE }
    with<PlayerComponent>()
    with<PlayerControlComponent>()
    with<BagComponent> {
      items[ItemType.HEALTH_POTION] = createItemEntity(ItemType.HEALTH_POTION, 5)
      items[ItemType.MANA_POTION] = createItemEntity(ItemType.MANA_POTION, 1)
    }
    with<InteractComponent>()
    with<MoveComponent> { maxSpeed = 5f }
    with<CameraLockComponent>()
    with<GearComponent>()
    with<StatsComponent> {
      stats[StatsType.LIFE] = 30f
      stats[StatsType.MAX_LIFE] = 30f
      stats[StatsType.MANA] = 10f
      stats[StatsType.MAX_MANA] = 10f
      stats[StatsType.STRENGTH] = StatsComponent.BASE_STRENGTH
      stats[StatsType.AGILITY] = StatsComponent.BASE_AGILITY
      stats[StatsType.INTELLIGENCE] = StatsComponent.BASE_INTELLIGENCE
      stats[StatsType.PHYSICAL_DAMAGE] = 7f
      stats[StatsType.MAGIC_DAMAGE] = 4f
      stats[StatsType.PHYSICAL_ARMOR] = 3f
      stats[StatsType.MAGIC_ARMOR] = 1f
    }
    with<CombatComponent> {
      learn<CommandAttack>()
      learn<CommandDeath>()
      learn<CommandUseItem>()
    }
  }
}

fun Engine.createEffectEntity(
  targetEntity: Entity,
  region: String,
  align: Int,
  removeDelay: Float,
  speed: Float = 1f,
  scaling: Float = 1f,
  offsetX: Float = 0f,
  offsetY: Float = 0f
): Entity {
  return this.entity {
    with<TransformComponent> {
      val targetTransform = targetEntity.transformCmp
      val effectSize = min(targetTransform.width, targetTransform.height) * scaling
      val wDiff = targetTransform.width - effectSize
      val hDiff = targetTransform.height - effectSize
      position.x = targetTransform.position.x + wDiff * 0.5f + offsetX
      position.y = targetTransform.position.y + offsetY
      position.z = 1f
      position.y += when (align) {
        Align.top -> targetTransform.height - effectSize
        Align.center -> hDiff * 0.5f
        else -> 0f
      }
      size.set(effectSize, effectSize)
    }
    with<RenderComponent>()
    with<AnimationComponent> {
      atlasFilePath = TextureAtlasAssets.EFFECTS.descriptor.fileName
      regionKey = region
      stateKey = "frame"
      animationSpeed = speed
    }
    if (removeDelay > 0f) {
      with<RemoveComponent> {
        delay = removeDelay
      }
    }
  }
}
