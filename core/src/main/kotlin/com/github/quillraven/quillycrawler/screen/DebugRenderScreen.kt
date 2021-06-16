package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.ashley.system.*
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.ashley.entity
import ktx.ashley.with
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.earthGravity

class DebugRenderScreen(private val game: QuillyCrawler) : AbstractScreen(game) {
  private val viewport = FitViewport(16f, 9f)
  private val world = World(earthGravity, true)
  private val shapeRenderer = ShapeRenderer()
  private val b2dRenderer = Box2DDebugRenderer()
  private val engine = PooledEngine().apply {
    addSystem(Box2DSystem(world, 1 / 60f))
    addSystem(AnimationSystem(game.assetStorage, QuillyCrawler.UNIT_SCALE))
    addSystem(RenderSystem(game.batch, viewport))
    addSystem(Box2DDebugRenderSystem(world, viewport, b2dRenderer))
    addSystem(DebugBoundingAreaSystem(assetStorage, viewport))
  }
  private val baseTexture: Texture by lazy {
    val pixmap = Pixmap(
      (1 / QuillyCrawler.UNIT_SCALE).toInt(),
      (1 / QuillyCrawler.UNIT_SCALE).toInt(),
      Pixmap.Format.RGB888
    ).apply {
      setColor(1f, 0f, 0f, 1f)
      fill()
    }
    Texture(pixmap)
  }
  private val smallTexture: Texture by lazy {
    val pixmap = Pixmap(
      (1 / QuillyCrawler.UNIT_SCALE * 0.5f).toInt(),
      (1 / QuillyCrawler.UNIT_SCALE * 0.5f).toInt(),
      Pixmap.Format.RGB888
    ).apply {
      setColor(0f, 1f, 0f, 1f)
      fill()
    }
    Texture(pixmap)
  }
  private val bigTexture: Texture by lazy {
    val pixmap = Pixmap(
      (1 / QuillyCrawler.UNIT_SCALE * 2f).toInt(),
      (1 / QuillyCrawler.UNIT_SCALE * 2f).toInt(),
      Pixmap.Format.RGB888
    ).apply {
      setColor(0f, 0f, 1f, 1f)
      fill()
    }
    Texture(pixmap)
  }

  private fun Sprite.initializeWithTexture(texture: Texture) {
    setRegion(texture)
    setSize(texture.width * QuillyCrawler.UNIT_SCALE, texture.height * QuillyCrawler.UNIT_SCALE)
    setOrigin(width * 0.5f, height * 0.5f)
  }

  private fun Sprite.initializeWithRegion(region: TextureRegion) {
    setRegion(region)
    setSize(region.regionWidth * QuillyCrawler.UNIT_SCALE, region.regionHeight * QuillyCrawler.UNIT_SCALE)
    setOrigin(width * 0.5f, height * 0.5f)
  }

  private fun nonB2dEntity(x: Float, y: Float, w: Float, h: Float, texture: Texture) {
    engine.entity {
      with<TransformComponent> {
        position.set(x, y, Z_DEFAULT.toFloat())
        size.set(w, h)
      }
      with<RenderComponent> { sprite.initializeWithTexture(texture) }
    }
  }

  private fun b2dEntity(x: Float, y: Float, w: Float, h: Float, texture: Texture) {
    engine.entity {
      val transformCmp = with<TransformComponent> {
        position.set(x, y, Z_DEFAULT.toFloat())
        size.set(w, h)
      }
      with<Box2DComponent> {
        body = world.body(BodyDef.BodyType.StaticBody) {
          position.set(
            transformCmp.position.x + transformCmp.size.x * 0.5f,
            transformCmp.position.y + transformCmp.size.y * 0.5f
          )
          fixedRotation = true
          allowSleep = false
          box(
            transformCmp.size.x,
            transformCmp.size.y,
            Box2DComponent.TMP_VECTOR2.set(0f, -transformCmp.size.y * 0.5f + transformCmp.size.y * 0.5f)
          ) {
            friction = 0f
            isSensor = false
          }
        }
      }
      with<RenderComponent> { sprite.initializeWithTexture(texture) }
    }
  }

  override fun show() {
    super.show()

    // non-box2d entity without scaling = 1:1 with UNIT_SCALE
    nonB2dEntity(1f, 1f, 1f, 1f, baseTexture)
    // non-box2d entity with 2x scaling
    nonB2dEntity(2f, 1f, 2f, 2f, baseTexture)
    // non-box2d entity with 0.5x scaling
    nonB2dEntity(4f, 1f, 0.5f, 0.5f, baseTexture)
    // non-box2d entity with a texture two times the size of UNIT_SCALE
    nonB2dEntity(6f, 1f, 1f, 1f, bigTexture)
    // non-box2d entity with a texture two times the size of UNIT_SCALE with scaling
    nonB2dEntity(8f, 1f, 0.5f, 0.5f, bigTexture)
    // non-box2d entity with a texture half the size of UNIT_SCALE
    nonB2dEntity(9f, 1f, 1f, 1f, smallTexture)
    // non-box2d entity with a texture half the size of UNIT_SCALE with scaling
    nonB2dEntity(10f, 1f, 2f, 2f, smallTexture)

    // entity with a non-squared texture
    engine.entity {
      with<TransformComponent> {
        position.set(13f, 1f, Z_DEFAULT.toFloat())
        size.set(2f, 2.25f)
      }
      with<RenderComponent> {
        sprite.initializeWithRegion(
          assetStorage[TextureAtlasAssets.ENTITIES.descriptor].findRegion(
            "BIG_DEMON/idle", 0
          )
        )
      }
    }

    // animation that should be centered inside the entity from above
    engine.entity {
      with<TransformComponent> {
        position.set(13f, 1f, Z_DEFAULT.toFloat())
        size.set(2f, 2.25f)
      }
      with<AnimationComponent> {
        atlasFilePath = TextureAtlasAssets.EFFECTS.descriptor.fileName
        regionKey = "HEAL"
        stateKey = "frame"
      }
      with<RenderComponent>()
    }

    // box2d entity without scaling = 1:1 with UNIT_SCALE
    b2dEntity(1f, 4f, 1f, 1f, baseTexture)
    // box2d entity with 2x scaling
    b2dEntity(2f, 4f, 2f, 2f, baseTexture)
    // box2d entity with 0.5x scaling
    b2dEntity(4f, 4f, 0.5f, 0.5f, baseTexture)
    // box2d entity with a texture two times the size of UNIT_SCALE
    b2dEntity(6f, 4f, 1f, 1f, bigTexture)
    // box2d entity with a texture two times the size of UNIT_SCALE with scaling
    b2dEntity(8f, 4f, 0.5f, 0.5f, bigTexture)
    // box2d entity with a texture half the size of UNIT_SCALE
    b2dEntity(9f, 4f, 1f, 1f, smallTexture)
    // box2d entity with a texture half the size of UNIT_SCALE with scaling
    b2dEntity(10f, 4f, 2f, 2f, smallTexture)
  }

  override fun hide() {
    super.hide()
    engine.removeAllEntities()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    viewport.update(width, height, true)
  }

  override fun render(delta: Float) {
    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      hide()
      show()
    } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
      game.addScreen(GameScreen(game))
      game.setScreen<GameScreen>()
    }

    super.render(delta)
    engine.update(delta)
  }

  override fun dispose() {
    super.dispose()
    baseTexture.dispose()
    smallTexture.dispose()
    bigTexture.dispose()
    world.dispose()
    shapeRenderer.dispose()
    b2dRenderer.dispose()
  }
}
