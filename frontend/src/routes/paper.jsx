import { Link, useLoaderData } from "react-router-dom";
import { getPaper } from "../apis/papers";
import React, { useEffect, useState } from "react";
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
  }, []);

  const columns = [
    {
      title: "Title",
      dataIndex: "title",
      sorter: (a, b) => a.title.length - b.title.length,
      sortDirections: ["descend"],
    },
    {
      title: "Year",
      dataIndex: "year",
      sorter: (a, b) => a.year - b.year,
      sortDirections: ["descend"],
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

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const onSelectChange = (newSelectedRowKeys) => {
    console.log("selectedRowKeys changed: ", newSelectedRowKeys);
    setSelectedRowKeys(newSelectedRowKeys);
  };

  return (
    <div id="paper">
      <div id="title">
        {paper.title} ({paper.year})
      </div>
      {/* <div id="desc">
        <p>Number of papers which cited this paper: {paper.numOfReferers}</p>
        <p>Number of papers cited by this paper: {paper.numOfReferees}</p>
      </div> */}
      <div>
        Authors:
        {paper.authors.map((author, index) => (
          <span key={index} style={{ paddingLeft: "10px" }}>
            <Link to={`/author/${author.id}`}>{author.name}</Link>
          </span>
        ))}
      </div>
      <Table columns={columns} dataSource={referers} title={() => "Cited by"} />
      {/* <div>
        Cited by:
        <ul>
          {paper.referers.map((paper, index) => (
            <li key={index}>
              <Link to={`/paper/${paper.id}`}>
                {paper.title} ({paper.year})
              </Link>
            </li>
          ))}
        </ul>
      </div> */}
      <Table
        columns={columns}
        dataSource={referees}
        title={() => "References"}
      />
      {/* <div>
        References:
        <ul>
          {paper.referees.map((paper, index) => (
            <li key={index}>
              <Link to={`/paper/${paper.id}`}>
                {paper.title} ({paper.year})
              </Link>
            </li>
          ))}
        </ul>
      </div> */}
    </div>
  );
}
