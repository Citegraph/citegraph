import React, { useEffect } from "react";
import { Link } from "react-router-dom";

export default function Welcome() {
  useEffect(() => {
    document.title = `Citegraph`;
  }, []);

  return (
    <div id="intro">
      <h1>Welcome to Citegraph</h1>
      <p>
        Citegraph is an open-source online visualizer of papers, authors, and
        citation relationships. Citegraph supports navigation of:
      </p>
      <h3>Paper ---cites--&gt; Paper relationships</h3>
      <p></p>
      <h3>Author ---writes--&gt; Paper relationships</h3>
      <p></p>
      <h3>Author ---cites--&gt; Author relationships</h3>
      <p>
        We deduce the relationship that A cites B if A has ever (co)authored a
        paper which cites another paper written by B.
      </p>
      <p>
        For example,{" "}
        <Link to={`/author/53f366a7dabfae4b3499c6fe`}>Geoffrey Hinton</Link> has
        ever cited 1.8k people, and more than 65k people have cited him.
      </p>
    </div>
  );
}
