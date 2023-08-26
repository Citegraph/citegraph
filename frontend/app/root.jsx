import {
  Outlet,
  Scripts,
  Link,
  Links,
  useNavigation,
  useLocation,
  Meta,
  useLoaderData,
} from "@remix-run/react";
import React, { useEffect } from "react";
import { getHotAuthors } from "./apis/authors";
import stylesheetUrl from "./index.css";
import { Skeleton } from "antd";
import Header from "./header";
import { useViewedAddresses, ViewedAddressesProvider } from "./address";

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

function Root() {
  const { authors } = useLoaderData();

  const navigation = useNavigation();

  const location = useLocation();
  const { historyList, setHistoryList } = useViewedAddresses();

  useEffect(() => {
    let currentTitle = document.title;
    // Remove " - Citegraph" from the tail if it exists
    const suffix = " - Citegraph";
    if (currentTitle.endsWith(suffix)) {
      currentTitle = currentTitle.slice(0, -suffix.length);
    }

    setHistoryList((prevHistory) => {
      // Excluding homepage or graph visualization (including playground) page
      if (
        location.pathname == "/" ||
        location.pathname.startsWith("/visualizer") ||
        location.pathname.startsWith("/playground")
      ) {
        return prevHistory;
      }
      // Limiting to last 10 visited entries
      const newHistory = [
        { path: location.pathname, title: currentTitle },
        ...prevHistory,
      ].slice(0, 10);
      return newHistory;
    });
  }, [location.pathname]);

  return (
    <div id="root">
      <div id="sidebar-container">
        <div id="sidebar">
          <nav>
            <p>
              <b>Playground</b>
            </p>
            <ul>
              <li key="path">
                <Link to={"/playground/shortest-path/"}>
                  Shortest Path Finder
                </Link>
                <Link to={"/playground/cluster/"}>Community Detector</Link>
              </li>
            </ul>
          </nav>
          {historyList.length > 0 && (
            <nav>
              <p>
                <b>Recently viewed</b>
              </p>
              <ul>
                {historyList.map((entry, index) => (
                  <li key={index}>
                    <Link
                      to={entry.path}
                      className="truncate-text"
                      title={entry.title}
                    >
                      {entry.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          )}
          {authors.length > 0 && (
            <nav>
              <p>
                <b>Author Trending</b>
              </p>
              <ul>
                {authors.map((author) => (
                  <li key={author.id}>
                    <Link to={`author/${author.id}`}>{author.name}</Link>
                  </li>
                ))}
              </ul>
            </nav>
          )}
        </div>
      </div>
      <div id="detail">
        {navigation.state == "loading" ? <Skeleton active /> : <Outlet />}
      </div>
    </div>
  );
}

export default function App() {
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
        <ViewedAddressesProvider>
          <Root />
        </ViewedAddressesProvider>
      </body>
    </html>
  );
}
