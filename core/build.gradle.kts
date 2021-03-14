dependencies {
  implementation("com.badlogicgames.box2dlights:box2dlights:${project.property("box2dlightsVersion")}")
  implementation("io.github.libktx:ktx-scene2d:${project.property("ktxVersion")}")
  implementation("io.github.libktx:ktx-style:${project.property("ktxVersion")}")
  api(project(":commons"))
}
