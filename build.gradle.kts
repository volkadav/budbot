plugins {
    id("java")
}

group = "org.perilouscodpiece"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.7.0")
    implementation("org.projectlombok:lombok:1.18.28")
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("org.slf4j:slf4j-jdk14:2.0.7")

    annotationProcessor("org.projectlombok:lombok:1.18.28")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "org.perilouscodpiece.budbot.Main"
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