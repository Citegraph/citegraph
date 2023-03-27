import { Link, useLoaderData } from "react-router-dom";
import { getAuthor } from "../apis/authors";
import React, { useEffect } from "react";

export async function loader({ params }) {
  const author = await getAuthor(params.authorId);
  return { author };
}

export default function Author() {
  const { author } = useLoaderData();

  useEffect(() => {
    document.title = `${author.name} - Citegraph`;
  }, []);

  return (
    <div id="author">
      <div id="name">{author.name}</div>
      <div id="desc">
        <p>Number of papers: {author.numOfPapers}</p>
        <p>Number of people who cited this author: {author.numOfReferers}</p>
        <p>Number of people cited by this author: {author.numOfReferees}</p>
      </div>
      <div id="pub">
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
      <div id="referer">
        People who cited this author:
        <ul>
          {author.referers.map((ppl, index) => (
            <li key={index}>
              <Link to={`/author/${ppl.author.id}`}>{ppl.author.name}</Link>{" "}
              {ppl.count}
            </li>
          ))}
        </ul>
      </div>
      <div id="referee">
        People who this author cited:
        <ul>
          {author.referees.map((ppl, index) => (
            <li key={index}>
              <Link to={`/author/${ppl.author.id}`}>{ppl.author.name}</Link>{" "}
              {ppl.count}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
