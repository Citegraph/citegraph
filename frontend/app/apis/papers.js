import { API_URL } from "./commons";

export async function getPaper(id, limit, getEdges = true) {
  return fetch(
    `${API_URL}/paper/${id}?limit=${limit}&getEdges=${getEdges}`
  ).then((r) => r.json());
}
