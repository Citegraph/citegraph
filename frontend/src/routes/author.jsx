import { Link, useLoaderData } from "react-router-dom";
import { getAuthor } from "../apis/authors";
import React, { useEffect } from "react";
import { Table } from "antd";

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
      sorter: (a, b) => a.title.length - b.title.length,
      sortDirections: ["descend"],
      render: (text, record) => <Link to={"/paper/" + record.key}>{text}</Link>,
    },
    {
      title: "Year",
      dataIndex: "year",
      sorter: (a, b) => a.year - b.year,
      sortDirections: ["descend"],
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
      sorter: (a, b) => a.title.length - b.title.length,
      sortDirections: ["descend"],
      render: (text, record) => (
        <Link to={"/author/" + record.key}>{text}</Link>
      ),
    },
    {
      title: "Occurrences",
      dataIndex: "count",
      sorter: (a, b) => a.count - b.count,
      sortDirections: ["descend"],
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

  return (
    <div id="author">
      <div id="name">{author.name.toUpperCase()}</div>
      <div id="desc">
        <p>Number of papers: {author.numOfPapers}</p>
        <p>Number of people who cited this author: {author.numOfReferers}</p>
        <p>Number of people cited by this author: {author.numOfReferees}</p>
      </div>
      <Table
        columns={paperCols}
        dataSource={papers}
        title={() => "Publications (first 100)"}
      />
      <Table
        columns={authorCols}
        dataSource={referers}
        title={() =>
          "People who cited " + author.name.toUpperCase() + " (first 100)"
        }
      />
      <Table
        columns={authorCols}
        dataSource={referees}
        title={() =>
          "People who " + author.name.toUpperCase() + " cited (first 100)"
        }
      />
    </div>
  );
}
