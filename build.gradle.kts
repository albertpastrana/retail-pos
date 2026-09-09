import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

version = "2.30.4"
description = "Openbravo POS"

repositories {
    mavenCentral()
}

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
    create("dataHelpers") {
        java.setSrcDirs(listOf("data"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    implementation(fileTree("lib") { include("*.jar") })
    listOf(
        "net.sourceforge.barbecue:barbecue:1.5-beta1",
        "org.beanshell:bsh-core:2.0b4",
        "commons-beanutils:commons-beanutils:1.7.0",
        "commons-codec:commons-codec:1.3",
        "commons-collections:commons-collections:3.1",
        "commons-digester:commons-digester:1.7",
        "commons-discovery:commons-discovery:0.2",
        "commons-lang:commons-lang:2.1",
        "commons-logging:commons-logging:1.0.4",
        "com.formdev:flatlaf:3.7.2",
        "jfree:jcommon:1.0.15",
        "jfree:jfreechart:1.0.12",
        "oro:oro:2.0.8",
        "org.swinglabs:swingx:0.9.5",
        "org.apache.velocity:velocity:1.5",
        "wsdl4j:wsdl4j:1.5.1",
        "axis:axis:1.4",
        "org.apache.axis:axis-jaxrpc:1.4",
        "org.apache.axis:axis-saaj:1.4",
        "net.sf.barcode4j:barcode4j-light:2.0",
    ).forEach { coord ->
        implementation(coord) {
            isTransitive = false
        }
    }
    "dataHelpersImplementation"(files("lib/derby.jar"))
}

val runtimeLibs by tasks.registering(Sync::class) {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("runtime-libs"))
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
    dependsOn(tasks.jar, localesJar, reportsJar, runtimeLibs)
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
    dependsOn(tasks.jar, localesJar, reportsJar, runtimeLibs, syncRunJars, "compileDataHelpersJava")
}

fun dataHelper(
    taskName: String,
    main: String,
    description: String,
    defaultArgs: List<String> = emptyList(),
    requireArgs: Boolean = false,
    checkLock: Boolean = true,
) {
    tasks.register<JavaExec>(taskName) {
        group = "pos"
        this.description = description
        classpath = sourceSets["dataHelpers"].runtimeClasspath
        mainClass.set(main)
        workingDir = layout.projectDirectory.asFile
        systemProperty("pos.home", layout.projectDirectory.asFile.absolutePath)
        if (defaultArgs.isNotEmpty()) {
            args(defaultArgs)
        }
        doFirst {
            val taskArgs = args.orEmpty()
            if (requireArgs && taskArgs.isEmpty()) {
                throw GradleException("Pass arguments with --args. See: ./gradlew help --task $taskName")
            }
            val db = taskArgs.getOrNull(0)
            if (checkLock && db != null) {
                val lockFile = workingDir.resolve(db).resolve("db.lck")
                if (lockFile.exists()) {
                    throw GradleException("Quit the POS first (Derby lock at $lockFile).")
                }
            }
        }
    }
}

dataHelper(
    "applyStoreResources",
    "ApplyStoreResources",
    "Upsert ticket/logo/button templates and add discount permissions to Administrator and Manager.",
    listOf("data/openbravopos-database"),
)
dataHelper(
    "importCatalog",
    "ImportCatalog",
    "Wipe products and stock, then load the TSV catalogue. Destructive. Quit the POS first.",
    listOf(
        "data/openbravopos-database",
        "data/import-categories.tsv",
        "data/import-products.tsv",
        "src-pos/com/openbravo/pos/templates/Ticket.Buttons.xml",
    ),
)
dataHelper(
    "keepCatalog",
    "KeepCatalog",
    "Drop products whose REFERENCE is not in a keep CSV. Pass: --args='data/openbravopos-database path/to/keep.csv'",
    requireArgs = true,
)
dataHelper(
    "updateResource",
    "UpdateResource",
    "Replace one RESOURCES row. Pass: --args='data/openbravopos-database NAME file'",
    requireArgs = true,
)
dataHelper(
    "dumpResource",
    "DumpResource",
    "Write named RESOURCES to a directory. Pass: --args='data/openbravopos-database outdir NAME...'",
    requireArgs = true,
)
dataHelper(
    "dumpAllResources",
    "DumpAllResources",
    "Dump all RESOURCES. Pass: --args='org.apache.derby.jdbc.EmbeddedDriver jdbc:derby:data/openbravopos-database outdir'",
    requireArgs = true,
    checkLock = false,
)
dataHelper(
    "showResource",
    "ShowResource",
    "Print named RESOURCES. Pass: --args='data/openbravopos-database NAME...'",
    requireArgs = true,
)
