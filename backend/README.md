
# 📘 backend/README.md（完成版）


# Openrocket‑Web Backend

This directory contains the **Java backend** for the Openrocket‑Web project.  
It provides a REST API that exposes OpenRocket‑core functionality to the frontend.

The backend is responsible for:
- Loading and saving `.ork` files  
- Managing rocket projects and parts  
- Running OpenRocket‑core calculations (CG, CP, mass, stability, etc.)  
- Running full flight simulations  
- Returning results as JSON for the frontend  

The frontend never interacts with OpenRocket‑core directly — all communication goes through this backend.

---

## 📁 Folder Structure

```
backend/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/example/rocketweb/
 │   │   │    ├── api/
 │   │   │    ├── service/
 │   │   │    ├── core/
 │   │   │    ├── model/
 │   │   │    ├── repository/
 │   │   │    └── RocketWebApp.java
 │   │   └── resources/
 │   │        ├── materials.json
 │   │        ├── part-types.json
 │   │        ├── fin-shapes.json
 │   │        └── application.yml
 │   └── test/
 ├── build.gradle (or pom.xml)
 └── README.md
```

---

## 📂 Directory Responsibilities

### `api/` — REST Controllers (Entry Points)
Defines all HTTP endpoints used by the frontend.

- `ProjectController.java`  
  Create/load/export projects, import `.ork` files.

- `PartController.java`  
  Add, update, delete rocket parts.

- `SimulationController.java`  
  Run simulations and fetch results.

- `MotorController.java`  
  Provide motor lists and thrust curve data.

These classes should contain **no business logic** — only request/response handling.

---

### `service/` — Business Logic Layer
Implements the actual operations requested by the API.

- `ProjectService.java`  
  Manages project lifecycle and summary calculations.

- `PartService.java`  
  Updates part properties and triggers recalculation.

- `SimulationService.java`  
  Prepares and runs simulations.

- `MotorService.java`  
  Loads motor data and assigns motors to mounts.

This layer coordinates between the repository and the OpenRocket‑core adapter.

---

### `core/` — OpenRocket‑core Integration
A thin wrapper around OpenRocket‑core.

- `OrkLoader.java`  
  Loads `.ork` files into Java objects.

- `OrkWriter.java`  
  Converts Java objects back into `.ork`.

- `RocketCalculator.java`  
  Computes CG, CP, mass, stability, etc.

- `SimulationRunner.java`  
  Executes full flight simulations and returns time‑series data.

This layer isolates OpenRocket‑core so the rest of the backend stays clean.

---

### `model/` — DTOs (Data Transfer Objects)
Defines the JSON structures exchanged with the frontend.

- `ProjectDto.java`  
- `PartDto.java`  
- `SimulationDto.java`  
- `SimulationResultDto.java`  
- `MotorDto.java`

These classes should contain **no logic**, only fields.

---

### `repository/` — Project Storage
Stores active projects in memory (or a database later).

- `ProjectRepository.java`  
  Interface for saving/loading projects.

- `InMemoryProjectRepository.java`  
  Simple in‑memory implementation using a map.

This allows the backend to keep multiple projects active at once.

---

### `resources/` — Static Data & Configuration
Contains fixed data used by the backend.

- `materials.json` — material list (density, name, etc.)  
- `part-types.json` — available part types  
- `fin-shapes.json` — fin shape definitions  
- `application.yml` — Spring Boot configuration  

These files are loaded at startup.

---

## 🚀 Running Locally

### Build
```
./gradlew build
```

### Run
```
./gradlew bootRun
```

The backend will start at:

```
http://localhost:8080
```

Test endpoint:
```
GET /api/health
```

---

## 🌐 Deploying to Render

Render can automatically build and run this backend.

### Build Command
```
./gradlew build
```

### Start Command
```
java -jar build/libs/backend-*.jar
```

Enable **Auto Deploy** so every push to `main` updates the server.

---

## 🤖 GitHub Copilot Notes

This backend is structured to be Copilot‑friendly:

- Clear separation of layers (`api`, `service`, `core`, `model`, `repository`)
- DTOs are simple and predictable
- Controllers contain minimal logic
- Services follow consistent naming and patterns
- OpenRocket‑core integration is isolated in `core/`

Copilot can:
- Autocomplete new endpoints  
- Generate DTOs  
- Suggest service logic  
- Infer JSON structures  
- Help maintain consistent architecture  

---

## 📌 Next Steps for Development

1. Implement `/api/projects` (create + list)  
2. Implement `.ork` import via `OrkLoader`  
3. Implement part editing (`PUT /api/projects/{id}/parts/{partId}`)  
4. Implement summary calculation via `RocketCalculator`  
5. Implement simulation execution  

Once these are done, the frontend can fully interact with the backend.

---

## License

This backend is licensed under **GPLv3**, consistent with OpenRocket and OpenRocket‑core.
