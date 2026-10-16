// NOTE: written without being able to run Gradle (no network where it was authored). The verified build is
// scripts/build-local.sh. Check the plugin version below against https://plugins.jetbrains.com/docs/intellij/
plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "dev.makslevental"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

// An IDE you already have (property `ideHome`, else the IDE_HOME environment variable), or a download.
val ideHome = providers.gradleProperty("ideHome").orElse(providers.environmentVariable("IDE_HOME"))

dependencies {
    intellijPlatform {
        if (ideHome.isPresent) {
            local(file(ideHome.get()))
        } else {
            create(providers.gradleProperty("platformType").get(), providers.gradleProperty("platformVersion").get())
        }
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "262" }
    }
}
