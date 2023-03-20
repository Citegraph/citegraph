import { Outlet, Link, useLoaderData } from "react-router-dom";
import { getAuthors } from "../authors";

export async function loader() {
    const authors = await getAuthors();
    return { authors };
  }

export default function Root() {
    const { authors } = useLoaderData();
    return (
      <>
        <div id="sidebar">
          <h1>React Router authors</h1>
          <div>
            <form id="search-form" role="search">
              <input
                id="q"
                aria-label="Search authors"
                placeholder="Search"
                type="search"
                name="q"
              />
              <div
                id="search-spinner"
                aria-hidden
                hidden={true}
              />
              <div
                className="sr-only"
                aria-live="polite"
              ></div>
            </form>
            <form method="post">
              <button type="submit">New</button>
            </form>
          </div>
          <nav>
            {authors.length ? (
                <ul>
                {authors.map((author) => (
                    <li key={author.id}>
                    <Link to={`author/${author.id}`}>
                        {author.name}
                    </Link>
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