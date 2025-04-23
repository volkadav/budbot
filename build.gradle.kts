plugins {
    id("java")
    id("com.adarshr.test-logger") version ("3.2.0")
    id("com.github.johnrengelman.shadow") version ("8.1.1")
}

group = "org.perilouscodpiece"
version = "1.2"

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
