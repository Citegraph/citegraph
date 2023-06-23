import { Link, useLoaderData } from "react-router-dom";
import { getAuthor } from "../apis/authors";
import React, { useEffect } from "react";
import { Breadcrumb, Descriptions, Tabs, Table } from "antd";

export async function loader({ params }) {
  const author = await getAuthor(params.authorId);
  return { author };
}

export default function Author() {
  const { author } = useLoaderData();

  useEffect(() => {
    document.title = `${author.name} - Citegraph`;
  }, [author.name]);

  const paperCols = [
    {
      title: "Title",
      dataIndex: "title",
      sorter: (a, b) => a.title.localeCompare(b.title),
      sortDirections: ["ascend", "descend", "ascend"],
      render: (text, record) => <Link to={"/paper/" + record.key}>{text}</Link>,
    },
    {
      title: "Year",
      dataIndex: "year",
      sorter: (a, b) => a.year - b.year,
      sortDirections: ["descend", "ascend", "descend"],
      defaultSortOrder: "descend",
    },
  ];
  const papers = [];
  author.papers.forEach((p) => {
    papers.push({
      key: p.id,
      title: p.title,
      year: p.year,
    });
  });

  const authorCols = [
    {
      title: "Name",
      dataIndex: "name",
      sorter: (a, b) => a.name.localeCompare(b.name),
      sortDirections: ["ascend", "descend", "ascend"],
      render: (text, record) => (
        <Link to={"/author/" + record.key}>{text}</Link>
      ),
    },
    {
      title: "Occurrences",
      dataIndex: "count",
      sorter: (a, b) => a.count - b.count,
      sortDirections: ["descend", "ascend", "descend"],
      defaultSortOrder: "descend",
    },
  ];

  const referers = [];
  author.referers.forEach((p) => {
    referers.push({
      key: p.author.id,
      name: p.author.name,
      count: p.count,
    });
  });

  const referees = [];
  author.referees.forEach((p) => {
    referees.push({
      key: p.author.id,
      name: p.author.name,
      count: p.count,
    });
  });

  const tabs = [
    {
      key: "1",
      label: `Publications (showing 100)`,
      children:
        papers && papers.length > 0 ? (
          <Table columns={paperCols} dataSource={papers} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `Referers (showing 100)`,
      children:
        referers && referers.length > 0 ? (
          <Table columns={authorCols} dataSource={referers} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "3",
      label: `Referees (showing 100)`,
      children:
        referees && referees.length > 0 ? (
          <Table columns={authorCols} dataSource={referees} />
        ) : (
          "N/A"
        ),
    },
  ];

  return (
    <div id="author">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: "Home",
            },
            {
              title: "Author",
            },
            {
              title: `${author.name.toUpperCase()}`,
            },
          ]}
        />
      </div>
      <div id="desc">
        <Descriptions title="Author Info" layout="vertical">
          <Descriptions.Item label="Name">
            {author.name.toUpperCase()}
          </Descriptions.Item>
          <Descriptions.Item label="Papers">
            {author.numOfPapers}
          </Descriptions.Item>
          <Descriptions.Item label="Number of coauthors">
            {author.numOfCoauthors}
          </Descriptions.Item>
          <Descriptions.Item label="Citations">
            {author.numOfPaperReferers}
          </Descriptions.Item>
          <Descriptions.Item label="Referers (who have cited the author)">
            {author.numOfReferers}
          </Descriptions.Item>
          <Descriptions.Item label="Referees (whom the author has cited)">
            {author.numOfReferees}
          </Descriptions.Item>
          <Descriptions.Item label="References (papers the author has cited)">
            {author.numOfPaperReferees}
          </Descriptions.Item>
        </Descriptions>
      </div>
      <Tabs defaultActiveKey="1" items={tabs} />
    </div>
  );
}
