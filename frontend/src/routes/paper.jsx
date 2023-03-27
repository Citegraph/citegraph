import { Link, useLoaderData } from "react-router-dom";
import { getPaper } from "../apis/papers";
import React, { useEffect } from "react";

export async function loader({ params }) {
  const paper = await getPaper(params.paperId);
  console.log("paper is", paper);
  return { paper };
}

export default function Paper() {
  const { paper } = useLoaderData();

  useEffect(() => {
    document.title = `${paper.title} - Citegraph`;
  }, []);

  return (
    <div id="paper">
      <div id="title">
        {paper.title} ({paper.year})
      </div>
      <div id="desc">
        <p>Number of papers which cited this paper: {paper.numOfReferers}</p>
        <p>Number of papers cited by this paper: {paper.numOfReferees}</p>
      </div>
      <div>
        Authors:
        <ul>
          {paper.authors.map((author, index) => (
            <li key={index}>
              <Link to={`/author/${author.id}`}>{author.name}</Link>
            </li>
          ))}
        </ul>
      </div>
      <div>
        Citations:
        <ul>
          {paper.referees.map((paper, index) => (
            <li key={index}>
              <Link to={`/paper/${paper.id}`}>
                {paper.title} ({paper.year})
              </Link>
            </li>
          ))}
        </ul>
      </div>
      <div>
        Cited by:
        <ul>
          {paper.referers.map((paper, index) => (
            <li key={index}>
              <Link to={`/paper/${paper.id}`}>
                {paper.title} ({paper.year})
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
