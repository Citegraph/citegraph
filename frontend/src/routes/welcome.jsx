import React, { useEffect } from "react";
import { Link } from "react-router-dom";
import { Tooltip, Button } from "@mui/material";
import Typography from "@mui/material/Typography";

export default function Welcome() {
  useEffect(() => {
    document.title = `Citegraph`;
  }, []);

  return (
    <div className="welcome">
      <div className="intro">
        <Typography variant="h2" gutterBottom>
          Welcome to Citegraph
        </Typography>
        <Typography variant="body1" gutterBottom>
          Citegraph is an open-source online visualizer of 5+ million papers, 4+
          million authors, and various relationships. In total, Citegraph has
          9.4 million vertices and 274 million edges.
        </Typography>
        <Typography variant="body1" gutterBottom>
          Citegraph supports navigation of:
        </Typography>
        <Typography variant="h5" gutterBottom>
          Paper ---cites--&gt; Paper relationships
        </Typography>
        <Typography variant="body1" gutterBottom>
          Citegraph contains 32+ million paper citation relationships
        </Typography>
        <Typography variant="h5" gutterBottom>
          Author ---writes--&gt; Paper relationships
        </Typography>
        <Typography variant="body1" gutterBottom>
          Citegraph contains 16+ million authorship relationships
        </Typography>
        <Typography variant="h5" gutterBottom>
          Author ---cites--&gt; Author relationships
        </Typography>
        <Typography variant="body1" gutterBottom>
          Citegraph contains 224+ million author citation relationships. We
          deduce the relationship that A cites B if A has ever (co)authored a
          paper which cites another paper written by B.
        </Typography>
        <Typography variant="body1" gutterBottom>
          For example,{" "}
          <Link to={`/author/53f366a7dabfae4b3499c6fe`}>Geoffrey Hinton</Link>{" "}
          has ever cited 1.8k people, and more than 65k people have cited him.
        </Typography>
      </div>
      <div className="footer">
        <Typography variant="body1" gutterBottom>
          This website is powered by{" "}
          <a
            href="https://janusgraph.org/"
            target="_blank"
            rel="noreferrer"
            className="no-underline"
          >
            JanusGraph
          </a>{" "}
          and{" "}
          <a
            href="https://www.aminer.org/citation/"
            target="_blank"
            rel="noreferrer"
            className="no-underline"
          >
            DBLP-Citation-network V14
          </a>
        </Typography>
      </div>
    </div>
  );
}
