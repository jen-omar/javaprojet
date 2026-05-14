# Mythoria Desktop

Mythoria Desktop is a JavaFX application for fantasy content and community management. It includes authentication, dashboard navigation, profile and wallet tools, worlds, events, marketplace features, Kinship collaboration workflows, and AI-assisted utilities.

## Requirements

- Java 17
- MySQL running locally
- Maven Wrapper included in the project

The app uses the database configured in:

```text
src/main/resources/config.properties
```

## Run The App

Because this project is inside a Windows path containing parentheses, `mvnw.cmd` can fail. Use the wrapper JAR directly from the project root:

```powershell
cd "C:\Users\windows 10\Downloads\javaprojet 33 (2)\javaprojet 33 (2)\javaprojet 33\javaprojet"
& "C:\Program Files\Java\jdk-17\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=$PWD" -cp .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain javafx:run
```

## Compile

```powershell
& "C:\Program Files\Java\jdk-17\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=$PWD" -cp .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -q -DskipTests compile
```

## Main Entry Point

```text
src/main/java/tn/esprit/Main.java
```

The JavaFX application class is:

```text
src/main/java/tn/esprit/controllers/HelloApplication.java
```

## Main Modules

- Dashboard: central navigation shell
- Worlds: create, edit, browse, and enter fantasy worlds
- Happenings: event management by local venue
- Bazaar: marketplace feature area
- Kinship: collaboration requests, proposals, reports, and discussion access
- Profile: user profile, identity verification, and wallet access

## Notes

- Login email alerts require mail configuration; without it, the app can still run but logs a mail configuration warning.
- Some FXML files may warn about JavaFX API version 21 while running on JavaFX 17. The app currently compiles and runs on Java 17.
- Generated files such as invoices, QR codes, and AI images are written to local project folders.
