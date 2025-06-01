import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.hierynomus:sshj:0.40.0")
    }
}

fun loadEnvFile(file: File): Properties {
    val props = Properties()
    if (file.exists()) {
        props.load(file.inputStream())
    }
    return props
}

val env = loadEnvFile(rootProject.file(".env"))

plugins {
    id("java")
    id("com.adarshr.test-logger") version ("3.2.0")
    id("com.github.johnrengelman.shadow") version ("8.1.1")
}

group = "org.perilouscodpiece"
version = "1.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.9.7.1") {
        exclude("commons-codec","commons-codec")
        exclude("ch.qos.logback", "logback-classic")
        exclude("ch.qos.logback", "logback-core")
    }
    implementation("commons-codec:commons-codec:1.17.1")
    implementation("org.projectlombok:lombok:1.18.34")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "org.perilouscodpiece.budbot.Main"
    // build uberjar:
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
 }

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.register("deploy") {
    dependsOn("build")

    doLast {
        val host = env.getProperty("DEPLOY_HOST") ?: error("DEPLOY_HOST not set")
        val user = env.getProperty("DEPLOY_USER") ?: error("DEPLOY_USER not set")
        val keyPath = env.getProperty("DEPLOY_SSH_KEY") ?: error("DEPLOY_SSH_KEY not set")
        val remotePath = env.getProperty("DEPLOY_PATH") ?: error("DEPLOY_PATH not set")

        val jarFile = tasks.named("jar").get().outputs.files.singleFile

        println("Deploying ${jarFile.name} to $user@$host:$remotePath using ssh keyfile $keyPath")

        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier()) // WARNING: disables host key checking - use with care
        ssh.connect(host)

        val keyProvider: KeyProvider = ssh.loadKeys(keyPath)
        ssh.authPublickey(user, keyProvider)

        val sftp = ssh.newSFTPClient()
        sftp.put(jarFile.absolutePath, "$remotePath")
        sftp.close()

        ssh.disconnect()

        println("Deploy complete!")
    }
}