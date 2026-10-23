# Yu-Gi-Oh! Random Card Generator

A desktop application built with **Java and JavaFX** to generate random Yu-Gi-Oh! cards, apply filters, and export decks compatible with **YGO Omega**.

## Requirements

- **Java JDK 21**
- Internet connection (for downloading dependencies)

Maven installation is not required, as the project includes **Maven Wrapper**.

## Installation & Running

### 1. Download the repository

Clone the repository:

```bash
git clone https://github.com/alessandrojack/RandomCardGeneratorYugioh.git
cd RandomCardGeneratorYugioh
```

Alternatively, download the ZIP from GitHub and extract it.

### 2. Run the application

Open a terminal in the project's root directory and execute:

**macOS / Linux**
```bash
./mvnw clean javafx:run
```

If you encounter permission issues:
```bash
chmod +x mvnw
```

**Windows**
```powershell
.\mvnw.cmd clean javafx:run
```

On the first launch, Maven will automatically download the required dependencies.

## Technologies

- Java 21
- JavaFX 21
- Maven
- SQLite
- Gson
- ControlsFX

## Author

Developed (vibecoded) by [alessandrojack](https://github.com/alessandrojack).
