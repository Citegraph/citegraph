import { Link } from "@remix-run/react";
import { Descriptions } from "antd";

export function PaperInfoPanel({ paper }) {
  return (
    <div className="node-info-panel">
      <Descriptions title="Paper Info" layout="vertical">
        <Descriptions.Item label="Title" span={3}>
          <Link to={"/paper/" + paper.id}>{paper.title}</Link>
        </Descriptions.Item>
        <Descriptions.Item label="Year" span={3}>
          {paper.year}
        </Descriptions.Item>
        <Descriptions.Item label="Citations" span={3}>
          {paper.numOfReferers}
        </Descriptions.Item>
        <Descriptions.Item label="References">
          {paper.numOfReferees}
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
        <Descriptions.Item label="Collaborators" span={3}>
          {author.numOfCoauthors}
        </Descriptions.Item>
        <Descriptions.Item label="Citations" span={3}>
          {author.numOfPaperReferers}
        </Descriptions.Item>
        <Descriptions.Item label="Referers" span={3}>
          {author.numOfReferers}
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
}
