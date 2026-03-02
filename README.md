# openrocket-web

A modern, browser‑based re‑implementation of the OpenRocket v24 design workflow.
This project aims to bring the full OpenRocket experience to the web: rocket design, geometry visualization, parameter editing, motor selection, and flight simulation — all in a single screen, without scrolling.

---

# Overview

Openrocket‑Web is a web application that recreates the core workflow of OpenRocket v24 using a modern frontend and a backend powered by OpenRocket‑core.
The goal is to provide a clean, responsive, OpenRocket‑like interface that works on desktop and tablets (including iPad), while maintaining full compatibility with .ork files.

---

# Features

Rocket Design

• OpenRocket‑style component tree
• 2D side‑view geometry visualization (SVG)
• Real‑time updates when editing parameters
• Shape, material, position, mass, and aerodynamic properties
• No scrolling — everything fits on one screen


Motor Selection

• Motor database browsing
• Thrust curve visualization
• Motor assignment to stages and mounts


Flight Simulation

• Full physics simulation via OpenRocket‑core
• Altitude, velocity, acceleration, angle, CG/CP time‑series
• Event log (launch, burnout, apogee, deployment, landing)
• Graphs rendered in the browser


File Compatibility

• Import .ork files
• Export .ork files
• 100% compatible with OpenRocket v24


---

# Repository Structure

rocket-web/
 ├── backend/              # Java backend using OpenRocket-core
 │    ├── api/             # REST API endpoints
 │    ├── service/         # Business logic
 │    ├── core/            # OpenRocket-core integration
 │    ├── model/           # DTOs and JSON models
 │    ├── repository/      # Project storage (in-memory or DB)
 │    └── resources/       # Material lists, part types, config
 │
 ├── frontend/             # Web UI (HTML/JS/CSS)
 │    ├── public/          # index.html and static assets
 │    └── src/             # UI components, API client, state
 │
 ├── docs/                 # Architecture, API specs, design docs
 │
 ├── README.md
 └── LICENSE


---

# Technology Stack

Frontend

• HTML / CSS / JavaScript
• SVG for geometry rendering
• Chart.js for graphs
• Responsive layout (OpenRocket‑style 3‑pane UI)


Backend

• Java
• OpenRocket‑core
• Spring Boot (recommended)
• REST API (JSON)


---

# How It Works

1. The frontend handles:

• UI rendering
• Component tree
• 2D geometry
• Graphs
• User interactions


2. The backend handles:

• Loading and saving .ork files
• Running OpenRocket‑core calculations
• Running flight simulations
• Returning results as JSON


3. The frontend updates the UI based on backend responses.

---

# License

This project is licensed under the GNU General Public License v3 (GPLv3).
Because OpenRocket and OpenRocket‑core are GPLv3, any derivative work — including this web implementation — must also be released under GPLv3.

---

# Project Goals

• Recreate the OpenRocket v24 workflow in a modern web environment
• Provide a clean, single‑screen, OpenRocket‑style UI
• Maintain .ork compatibility
• Enable rocket design on tablets and browsers
• Keep the project open and community‑driven


---

# Status

This project is currently in early development.
Contributions, discussions, and feedback are welcome.

---

# Contributing

Pull requests and issue reports are encouraged.
Please check the docs/ folder for architecture and API specifications before contributing.

---

# Acknowledgements

This project is not affiliated with the official OpenRocket team.
OpenRocket is an open‑source project licensed under GPLv3.
Special thanks to the OpenRocket developers for creating an incredible tool for the rocketry community.

---
