# Vertretungsplan App
A Java application that fetches class substitutions and matches them with a student's timetable from BonniWeb.

## Features
- Login to BonniWeb and fetch substitution data
- Parse timetable PDFs
- Match substitutions with your courses
- JavaFX GUI for displaying results

## How to use
> Just download from the [Release](https://github.com/sys-skye/Vertretungsplan_Java/releases/tag/1Release) page

---

# Build it yourself

## Prerequisites
- Java 17+
- Maven 3.6+
- BonniWeb account credentials

## Setup
1. Clone or download the project

OPTIONAL
---
3. Set your BonniWeb credentials as environment variables:
   ```bash
   set BONNIWEB_USER=your_username
   set BONNIWEB_PASS=your_password
   ```
   
   Or edit `src/main/resources/config.properties`:
   ```properties
   username=your_username
   password=your_password
   ```
---

## Build

```bash
mvn clean package
```

## Run

```bash
mvn javafx:run
```

## Project Structure

- `Launcher.java` - Entry point with JavaFX GUI
- `Vertretungsplan.java` - Main logic for fetching and processing data
- `BonniwebClient.java` - Handles BonniWeb authentication and data fetching
- `TimetableMatcher.java` - Matches substitutions with timetable
- `CourseMatcher.java` - Maps courses to substitution entries

## License

Unlicensed
