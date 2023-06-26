import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getAuthor } from "../../apis/authors";
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

export async function loader({ params, request }) {
  const limit =
    new URL(request.url).searchParams.get("limit") || DEFAULT_SEARCH_LIMIT;
  const author = await getAuthor(params.authorId, limit);
  return { author };
}

export const meta = ({ data }) => {
  const author = data.author;
  return {
    title: `${author.name} - Citegraph`,
    description: `Details of ${author.name}, including publications, coauthors, citations, potential referers, etc.`,
  };
};

export default function Author() {
  const fetcher = useFetcher();
  const initialData = useLoaderData().author;
  const [author, setAuthor] = useState(initialData);
  const [limitValue, setLimitValue] = useState(DEFAULT_SEARCH_LIMIT);
  const [loading, setLoading] = useState(false);

  const onLimitChange = (newValue) => {
    if (fetcher.state === "idle") {
      setLimitValue(newValue);
      setLoading(true);
      fetcher.load(`/author/${author.id}?limit=${newValue}`);
    }
  };

  // invoked when new page is loaded
  useEffect(() => {
    setAuthor(initialData);
    setLimitValue(DEFAULT_SEARCH_LIMIT);
    setLoading(false);
  }, [initialData]);

  // invoked when search limit changed
  useEffect(() => {
    if (fetcher.data) {
      setAuthor(fetcher.data.author);
      setLoading(false);
    }
  }, [fetcher.data, setLoading]);

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

  const referers = author.referers.map((p) => ({
    key: p.author.id,
    name: p.author.name,
    count: p.count,
  }));

  const referees = author.referees.map((p) => ({
    key: p.author.id,
    name: p.author.name,
    count: p.count,
  }));

  const tabs = [
    {
      key: "1",
      label: `Publications (${(papers && papers.length) || 0} rows)`,
      children:
        papers && papers.length > 0 ? (
          <Table columns={paperCols} dataSource={papers} loading={loading} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `Referers (${(referers && referers.length) || 0} rows)`,
      children:
        referers && referers.length > 0 ? (
          <Table columns={authorCols} dataSource={referers} loading={loading} />
        ) : (
          "N/A"
        ),
    },
    {
      key: "3",
      label: `Referees (${(referees && referees.length) || 0} rows)`,
      children:
        referees && referees.length > 0 ? (
          <Table columns={authorCols} dataSource={referees} loading={loading} />
        ) : (
          "N/A"
        ),
    },
  ];

  const maxSearchLimit = Math.max(
    author.numOfPapers || 0,
    author.numOfReferees || 0,
    author.numOfReferers || 0
  );

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
