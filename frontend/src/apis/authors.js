export async function getHotAuthors() {
  return fetch("http://localhost:8080/discover/authors").then((r) => r.json());
}

export async function getAuthor(id) {
  return fetch("http://localhost:8080/author/" + id).then((r) => r.json());
}
