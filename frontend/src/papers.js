export async function getPaper(id) {
  return fetch("http://localhost:8080/paper/" + id).then((r) => r.json());
}
