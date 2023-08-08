import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getAuthor } from "../../apis/authors";
import React, { useEffect, useState } from "react";
import { DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT } from "../../apis/commons";
import { BulbTwoTone, InfoCircleOutlined } from "@ant-design/icons";
import {
  Breadcrumb,
  Button,
  Descriptions,
  Divider,
  Tabs,
  Tooltip,
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
      title: "Citations",
      dataIndex: "citations",
      sorter: (a, b) => a.citations - b.citations,
      sortDirections: ["descend", "ascend", "descend"],
    },
    {
      title: "PageRank",
      dataIndex: "pagerank",
      sorter: (a, b) => a.pagerank - b.pagerank,
      sortDirections: ["descend", "ascend", "descend"],
    },
    {
      title: "Year",
      dataIndex: "year",
      sorter: (a, b) => a.year - b.year,
      sortDirections: ["descend", "ascend", "descend"],
      defaultSortOrder: "descend",
    },
  ];

  const papers =
    author && author.papers
      ? author.papers.map((p) => ({
          key: p.id,
          title: p.title,
          citations: p.numOfReferers,
          pagerank: p.pagerank.toFixed(2),
          year: p.year,
        }))
      : [];

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
      title: "PageRank",
      dataIndex: "pagerank",
      sorter: (a, b) => a.pagerank - b.pagerank,
      sortDirections: ["descend", "ascend", "descend"],
    },
    {
      title: "Occurrences",
      dataIndex: "count",
      sorter: (a, b) => a.count - b.count,
      sortDirections: ["descend", "ascend", "descend"],
      defaultSortOrder: "descend",
    },
  ];

  const coauthors =
    author && author.coauthors
      ? author.coauthors.map((p) => ({
          key: p.author.id,
          name: p.author.name,
          pagerank: p.author.pagerank.toFixed(2),
          count: p.count,
        }))
      : [];

  const referers =
    author && author.referers
      ? author.referers.map((p) => ({
          key: p.author.id,
          name: p.author.name,
          pagerank: p.author.pagerank.toFixed(2),
          count: p.count,
        }))
      : [];

  const referees =
    author && author.referees
      ? author.referees.map((p) => ({
          key: p.author.id,
          name: p.author.name,
          pagerank: p.author.pagerank.toFixed(2),
          count: p.count,
        }))
      : [];

  const tabs = [
    {
      key: "1",
      label: `Publications (${(papers && papers.length) || 0} rows)`,
      children:
        papers && papers.length > 0 ? (
          <div className="tabular">
            <Table columns={paperCols} dataSource={papers} loading={loading} />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `Collaborators (${(coauthors && coauthors.length) || 0} rows)`,
      children:
        coauthors && coauthors.length > 0 ? (
          <div className="tabular">
            <Table
              columns={authorCols}
              dataSource={coauthors}
              loading={loading}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "3",
      label: `Referers (${(referers && referers.length) || 0} rows)`,
      children:
        referers && referers.length > 0 ? (
          <div className="tabular">
            <Table
              columns={authorCols}
              dataSource={referers}
              loading={loading}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "4",
      label: `Referees (${(referees && referees.length) || 0} rows)`,
      children:
        referees && referees.length > 0 ? (
          <div className="tabular">
            <Table
              columns={authorCols}
              dataSource={referees}
              loading={loading}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
  ];

  const maxSearchLimit = Math.min(
    MAX_SEARCH_LIMIT,
    Math.max(
      author.numOfPapers || 0,
      author.numOfReferees || 0,
      author.numOfCoauthors || 0,
      author.numOfReferers || 0
    )
  );

  const sliderMarks = {
    [DEFAULT_SEARCH_LIMIT]: DEFAULT_SEARCH_LIMIT,
    [maxSearchLimit]: maxSearchLimit,
  };

  return (
    <div id="author">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
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
        <Descriptions
          title="Author Info"
          layout="vertical"
          extra={
            <Link to={"/visualizer/" + author.id}>
              <Button icon={<BulbTwoTone />}>Open Visualization</Button>
            </Link>
          }
        >
          <Descriptions.Item label="Name">
            {author.name.toUpperCase()}
          </Descriptions.Item>
          <Descriptions.Item label="Papers">
            {author.numOfPapers}
          </Descriptions.Item>
          <Descriptions.Item label="Collaborators">
            {author.numOfCoauthors}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                Citations&nbsp;
                <Tooltip title={`Cited by ${author.numOfPaperReferers} papers`}>
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {author.numOfPaperReferers}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                PageRank&nbsp;
                <Tooltip title={`Sum of pageranks of this author's papers`}>
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {author.pagerank.toFixed(2)}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                Referers&nbsp;
                <Tooltip title={`Cited by ${author.numOfReferers} people`}>
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {author.numOfReferers}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                Referees&nbsp;
                <Tooltip
                  title={`This author has cited ${author.numOfReferees} people`}
                >
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {author.numOfReferees}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                References&nbsp;
                <Tooltip
                  title={`This author has cited ${author.numOfPaperReferees} papers`}
                >
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {author.numOfPaperReferees}
          </Descriptions.Item>
        </Descriptions>
      </div>
      <Divider dashed />
      {maxSearchLimit > DEFAULT_SEARCH_LIMIT && (
        <div id="graph-config">
          <Text>Search Limit</Text>
          <Row>
            <Col xs={24} md={8}>
              <Slider
                min={DEFAULT_SEARCH_LIMIT}
                max={maxSearchLimit}
                defaultValue={DEFAULT_SEARCH_LIMIT}
                marks={sliderMarks}
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
            <Col xs={0} md={4}>
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
