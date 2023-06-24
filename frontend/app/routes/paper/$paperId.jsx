import { Link, useLoaderData, useParams } from "@remix-run/react";
import { getPaper } from "../../apis/papers";
import React, { useEffect } from "react";
import { Breadcrumb, Descriptions, Tabs, Table } from "antd";

export async function loader({ params }) {
  const paper = await getPaper(params.paperId);
  return { paper };
}

export default function Paper() {
  const params = useParams();
  console.log("paper id = ", params.paperId);

  const { paper } = useLoaderData();

  useEffect(() => {
    document.title = `${paper.title} - Citegraph`;
  }, [paper.title]);

  const columns = [
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

  const tabs = [
    {
      key: "1",
      label: `Cited by (showing 100)`,
      children:
        referers && referers.length > 0 ? (
          <Table columns={columns} dataSource={referers} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `References (showing 100)`,
      children:
        referees && referees.length > 0 ? (
          <Table columns={columns} dataSource={referees} />
        ) : (
          "N/A"
        ),
    },
  ];

  return (
    <div id="paper">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: "Home",
            },
            {
              title: "Paper",
            },
            {
              title: `${paper.title}`,
            },
          ]}
        />
      </div>
      <div id="desc">
        <Descriptions title="Paper Info" layout="vertical">
          <Descriptions.Item label="Title">{paper.title}</Descriptions.Item>
          <Descriptions.Item label="Citations">
            {paper.numOfReferers}
          </Descriptions.Item>
          <Descriptions.Item label="References (which the paper has cited)">
            {paper.numOfReferees}
          </Descriptions.Item>
          <Descriptions.Item label="Authors">
            {paper.authors.map((author, index) => (
              <span
                key={index}
                style={{ paddingLeft: index !== 0 ? "10px" : "0" }}
              >
                <Link to={`/author/${author.id}`}>{author.name}</Link>
              </span>
            ))}
          </Descriptions.Item>
        </Descriptions>
      </div>
      <Tabs defaultActiveKey="1" items={tabs} />
    </div>
  );
}
