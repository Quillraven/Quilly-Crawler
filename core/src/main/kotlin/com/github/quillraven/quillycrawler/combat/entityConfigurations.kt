package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.AnimationComponent
import com.github.quillraven.commons.ashley.component.Box2DComponent.Companion.TMP_VECTOR2
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.TransformComponent
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.quillycrawler.QuillyCrawler.Companion.UNIT_SCALE
import com.github.quillraven.quillycrawler.ashley.component.*
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.combat.command.*
import ktx.ashley.EngineEntity
import ktx.ashley.with
import ktx.collections.set
import kotlin.math.floor

private fun EngineEntity.withTransformAndAnimation(
  regionKey: String,
  w: Float,
  h: Float,
  setPos: TransformComponent.() -> Unit
) {
  with<TransformComponent> {
    this.size.set(w, h)
    this.apply(setPos)
  }
  with<AnimationComponent> {
    this.atlasFilePath = TextureAtlasAssets.ENTITIES.descriptor.fileName
    this.regionKey = regionKey
    this.stateKey = "idle"
    this.animationSpeed = 0f
  }
  with<RenderComponent>()
}

fun EngineEntity.configurePlayerCombatEntity(playerEntity: Entity, viewport: Viewport) {
  withTransformAndAnimation(playerEntity.animationCmp.regionKey, 1f * 1.5f, 1.75f * 1.5f) {
    position.set(
      viewport.camera.position.x - size.x * 0.5f + 1f,
      viewport.camera.position.y - viewport.worldHeight * 0.5f + 1f,
      position.z
    )
    // wizard graphic has 9 pixels of empty space at the top and therefore some effects which are aligned top
    // go into the "void".
    // To fix that, we adjust the bounding box by those 9 pixels
    boundingBoxOffset.y = -(9f * UNIT_SCALE * 1.5f)
  }
  with<PlayerComponent>()
  with<BagComponent> { playerEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value } }
  with<GearComponent> { playerEntity.gearCmp.gear.forEach { entry -> gear[entry.key] = entry.value } }
  with<StatsComponent> { playerEntity.statsCmp.stats.forEach { entry -> stats[entry.key] = entry.value } }
  with<CombatComponent> { commandsToLearn.addAll(playerEntity.combatCmp.commandsToLearn) }
  with<BuffComponent>()
}

fun EngineEntity.configureEnemyCombatEntity(
  name: String,
  dungeonLevel: Int,
  viewport: Viewport,
  enemyIndex: Int,
  numEntities: Int
) {
  when (name) {
    "BIG_DEMON" -> TMP_VECTOR2.set(2f, 2.25f)
    "CHORT" -> TMP_VECTOR2.set(1f, 1.5f)
    else -> TMP_VECTOR2.set(1f, 1f)
  }
  withTransformAndAnimation(name, TMP_VECTOR2.x * 1.25f, TMP_VECTOR2.y * 1.25f) {
    val leftX = viewport.camera.position.x - viewport.worldWidth * 0.5f
    position.set(
      // -1f at the end is used to move the entities to the left because there is an UI element that is on the right edge
      leftX + (viewport.worldWidth / (numEntities + 1)) * (enemyIndex + 1) - size.x * 0.5f - 1f,
      // -3.5f is used to move the entities away from the top edge because of UI elements and some scaling combat orders
      viewport.camera.position.y + viewport.worldHeight * 0.5f - 3.5f - MathUtils.random(0f, 1.1f),
      position.z
    )
  }
  val combatCmp = with<CombatComponent> {
    learn<CommandAttack>()
    learn<CommandDefend>()
    learn<CommandDeath>()
  }
  with<BuffComponent>()

  val statsCmp: StatsComponent = when (name) {
    "GOBLIN" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 7f
        stats[StatsType.AGILITY] = 7f
        stats[StatsType.PHYSICAL_DAMAGE] = 3f
        stats[StatsType.PHYSICAL_ARMOR] = 1f
        stats[StatsType.MAGIC_ARMOR] = 1f
      }
    }
    "CHORT" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 11f
        stats[StatsType.AGILITY] = 8f
        stats[StatsType.PHYSICAL_DAMAGE] = 5f
        stats[StatsType.PHYSICAL_ARMOR] = 4f
      }
    }
    "IMP" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 9f
        stats[StatsType.AGILITY] = 10f
        stats[StatsType.PHYSICAL_DAMAGE] = 4f
        stats[StatsType.PHYSICAL_ARMOR] = 3f
        stats[StatsType.MAGIC_ARMOR] = 5f
      }
    }
    "SKELET" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 15f
        stats[StatsType.AGILITY] = 3f
        stats[StatsType.PHYSICAL_DAMAGE] = 6f
      }
    }
    "BIG_DEMON" -> {
      combatCmp.learn<CommandTransform>()
      combatCmp.learn<CommandHeal>()
      combatCmp.learn<CommandFirebolt>()
      with<CombatAIComponent> { treeFilePath = "ai/big_demon.tree" }
      with {
        stats[StatsType.LIFE] = 32f
        stats[StatsType.MANA] = 15f
        stats[StatsType.AGILITY] = 3f
        stats[StatsType.PHYSICAL_DAMAGE] = 7f
        stats[StatsType.MAGIC_DAMAGE] = 7f
        stats[StatsType.PHYSICAL_ARMOR] = 6f
        stats[StatsType.MAGIC_ARMOR] = 6f
      }
    }
    "DUMMY" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with { stats[StatsType.LIFE] = 20000f }
    }
    else -> {
      throw GdxRuntimeException("Unsupported enemy combat name: $name")
    }
  }

  // adjusts stats according to dungeon level: each level increases stats by 5%
  statsCmp.stats.forEach { entry -> statsCmp[entry.key] = floor(entry.value * (1f + (dungeonLevel - 1) * 0.05f)) }
  statsCmp[StatsType.MAX_LIFE] = statsCmp[StatsType.LIFE]
  statsCmp[StatsType.MAX_MANA] = statsCmp[StatsType.MANA]
}
