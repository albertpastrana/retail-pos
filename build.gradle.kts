import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("com.diffplug.spotless") version "8.10.1"
}

version = providers.gradleProperty("appVersion").orElse("3.0.0-RC1").get()
description = "Retail POS"

repositories {
    mavenCentral()
}

spotless {
    java {
        // Eclipse formats layout. It does not sort imports (no importOrder /
        // removeUnusedImports) and does not reflow string literals the way
        // google-java-format does.
        eclipse()
    }
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
    create("integrationTest") {
        java.setSrcDirs(listOf("src/integrationTest/java"))
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    implementation("org.apache.derby:derby:10.14.2.0")
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.flywaydb:flyway-mysql:9.22.3")
    listOf(
        "net.sourceforge.barbecue:barbecue:1.5-beta1",
        "org.beanshell:bsh-core:2.0b4",
        "commons-beanutils:commons-beanutils:1.9.4",
        "commons-codec:commons-codec:1.22.1",
        "commons-collections:commons-collections:3.2.2",
        "commons-digester:commons-digester:1.8.1",
        "commons-discovery:commons-discovery:0.5",
        "commons-lang:commons-lang:2.6",
        "commons-logging:commons-logging:1.2",
        "com.lowagie:itext:2.1.0",
        "com.formdev:flatlaf:3.7.2",
        "jfree:jcommon:1.0.16",
        "jfree:jfreechart:1.0.13",
        "org.apache.poi:poi:3.2-FINAL",
        "org.eclipse.jdt:core:3.1.1",
        "org.javapos:javapos:1.12.2",
        "org.rxtx:rxtx:2.1.7",
        "oro:oro:2.0.8",
        "org.swinglabs:swingx:0.9.5",
        "org.apache.velocity:velocity:1.7",
        "wsdl4j:wsdl4j:1.6.3",
        "axis:axis:1.4",
        "org.apache.axis:axis-jaxrpc:1.4",
        "org.apache.axis:axis-saaj:1.4",
        "net.sf.barcode4j:barcode4j-light:2.0",
    ).forEach { coord ->
        implementation(coord) {
            isTransitive = false
        }
    }
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    "dataHelpersImplementation"("org.apache.derby:derby:10.14.2.0")
    "dataHelpersImplementation"("org.postgresql:postgresql:42.7.13")
    "dataHelpersImplementation"("com.mysql:mysql-connector-j:8.4.0")
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter:5.12.2")
    "integrationTestRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.12.2")
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
    archiveFileName.set("retail-pos.jar")
    destinationDirectory.set(layout.buildDirectory.dir("jar"))
    manifest {
        attributes(
            "Implementation-Vendor" to "Retail POS",
            "Implementation-Title" to "Retail POS",
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
    doNotTrackState("Copies the runnable jars into the project directory for local launchers")
    dependsOn(tasks.jar, localesJar, reportsJar, runtimeLibs)
    from(layout.buildDirectory.dir("jar")) {
        include("retail-pos.jar", "locales.jar", "reports.jar")
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
    dependsOn("checkMigrationChecksums")
}

tasks.register<Exec>("checkMigrationChecksums") {
    group = "verification"
    description = "Ensure applied Flyway migrations are immutable."
    commandLine("bash", "scripts/check-migration-checksums.sh")
}

tasks.named("compileDataHelpersJava") {
    mustRunAfter(syncRunJars)
}

tasks.named("compileIntegrationTestJava") {
    mustRunAfter(syncRunJars)
}

tasks.named("spotlessJava") {
    mustRunAfter(syncRunJars)
}

val integrationTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Apply Flyway to Derby, MySQL, and PostgreSQL. Start Compose first: docker compose up -d --wait"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath + files("locales")
    useJUnitPlatform()
    systemProperty("pos.mysql.url", "jdbc:mysql://127.0.0.1:13306/pos")
    systemProperty("pos.mysql.user", "root")
    systemProperty("pos.mysql.password", "pos")
    systemProperty("pos.postgres.url", "jdbc:postgresql://127.0.0.1:15432/pos")
    systemProperty("pos.postgres.user", "pos")
    systemProperty("pos.postgres.password", "pos")
}

tasks.register("ciCheck") {
    group = "verification"
    description = "Run the normal checks and the full database integration suite."
    dependsOn(tasks.check, integrationTest)
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
    listOf("data/retail-pos-database"),
)
dataHelper(
    "importCatalog",
    "ImportCatalog",
    "Wipe products and stock, then load the TSV catalogue. Destructive. Quit the POS first.",
    listOf("data/retail-pos-database", "data/import-categories.tsv", "data/import-products.tsv"),
)
dataHelper(
    "insertCatalogRows",
    "InsertCatalogRows",
    "Insert or update PRODUCTS from a batch TSV. Pass: --args='JDBC_URL USER PASSWORD categories.tsv batch.tsv'",
    requireArgs = true,
    checkLock = false,
)
dataHelper(
    "loadFallbackCatalog",
    "LoadFallbackCatalog",
    "Replace the shared fallback catalogue from categories, products, and prices TSVs.",
    requireArgs = true,
    checkLock = false,
)
dataHelper(
    "keepCatalog",
    "KeepCatalog",
    "Drop products whose REFERENCE is not in a keep CSV. Pass: --args='data/retail-pos-database path/to/keep.csv'",
    requireArgs = true,
)
dataHelper(
    "updateResource",
    "UpdateResource",
    "Replace one RESOURCES row. Pass: --args='data/retail-pos-database NAME file'",
    requireArgs = true,
)
dataHelper(
    "dumpResource",
    "DumpResource",
    "Write named RESOURCES to a directory. Pass: --args='data/retail-pos-database outdir NAME...'",
    requireArgs = true,
)
dataHelper(
    "dumpAllResources",
    "DumpAllResources",
    "Dump all RESOURCES. Pass: --args='org.apache.derby.jdbc.EmbeddedDriver jdbc:derby:data/retail-pos-database outdir'",
    requireArgs = true,
    checkLock = false,
)
dataHelper(
    "showResource",
    "ShowResource",
    "Print named RESOURCES. Pass: --args='data/retail-pos-database NAME...'",
    requireArgs = true,
)
