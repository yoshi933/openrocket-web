const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

/**
 * Fetch all parts for a project.
 * @param {string} projectId
 * @returns {Promise<Array>}
 */
export async function fetchParts(projectId) {
  const res = await fetch(`${API_BASE}/api/projects/${projectId}/parts`);
  if (!res.ok) throw new Error(`Failed to fetch parts for project ${projectId}: ${res.status}`);
  return res.json();
}

/**
 * Fetch a single part by ID.
 * @param {string} projectId
 * @param {string} partId
 * @returns {Promise<Object>}
 */
export async function fetchPart(projectId, partId) {
  const res = await fetch(`${API_BASE}/api/projects/${projectId}/parts/${partId}`);
  if (!res.ok) throw new Error(`Failed to fetch part ${partId}: ${res.status}`);
  return res.json();
}

/**
 * Create a new part for a project.
 * @param {string} projectId
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function createPart(projectId, data) {
  const res = await fetch(`${API_BASE}/api/projects/${projectId}/parts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Failed to create part: ${res.status}`);
  return res.json();
}

/**
 * Update an existing part.
 * @param {string} projectId
 * @param {string} partId
 * @param {Object} data
 * @returns {Promise<Object>}
 */
export async function updatePart(projectId, partId, data) {
  const res = await fetch(`${API_BASE}/api/projects/${projectId}/parts/${partId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Failed to update part ${partId}: ${res.status}`);
  return res.json();
}

/**
 * Delete a part.
 * @param {string} projectId
 * @param {string} partId
 * @returns {Promise<void>}
 */
export async function deletePart(projectId, partId) {
  const res = await fetch(`${API_BASE}/api/projects/${projectId}/parts/${partId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Failed to delete part ${partId}: ${res.status}`);
}
