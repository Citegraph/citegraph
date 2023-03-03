import localforage from "localforage";
import { matchSorter } from "match-sorter";
import sortBy from "sort-by";

export async function getAuthors(query) {
  await fakeNetwork(`getAuthors:${query}`);
  let authors = await localforage.getItem("authors");
  if (!authors) authors = [{"name": "Jingren Zhou", "id": 1}];
  if (query) {
    authors = matchSorter(authors, query, { keys: ["name"] });
  }
  return authors.sort(sortBy("name"));
}

export async function getAuthor(id) {
  return fetch("http://localhost:8080/author/" + id).then(r => r.json());
}

function set(authors) {
  return localforage.setItem("authors", authors);
}

// fake a cache so we don't slow down stuff we've already seen
let fakeCache = {};

async function fakeNetwork(key) {
  if (!key) {
    fakeCache = {};
  }

  if (fakeCache[key]) {
    return;
  }

  fakeCache[key] = true;
  return new Promise(res => {
    setTimeout(res, Math.random() * 800);
  });
}