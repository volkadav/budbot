# BudBot

A simple Telegram bot.

## Build

gradle clean test jar (or ./gradlew etc.)

(makes build/libs/budbot-{version string}.jar)

don't forget to put your Telegram api token in src/main/resources/apitoken
(for the actual #budhole budbot, gpg -d apitoken.asc in that dir before build)

## Run

java -jar /path/to/jarfile

## Tested on
JDK 17 (21 does not compile yet due to lombok/gradle/et al. issues)

linux x86
macos aarch64
