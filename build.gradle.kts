plugins {
    id("java")
    id("com.adarshr.test-logger") version("3.2.0")
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "org.perilouscodpiece"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.8.0")
    implementation("org.projectlombok:lombok:1.18.34")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "org.perilouscodpiece.budbot.Main"
 }

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}
