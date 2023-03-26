import React, { useEffect } from "react";

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
        <ul>
          <li>Paper ---cites--&gt; Paper relationships</li>
          <li>bbb</li>
        </ul>
      </p>
    </div>
  );
}
