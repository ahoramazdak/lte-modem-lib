# LTE Modem Library

Java library for managing LTE modem PDP contexts via AT commands.

## Features

- ✅ Attach/Detach PDP context
- ✅ Activate/Deactivate PDP
- ✅ Async API with `CompletableFuture`
- ✅ Event-driven architecture
- ✅ Unit tests with mock serial port
- ✅ Retry logic with exponential backoff
- ✅ GitHub Actions CI

## Build
mvn clean install

## Run Demo
mvn exec:java -Dexec.mainClass="com.example.ltemodem.demo.DemoMain"
