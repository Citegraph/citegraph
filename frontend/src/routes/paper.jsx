import { Link, useLoaderData } from "react-router-dom";
import { getPaper } from "../apis/papers";
import React, { useEffect } from "react";
import { Table } from "antd";

export async function loader({ params }) {
  const paper = await getPaper(params.paperId);
  console.log("paper is", paper);
  return { paper };
}

export default function Paper() {
  const { paper } = useLoaderData();

  useEffect(() => {
    document.title = `${paper.title} - Citegraph`;
  }, [paper.title]);

  const columns = [
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
  const referers = [];
  paper.referers.forEach((p) => {
    referers.push({
      key: p.id,
      title: p.title,
      year: p.year,
    });
  });
  const referees = [];
  paper.referees.forEach((p) => {
    referees.push({
      key: p.id,
      title: p.title,
      year: p.year,
    });
  });

  return (
    <div id="paper">
      <div id="title">
        {paper.title} ({paper.year})
      </div>
      <div id="desc">
        <p>Number of papers which cited this paper: {paper.numOfReferers}</p>
        <p>Number of references: {paper.numOfReferees}</p>
      </div>
      <div>
        Authors:
        {paper.authors.map((author, index) => (
          <span key={index} style={{ paddingLeft: "10px" }}>
            <Link to={`/author/${author.id}`}>{author.name}</Link>
          </span>
        ))}
      </div>
      {referers && referers.length > 0 && (
        <Table
          columns={columns}
          dataSource={referers}
          title={() => "Cited by"}
        />
      )}
      {referees && referees.length > 0 && (
        <Table
          columns={columns}
          dataSource={referees}
          title={() => "References"}
        />
      )}
    </div>
  );
}
