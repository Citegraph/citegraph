import { Outlet, Link, useLoaderData } from "react-router-dom";
import React, { useState } from "react";
import { getAuthors } from "../authors";
import logo from "../assets/logo.svg";

export async function loader() {
  const authors = await getAuthors();
  return { authors };
}

export default function Root() {
  const { authors } = useLoaderData();

  const [query, setQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);

  const handleSearch = (event) => {
    const query = event.target.value;
    setQuery(query);

    fetch(`http://localhost:8080/search/author/${query}`)
      .then((response) => response.json())
      .then((data) => {
        setSearchResults(data);
      })
      .catch((error) => {
        console.error("Error fetching search results: ", error);
      });
  };
  return (
    <>
      <div id="sidebar">
        <Link to="/">
          <img src={logo} id="logo" alt="Logo" />
        </Link>
        <div>
          <form id="search-form" role="search">
            <input
              id="q"
              aria-label="Search authors"
              placeholder="Search"
              type="search"
              name="q"
              value={query}
              onChange={handleSearch}
            />
            <div id="search-spinner" aria-hidden hidden={true} />
            <div className="sr-only" aria-live="polite"></div>

            {searchResults.map((result) => (
              <div key={result.id}>{result.name}</div>
            ))}
          </form>
        </div>
        <nav>
          {authors.length ? (
            <ul>
              {authors.map((author) => (
                <li key={author.id}>
                  <Link to={`author/${author.id}`}>{author.name}</Link>
                </li>
              ))}
            </ul>
          ) : (
            <p>
              <i>No authors</i>
            </p>
          )}
        </nav>
      </div>
      <div id="detail">
        <Outlet />
      </div>
    </>
  );
}
