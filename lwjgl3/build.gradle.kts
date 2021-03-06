plugins {
  application
}

application {
  mainClass.set("com.github.quillraven.quillycrawler.lwjgl3.LauncherKt")

  version = "0.0.1-SNAPSHOT"
  applicationName = "Quilly Crawler"
}

sourceSets {
  main {
    resources.srcDir(rootProject.file("assets"))
  }
}

dependencies {
  implementation(project(":core"))
  implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${project.property("gdxVersion")}")
  implementation("com.badlogicgames.gdx:gdx-platform:${project.property("gdxVersion")}:natives-desktop")
  implementation("com.badlogicgames.gdx:gdx-box2d-platform:${project.property("gdxVersion")}:natives-desktop")
  implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:${project.property("gdxControllersVersion")}")
}

tasks {
  jar {
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveFileName.set("${application.applicationName}-${project.version}.jar")

    manifest {
      attributes["Main-Class"] = application.mainClass.get()
    }
  }
}

