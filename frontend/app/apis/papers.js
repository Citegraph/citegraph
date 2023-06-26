import { API_URL } from "./commons";

export async function getPaper(id, limit) {
  return fetch(`${API_URL}/paper/${id}?limit=${limit}`).then((r) => r.json());
}
