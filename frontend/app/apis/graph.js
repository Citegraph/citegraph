import { API_URL } from "./commons";

export async function getVertex(id, limit, getEdges = true) {
  return fetch(
    `${API_URL}/graph/vertex/${id}?limit=${limit}&getEdges=${getEdges}`
  ).then((r) => r.json());
}

export async function getPath(fromId, toId) {
  return fetch(`${API_URL}/graph/path?fromId=${fromId}&toId=${toId}`).then(
    (r) => r.json()
  );
}

export async function getCluster(id) {
  return fetch(`${API_URL}/graph/cluster/${id}`).then((r) => r.json());
}

export async function getCitationNetwork(id) {
  return fetch(`${API_URL}/graph/citations/${id}`).then((r) => r.json());
}
