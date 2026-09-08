import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

version = "2.30.3"
description = "Openbravo POS"

val gitRevision: Provider<String> =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .map { it.trim().ifEmpty { "0" } }

sourceSets {
    main {
        java.setSrcDirs(listOf("src-beans", "src-data", "src-pos"))
        resources {
            setSrcDirs(listOf("src-beans", "src-data", "src-pos", "resources"))
            exclude("**/*.java")
            exclude("**/Thumbs.db")
            exclude("**/filesystem.attributes")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    implementation(fileTree("lib") { include("*.jar") })
}

tasks.processResources {
    val appVersion = version.toString()
    val revision = gitRevision
    inputs.property("appVersion", appVersion)
    inputs.property("gitRevision", revision)
    filesMatching("version.properties") {
        filter(ReplaceTokens::class, "tokens" to mapOf("APP_VERSION" to appVersion, "GIT_REVISION" to revision.get()))
    }
}

tasks.jar {
    archiveFileName.set("openbravopos.jar")
    destinationDirectory.set(layout.buildDirectory.dir("jar"))
    manifest {
        attributes(
            "Implementation-Vendor" to "Openbravo SL",
            "Implementation-Title" to "Openbravo Network POS",
            "Implementation-Version" to version,
            "Main-Class" to "com.openbravo.pos.forms.StartPOS",
        )
    }
}

val localesJar by tasks.registering(Jar::class) {
    archiveFileName.set("locales.jar")
    destinationDirectory.set(layout.buildDirectory.dir("jar"))
    from("locales")
}

val reportsJar by tasks.registering(Jar::class) {
    archiveFileName.set("reports.jar")
    destinationDirectory.set(layout.buildDirectory.dir("jar"))
    from("reports")
}

val syncRunJars by tasks.registering(Copy::class) {
    dependsOn(tasks.jar, localesJar, reportsJar)
    from(layout.buildDirectory.dir("jar")) {
        include("openbravopos.jar", "locales.jar", "reports.jar")
    }
    into(layout.projectDirectory)
}

tasks.jar {
    finalizedBy(syncRunJars)
}

tasks.assemble {
    dependsOn(localesJar, reportsJar, syncRunJars)
}

tasks.check {
    dependsOn(tasks.jar, localesJar, reportsJar, syncRunJars)
}
