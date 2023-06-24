import { API_URL } from "./commons";

export async function getHotAuthors() {
  return fetch(`${API_URL}/discover/authors`).then((r) => r.json());
}

export async function getAuthor(id) {
  return fetch(`${API_URL}/author/${id}`).then((r) => r.json());
}
