# 🎵 myPod 

A retro iPod-inspired music player built with **JavaFX**, **JPA/Hibernate**, and the **iTunes Search API**. Browse artists, albums, and songs through a nostalgic click-wheel interface, manage playlists, and preview tracks — all backed by a MySQL database.

![Java](https://img.shields.io/badge/Java-25-orange)
![Hibernate](https://img.shields.io/badge/Hibernate-7.2-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-25-green)
![MySQL](https://img.shields.io/badge/MySQL-9.5-blue)

---
## 👥 About This Project

This project was originally developed as a group assignment during my studies.
I've forked it to my personal portfolio as it represents work I'm proud of.

My main contributions were:
- **Entity layer** — designing and implementing the JPA entities
- **Repository layer** — repository interfaces and their implementations (CRUD)
- **Test suite** — writing the full repository test coverage

> The original project was a collaborative effort, and credit goes to the whole team for the overall design and features!

---

## ✨ Features

- **iPod-style UI** — Navigate menus with keyboard (arrow keys, Enter, Escape) through an authentic click-wheel design
- **iTunes API integration** — Automatically fetches artists, albums, songs, and album artwork from Apple's iTunes Search API
- **Song previews** — Stream 30-second song previews with playback controls and volume adjustment
- **Playlist management** — Create, edit, and browse custom playlists via a dedicated playlist editor
- **Persistent storage** — All music data and playlists are stored in a MySQL database using JPA/Hibernate (code-first)
- **Album artwork** — Cover images are downloaded and stored as BLOBs, displayed on the Now Playing screen

## 🏗️ Architecture

```
org.example
├── entity/                        # JPA entities: Artist, Album, Song, Playlist
├── repo/                          # Repository interfaces & implementations (CRUD)
├── logging/                       # Custom logging connection wrapper
├── App                            # Application entry point
├── MyPod                          # JavaFX Application (UI, navigation, playback)
├── DatabaseInitializer            # Seeds the database from the iTunes API
├── ItunesApiClient                # HTTP client for the iTunes Search API
├── ItunesDTO                      # Data transfer object for API responses
├── ItunesPlayList                 # Playlist editor window
├── PersistenceManager             # Shared EntityManagerFactory provider
└── EntityManagerFactoryProvider   # EMF configuration & creation
```

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 25** | Language & runtime |
| **JavaFX 25** | Graphical user interface |
| **Hibernate 7.2** | ORM / JPA provider |
| **MySQL 9.5** | Production database |
| **H2** | In-memory test database |
| **Jackson** | JSON parsing (iTunes API) |
| **HikariCP** | Connection pooling |
| **Log4j 2** | Logging framework |
| **JUnit 6 / AssertJ / Mockito** | Testing |
| **Docker Compose** | Database container orchestration |

### Data Model

| Relationship | Type |
|---|---|
| Artist → Album | One-to-Many |
| Album → Song | One-to-Many |
| Playlist ↔ Song | Many-to-Many |

## 📋 Requirements

- **Java 25** (or compatible JDK)
- **Maven 3.9+**
- **Docker & Docker Compose** (for the MySQL database)

## 🚀 Getting Started

### 1. Start the database

```bash
docker compose up -d
```

This launches a MySQL 9.5 container with:
- **Database:** `myPodDB`
- **Port:** `3306`
- **User / Password:** `user` / `pass`

### 2. Build and run

```bash
mvn clean javafx:run
```

On first launch, the app will:
1. Auto-create all database tables (via Hibernate `hbm2ddl.auto=update`)
2. Fetch song data from the iTunes API for a curated set of artists
3. Create default playlists ("Library" and "Favorites")

## 🕹️ Controls

| Key | Action                                               |
|---|------------------------------------------------------|
| ↑ / ↓ | Navigate menus · Adjust volume on Now Playing screen |
| Enter | Select / Confirm                                     |
| Escape | Go back                                              |

## 🧪 Testing

Tests use an **H2 in-memory database** so no external services are needed:

```bash
mvn test
```

Test suites cover the repository layer for all core entities:
- `SongRepoTest`
- `ArtistRepoTest`
- `AlbumRepoTest`
- `PlaylistRepoTest`
