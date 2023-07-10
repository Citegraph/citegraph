import { API_URL } from "./commons";

export async function getVertex(id, limit, getEdges = true) {
  return fetch(
    `${API_URL}/graph/vertex/${id}?limit=${limit}&getEdges=${getEdges}`
  ).then((r) => r.json());
}
