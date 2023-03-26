import { Link, useLoaderData } from "react-router-dom";
import { getAuthor } from "../apis/authors";
import React, { useEffect } from "react";

export async function loader({ params }) {
  const author = await getAuthor(params.authorId);
  console.log("author is", author);
  return { author };
}

export default function Author() {
  const { author } = useLoaderData();

  useEffect(() => {
    document.title = `${author.name} - Citegraph`;
  }, []);

  return (
    <div id="author">
      <div>Name: {author.name}</div>
      <div>
        Publications:
        <ul>
          {author.papers.map((paper, index) => (
            <li key={index}>
              <Link to={`/paper/${paper.id}`}>
                {paper.title}, {paper.year}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
