# BudBot

A simple Telegram bot.

## Build

gradle clean test shadowJar (or ./gradlew etc.)

(makes build/libs/budbot-{version string}-all.jar)

don't forget to put your Telegram api token in src/main/resources/apitoken
(for the actual #budhole budbot, gpg -d apitoken.asc in that dir before build)

## Run

standalone: java -jar /path/to/jarfile

systemd example unit (in /etc/systemd/system/budbot.service):
```
[Unit]
Description=budbot (telegram bot for budhole)
After=syslog.target network.target

[Service]
SuccessExitStatus=143

User=www-data
Group=www-data

Type=simple

Environment="JAVA_HOME=/whatever/your/java_home/is"
Environment="logging.level.app=INFO"
Environment="logging.level.root=WARN"
WorkingDirectory=/path/to/dir/goes/here
ExecStart=/usr/bin/java -jar budbot.jar
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
```
The usual commands then work like:
- systemctl daemon-reload
- systemctl start/stop/restart/status budbot
- journalctl -fu budbot
- etc.

## Tested on
JDK 17 (21 does not compile yet due to lombok/gradle/et al. issues)

linux x86
macos aarch64
