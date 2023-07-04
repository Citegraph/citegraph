import { API_URL } from "./commons";

export async function getHotAuthors() {
  return fetch(`${API_URL}/discover/authors`).then((r) => r.json());
}

export async function getAuthor(id, limit, getEdges = true) {
  return fetch(
    `${API_URL}/author/${id}?limit=${limit}&getEdges=${getEdges}`
  ).then((r) => r.json());
}
