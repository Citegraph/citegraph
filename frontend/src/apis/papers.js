import { API_URL } from "./commons";

export async function getPaper(id) {
  return fetch(`${API_URL}/paper/${id}`).then((r) => r.json());
}
