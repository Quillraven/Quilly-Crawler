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
  implementation(projects.core)
  implementation(libs.gdx.backend)
  implementation(libs.gdx.desktop.platform) {
    artifact {
      name = libs.gdx.desktop.platform.get().module.name
      classifier = "natives-desktop"
      type = "jar"
    }
  }
  implementation(libs.gdx.desktop.box2d) {
    artifact {
      name = libs.gdx.desktop.box2d.get().module.name
      classifier = "natives-desktop"
      type = "jar"
    }
  }
  implementation(libs.gdx.desktop.controllers)
}

tasks {
  jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveFileName.set("${application.applicationName}-${project.version}.jar")

    manifest {
      attributes["Main-Class"] = application.mainClass.get()
    }
  }
}

