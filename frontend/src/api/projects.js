const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

/**
 * Fetch all projects.
 * @returns {Promise<Array>}
 */
export async function fetchProjects() {
  const res = await fetch(`${API_BASE}/api/projects`);
  if (!res.ok) throw new Error(`Failed to fetch projects: ${res.status}`);
  return res.json();
}

/**
 * Fetch a single project by ID.
 * @param {string} id
 * @returns {Promise<Object>}
 */
export async function fetchProject(id) {
  const res = await fetch(`${API_BASE}/api/projects/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch project ${id}: ${res.status}`);
  return res.json();
}

/**
 * Create a new project.
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function createProject(data) {
  const res = await fetch(`${API_BASE}/api/projects`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Failed to create project: ${res.status}`);
  return res.json();
}

/**
 * Delete a project by ID.
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteProject(id) {
  const res = await fetch(`${API_BASE}/api/projects/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Failed to delete project ${id}: ${res.status}`);
}
