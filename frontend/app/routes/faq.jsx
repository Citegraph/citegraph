import { Collapse } from "antd";
import React from "react";

const items = [
  {
    key: "1",
    label: "Is Citegraph free? Why did you build this website?",
    children: (
      <div>
        <p>
          Citegraph is and will always be free and open-source without ads. If
          you'd like to build and host the platform yourself, please follow the
          guideline on{" "}
          <a
            href="https://github.com/citegraph/citegraph"
            target="_blank"
            rel="noreferrer"
          >
            GitHub
          </a>
          .
        </p>
        <p>
          This website was initially built as a demo of{" "}
          <a href="https://janusgraph.org" target="_blank" rel="noreferrer">
            JanusGraph
          </a>
          , an open-source graph database, but we felt it could be useful for
          researchers, so we are committed to developing and maintaining it. We
          don't accept donation but contribution and feedback are always
          welcome.
        </p>
      </div>
    ),
  },
  {
    key: "2",
    label: "Why is some data missing/wrong? Why is data not up-to-date?",
    children: (
      <div>
        <p>
          The information in Citegraph comes from open-source datasets.
          Sometimes, there might be mistakes or gaps in this data. One common
          issue is confusion between authors who share the same name. This can
          lead to an author having more than one page, or different authors
          being mixed up as one person.
        </p>
        <p>
          As of now, the data set purely comes from DBLP-Citation-network V14,
          an open-source citation dataset published on 2023-01-31.
        </p>
        <p>
          If you spot any mistakes or missing data, please let us know. You can
          do this by raising an issue on our GitHub page. We'll do our best to
          fix it manually.
        </p>
      </div>
    ),
  },
  {
    key: "3",
    label: "What are `REFERS` edges? What are `referers` and `referees`?",
    children: (
      <div>
        <p>
          Citegraph draws an edge from author A to author B if A has ever
          written a paper X that cited a paper Y written by author B, i.e. A
          ---writes--&gt; X ---cites--&gt; Y &lt;--writes--- B. We call this
          edge as `REFER` edge. The edge also has a counter which records how
          many times A has `referred` B.
        </p>
        <p>
          `Referers` of an author X are people who have `referred` X. If you are
          looking for potential collaborators or people who can write you
          reference letters, this feature can be handy.
        </p>
        <p>
          `Referees` of an author X are people who X has `referred`. If you are
          interested in researcher X's works, this feature can help you find out
          whose works X has been following.
        </p>
      </div>
    ),
  },
  {
    key: "4",
    label: "How is pagerank calculated?",
    children: (
      <div>
        <p>
          Pagerank is calculated using the classic page rank algorithm by{" "}
          <a
            href="https://tinkerpop.apache.org/docs/current/reference/#pagerankvertexprogram"
            target="_blank"
            rel="noreferrer"
          >
            PageRankVertexProgram
          </a>
          . All papers are involved in the calculation and they vote for the
          papers they cite until the pagerank score converges. We scale the
          score values so that the average pagerank score is 1.0. That means, if
          a paper's pagerank score is larger than 1, then it has more impacts
          than an average paper in Citegraph.
        </p>
        <p>
          The pagerank of an author is simply a summation of all their papers'
          pageranks. If an author's pagerank score is larger than the number of
          their papers, then this author's impact per paper is higher than an
          average author.
        </p>
        <p>
          We visualize pageranks in the visualizer. A larger circle means that
          paper or author has a higher pagerank and vice versa.
        </p>
        <p>
          Note: the author order of a paper is not taken into consideration when
          computing authors' pageranks. Also, we assume one citation represents
          one unweighted positive vote, which may not always be the case.
        </p>
      </div>
    ),
  },
  {
    key: "5",
    label: "Why can't I increase search limit beyond 1000?",
    children: (
      <p>
        Unfortunately, to reduce the pressure on server, we set a hard upper
        bound for the number of relations to load. We may consider increasing
        this number for registered users (if we have a registration system).
      </p>
    ),
  },
  {
    key: "6",
    label: "Why are authors of a paper not in their original order?",
    children: (
      <p>
        Citegraph by default sorts authors by their pagerank (influence). At the
        moment, the original author order is not retained. Please submit an
        issue on GitHub if you find it important for your use case.
      </p>
    ),
  },
];

export const meta = () => {
  return {
    title: `Common Questions | Citegraph`,
  };
};

export default function FAQ() {
  return <Collapse items={items} defaultActiveKey={[1, 2, 3, 4, 5, 6]} />;
}
