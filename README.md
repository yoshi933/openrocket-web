# openrocket-web

This project has just begun. The goal of this project is to make OpenRocket, which is famous for its rocket simulation, available on desktop applications such as iPads and devices that do not run Java. There are no restrictions on this project. The current plan is to implement the front end in HTML (or React).

I'm not a genius programmer. I would like to advance the project while utilizing AI.

（This README has been translated）

backend/
 ├── src/
 │   ├── main/
 │   │   ├── java/
 │   │   │   └── com/example/rocketweb/
 │   │   │        ├── api/
 │   │   │        │    ├── ProjectController.java
 │   │   │        │    ├── PartController.java
 │   │   │        │    ├── SimulationController.java
 │   │   │        │    └── MotorController.java
 │   │   │        │
 │   │   │        ├── service/
 │   │   │        │    ├── ProjectService.java
 │   │   │        │    ├── PartService.java
 │   │   │        │    ├── SimulationService.java
 │   │   │        │    └── MotorService.java
 │   │   │        │
 │   │   │        ├── core/
 │   │   │        │    ├── OrkLoader.java
 │   │   │        │    ├── OrkWriter.java
 │   │   │        │    ├── RocketCalculator.java
 │   │   │        │    └── SimulationRunner.java
 │   │   │        │
 │   │   │        ├── model/
 │   │   │        │    ├── ProjectDto.java
 │   │   │        │    ├── PartDto.java
 │   │   │        │    ├── SimulationDto.java
 │   │   │        │    ├── SimulationResultDto.java
 │   │   │        │    └── MotorDto.java
 │   │   │        │
 │   │   │        ├── repository/
 │   │   │        │    ├── ProjectRepository.java
 │   │   │        │    └── InMemoryProjectRepository.java
 │   │   │        │
 │   │   │        └── RocketWebApp.java
 │   │   │
 │   │   └── resources/
 │   │        ├── materials.json
 │   │        ├── part-types.json
 │   │        ├── fin-shapes.json
 │   │        └── application.yml
 │   │
 │   └── test/
 │        └── （テストコード）
 │
 ├── build.gradle  または  pom.xml
 └── README.md
