export const meta = () => {
  return {
    title: `About | Citegraph`,
  };
};

export default function About() {
  return (
    <div>
      <p>
        Citegraph is an open-source online web visualizer of citation networks,
        powered by graph database{" "}
        <a
          href="https://janusgraph.org/"
          target="_blank"
          rel="noreferrer"
          className="no-underline"
        >
          JanusGraph
        </a>{" "}
        and dblp dataset{" "}
        <a
          href="https://www.aminer.org/citation/"
          target="_blank"
          rel="noreferrer"
          className="no-underline"
        >
          DBLP-Citation-network V14
        </a>
        .
      </p>
      <p>
        This website is under active development. For advice/feedback/bug
        reports, please open an issue on{" "}
        <a
          href="https://github.com/citegraph/citegraph"
          target="_blank"
          rel="noreferrer"
          className="no-underline"
        >
          GitHub
        </a>
        . Any kind of contribution is welcome!
      </p>
    </div>
  );
}
