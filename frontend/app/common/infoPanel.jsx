import { Link } from "@remix-run/react";
import { Descriptions } from "antd";

export function PaperInfoPanel({ paper }) {
  return (
    <div className="node-info-panel">
      <Descriptions title="Paper Info" layout="vertical">
        <Descriptions.Item label="Title" span={3}>
          <Link to={"/paper/" + paper.id}>{paper.title}</Link>
        </Descriptions.Item>
        <Descriptions.Item label="PageRank" span={3}>
          {paper.pagerank.toFixed(2)}
        </Descriptions.Item>
        <Descriptions.Item label="Year" span={3}>
          {paper.year}
        </Descriptions.Item>
        <Descriptions.Item label="Citations" span={3}>
          {paper.numOfReferers || paper.numOfPaperReferers || 0}
        </Descriptions.Item>
        <Descriptions.Item label="References">
          {paper.numOfReferees || paper.numOfPaperReferees || 0}
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
}

export function AuthorInfoPanel({ author }) {
  return (
    <div className="node-info-panel">
      <Descriptions title="Author Info" layout="vertical">
        <Descriptions.Item label="Name" span={3}>
          <Link to={"/author/" + author.id}>{author.name}</Link>
        </Descriptions.Item>
        <Descriptions.Item label="PageRank" span={3}>
          {author.pagerank.toFixed(2)}
        </Descriptions.Item>
        <Descriptions.Item label="Collaborators" span={3}>
          {author.numOfCoauthors || author.numOfCoworkers || 0}
        </Descriptions.Item>
        <Descriptions.Item label="Citations" span={3}>
          {author.numOfPaperReferers || 0}
        </Descriptions.Item>
        <Descriptions.Item label="Referers" span={3}>
          {author.numOfReferers || author.numOfAuthorReferers || 0}
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
}
