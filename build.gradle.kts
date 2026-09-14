plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.1"
}

group = "silenoid"
version = (findProperty("version") as String?)?.takeIf { it != "unspecified" } ?: "0.9.9"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass.set("com.silenoids.App")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.formdev:flatlaf:3.0")
    implementation("com.formdev:flatlaf-intellij-themes:3.0")
    implementation("com.intellij:forms_rt:7.0.3")
    implementation("com.github.goxr3plus:java-stream-player:10.0.1")
    implementation("com.j2html:j2html:1.6.0")
    implementation("com.squareup.okhttp:okhttp:2.7.5")
    implementation("org.json:json:20230227")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.silenoids.App"
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.silenoids.App"
    }
}

//tasks.named("build") {
//    dependsOn(tasks.named("shadowJar"))
//}
