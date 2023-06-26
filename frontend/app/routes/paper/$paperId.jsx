import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getPaper } from "../../apis/papers";
import React, { useEffect, useState } from "react";
import { DEFAULT_SEARCH_LIMIT } from "../../apis/commons";
import {
  Breadcrumb,
  Descriptions,
  Divider,
  Tabs,
  Typography,
  Table,
  Col,
  InputNumber,
  Row,
  Slider,
} from "antd";

const { Text } = Typography;

export async function loader({ request, params }) {
  const limit =
    new URL(request.url).searchParams.get("limit") || DEFAULT_SEARCH_LIMIT;
  const paper = await getPaper(params.paperId, limit);
  return { paper };
}

export const meta = ({ data }) => {
  const paper = data.paper;
  return {
    title: `${paper.title} - Citegraph`,
    description: `Details of paper ${paper.title} published on ${paper.year}`,
  };
};

export default function Paper() {
  const fetcher = useFetcher();
  const initialData = useLoaderData().paper;
  const [paper, setPaper] = useState(initialData);
  const [limitValue, setLimitValue] = useState(DEFAULT_SEARCH_LIMIT);
  const [loading, setLoading] = useState(false);

  const onLimitChange = (newValue) => {
    if (fetcher.state === "idle") {
      setLimitValue(newValue);
      setLoading(true);
      fetcher.load(`/paper/${paper.id}?limit=${newValue}`);
    }
  };

  // invoked when new page is loaded
  useEffect(() => {
    setPaper(initialData);
    setLimitValue(DEFAULT_SEARCH_LIMIT);
    setLoading(false);
  }, [initialData]);

  // invoked when search limit changed
  useEffect(() => {
    if (fetcher.data) {
      setPaper(fetcher.data.paper);
      setLoading(false);
    }
  }, [fetcher.data, setLoading]);

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

  const referers = paper.referers.map((p) => ({
    key: p.id,
    title: p.title,
    year: p.year,
  }));

  const referees = paper.referees.map((p) => ({
    key: p.id,
    title: p.title,
    year: p.year,
  }));

  const tabs = [
    {
      key: "1",
      label: `Cited by (${(referers && referers.length) || 0} rows)`,
      children:
        referers && referers.length > 0 ? (
          <Table columns={columns} dataSource={referers} loading={loading} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `References (${(referees && referees.length) || 0} rows)`,
      children:
        referees && referees.length > 0 ? (
          <Table columns={columns} dataSource={referees} loading={loading} />
        ) : (
          "N/A"
        ),
    },
  ];

  const maxSearchLimit = Math.max(
    paper.numOfReferers || 0,
    paper.numOfReferees || 0
  );

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
      <Divider dashed />
      {maxSearchLimit > DEFAULT_SEARCH_LIMIT && (
        <div id="searchLimitConfig">
          <Text>Search Limit</Text>
          <Row>
            <Col span={8}>
              <Slider
                min={DEFAULT_SEARCH_LIMIT}
                max={maxSearchLimit}
                onChange={onLimitChange}
                disabled={fetcher.state !== "idle"}
                step={100}
                value={
                  typeof limitValue === "number"
                    ? limitValue
                    : DEFAULT_SEARCH_LIMIT
                }
              />
            </Col>
            <Col span={4}>
              <InputNumber
                min={DEFAULT_SEARCH_LIMIT}
                max={maxSearchLimit}
                style={{ margin: "0 16px" }}
                value={limitValue}
                disabled={fetcher.state !== "idle"}
                onChange={onLimitChange}
                step={100}
              />
            </Col>
          </Row>
        </div>
      )}
      <Tabs defaultActiveKey="1" items={tabs} />
    </div>
  );
}
