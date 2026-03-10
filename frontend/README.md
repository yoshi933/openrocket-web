

# 📘 frontend/README.md（完成版）


# Openrocket‑Web Frontend

This directory contains the **web-based UI** for Openrocket‑Web.  
The goal of the frontend is to reproduce the **original OpenRocket v22 desktop UI** as faithfully as possible,  
including the three‑pane layout, component tree, 2D rocket view, and component editor panel.

The frontend communicates with the backend (Java + OpenRocket‑core) via REST APIs.

---

## 🎯 UI Goals

- Match the layout and behavior of the **OpenRocket v22 desktop UI**
- Three‑pane design:
  - **Left:** Component Tree
  - **Center:** 2D Side View (SVG)
  - **Right:** Component Editor
- Bottom tabs:
  - Design / Motors / Simulation / Flight Data
- Responsive layout (desktop‑first)
- Smooth interaction with backend APIs

---

## 📁 Folder Structure(supposition)

```
frontend/
 ├── public/
 │   ├── index.html
 │   ├── favicon.ico
 │   └── assets/
 │        ├── icons/
 │        └── images/
 │
 ├── src/
 │   ├── app/
 │   │    ├── App.js
 │   │    ├── MenuBar.js
 │   │    └── Tabs.js
 │   │
 │   ├── ui/
 │   │    ├── design/
 │   │    │    ├── ComponentTree/
 │   │    │    ├── RocketView/
 │   │    │    └── ComponentEditor/
 │   │    │
 │   │    ├── motors/
 │   │    └── simulation/
 │   │
 │   ├── api/
 │   ├── store/
 │   ├── utils/
 │   ├── styles/
 │   └── main.js
 │
 ├── package.json
 └── README.md
```

---

## 📂 Directory Responsibilities

### `public/`
Static files loaded directly by the browser.  
`index.html` contains the root `<div id="app">`.

---

### `src/app/`
Top‑level application structure.

- **App.js** — main layout wrapper  
- **MenuBar.js** — File / Edit / Tools / Help (OpenRocket‑style)  
- **Tabs.js** — bottom tab bar (Design / Motors / Simulation / Flight Data)

---

### `src/ui/design/`
Implements the **Design** tab.

#### ComponentTree/
Left pane: hierarchical component tree  
- Drag & drop support  
- Expand/collapse nodes  
- Matches OpenRocket’s structure

#### RocketView/
Center pane: 2D side view (SVG)  
- Draws rocket geometry  
- Updates when parts change  
- Uses geometry utilities

#### ComponentEditor/
Right pane: component property editor  
- Dynamic fields based on selected part  
- NumberField / SelectField / CheckboxField components

---

### `src/ui/motors/`
Implements the **Motors** tab.

- MotorBrowser — list of motors  
- MotorDetail — motor info  
- ThrustCurveGraph — thrust curve visualization

---

### `src/ui/simulation/`
Implements the **Simulation** tab.

- SimulationSettings — wind, launch angle, etc.  
- SimulationRunButton — triggers backend simulation  
- SimulationGraphs — altitude/velocity/acceleration graphs

---

### `src/api/`
REST API client for backend communication.

- `projects.js` — `/api/projects`  
- `parts.js` — `/api/projects/{id}/parts`  
- `motors.js` — `/api/motors`  
- `simulations.js` — `/api/projects/{id}/simulations`  
- `client.js` — shared fetch wrapper

---

### `src/store/`
Application state management.

- `projectStore.js` — current project  
- `componentStore.js` — component tree  
- `selectionStore.js` — selected component  
- `uiStore.js` — active tab, UI state

---

### `src/utils/`
Utility functions.

- `geometry.js` — SVG coordinate calculations  
- `svgBuilder.js` — generate SVG shapes for parts  
- `format.js` — number formatting  
- `throttle.js` — performance helpers

---

### `src/styles/`
CSS files for layout and components.

- `layout.css` — three‑pane layout  
- `theme.css` — colors, fonts  
- `components.css` — UI elements  
- `reset.css` — CSS reset

---

## 🔌 Connecting to the Backend

The backend must be running (locally or on Render).  
Set the backend URL in `src/api/client.js`:

```js
export const API_BASE = "https://your-backend.onrender.com";
```

During development:

```js
export const API_BASE = "http://localhost:8080";
```

---

## 🚀 Development

Install dependencies:

```
npm install
```

Start dev server:

```
npm run dev
```

Open:

```
http://localhost:5173
```

---

## 🧪 Testing Backend Integration

A test endpoint exists on the backend:

```
GET /api/projects/test-core
```

You can call it from the frontend to verify that OpenRocket‑core is working.

---

## 📌 Next Steps

- Implement ComponentTree rendering  
- Implement RocketView2D SVG drawing  
- Connect ComponentEditor to backend part updates  
- Implement motor browser and thrust curve graph  
- Implement simulation graphs  
- Add `.ork` import/export support  

---

## License

This project is licensed under **GPLv3**, consistent with OpenRocket and OpenRocket‑core.

```
