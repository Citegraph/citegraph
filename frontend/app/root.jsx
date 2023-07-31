import {
  Outlet,
  Scripts,
  Link,
  Links,
  Meta,
  useLoaderData,
} from "@remix-run/react";
import React, { useState } from "react";
import { debounce } from "lodash";
import { getHotAuthors } from "./apis/authors";
import stylesheetUrl from "./index.css";
import { API_URL } from "./apis/commons";
import { MenuOutlined } from "@ant-design/icons";
import Header from "./header";

export const links = () => {
  return [
    { rel: "icon", href: "/favicon.ico" },
    { rel: "stylesheet", href: stylesheetUrl },
    { rel: "stylesheet", href: "/antd.css" },
  ];
};

export const meta = () => {
  return {
    title: `Citegraph | Open-Source Citation Networks Visualizer`,
    description:
      "Citegraph is an open-source online visualizer of 5+ million papers, 4+ million authors, and various relationships. " +
      "In total, Citegraph has 9.4 million vertices and 274 million edges.",
  };
};

export async function loader() {
  let authors = [];
  try {
    authors = await getHotAuthors();
  } catch (error) {
    console.error("Failed to fetch popular authors", error);
  }
  return { authors };
}

export default function Root() {
  const { authors } = useLoaderData();

  const [loading, setLoading] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [query, setQuery] = useState("");

  const [searchType, setSearchType] = useState("author");
  const [placeholder, setPlaceholder] = useState("Search author name");

  const handleSearchTypeChange = (value) => {
    setSearchType(value);
    if (value === "author") {
      setPlaceholder("Search author name");
      setSearchResults([]);
    } else {
      setPlaceholder("Search paper title");
      setSearchResults([]);
    }
  };

  const handleSearch = debounce((event) => {
    const query = event.target ? event.target.value : event;
    setQuery(query);
    if (!query) {
      setSearchResults([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    fetch(`${API_URL}/search/${searchType}/${query}`)
      .then((response) => response.json())
      .then((data) => {
        setSearchResults(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error fetching search results: ", error);
        setLoading(false);
      });
  }, 500);

  const [isSidebarActive, setSidebarActive] = useState(false);

  const toggleSidebar = () => {
    setSidebarActive(!isSidebarActive);
  };

  return (
    <html lang="en">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <Meta />
        <Links />
        <script
          async
          src="https://www.googletagmanager.com/gtag/js?id=G-Z7LN8421SX"
        ></script>
        <script
          dangerouslySetInnerHTML={{
            __html: `
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());

  gtag('config', 'G-Z7LN8421SX');
  `,
          }}
        ></script>
      </head>
      <body>
        <Scripts />
        <Header />
        <div id="root">
          <div id="hamburger-container">
            <button className="hamburger" onClick={toggleSidebar}>
              <MenuOutlined />
            </button>
          </div>
          <div
            id="sidebar-container"
            className={isSidebarActive ? "active" : ""}
          >
            <div id="sidebar">
              <nav>
                <p>
                  <b>People are searching</b>
                </p>
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
          </div>
          <div id="detail">
            <Outlet />
          </div>
        </div>
      </body>
    </html>
  );
}
