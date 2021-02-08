dependencies {
    implementation("com.badlogicgames.box2dlights:box2dlights:${project.property("box2dlightsVersion")}")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:${project.property("gdxControllersVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.property("coroutines")}")
    api(project(":commons"))
}
