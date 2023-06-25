import { API_URL } from "./commons";

export async function getHotAuthors() {
  return fetch(`${API_URL}/discover/authors`).then((r) => r.json());
}

export async function getAuthor(id, limit = 100) {
  return fetch(`${API_URL}/author/${id}?limit=${limit}`).then((r) => r.json());
}
