import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getPaper } from "../../apis/papers";
import React, { useEffect, useState } from "react";
import {
  DEFAULT_SEARCH_LIMIT,
  MAX_SEARCH_LIMIT,
  DEFAULT_PAGE_SIZE,
} from "../../apis/commons";
import { BulbTwoTone, InfoCircleOutlined } from "@ant-design/icons";
import {
  Breadcrumb,
  Button,
  Descriptions,
  Divider,
  Tabs,
  Typography,
  Tooltip,
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
  if (!paper || paper.error) {
    throw new Response(paper.error, { status: paper.status });
  }
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

  const paperColumns = [
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
      sorter: (a, b) => a.citations - b.citations,
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

  const authorColumns = [
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
      title: "Order",
      dataIndex: "order",
      sorter: (a, b) => a.order - b.order,
      sortDirections: ["ascend", "descend", "ascend"],
      defaultSortOrder: "ascend",
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
  ];

  const authors =
    paper && paper.authors
      ? paper.authors.map((p) => ({
          key: p.id,
          name: p.name,
          order: p.order,
          citations: p.numOfPaperReferers,
          pagerank: p.pagerank.toFixed(2),
        }))
      : [];

  const referers =
    paper && paper.referers
      ? paper.referers.map((p) => ({
          key: p.id,
          title: p.title,
          citations: p.numOfReferers,
          pagerank: p.pagerank.toFixed(2),
          year: p.year,
        }))
      : [];

  const referees =
    paper && paper.referees
      ? paper.referees.map((p) => ({
          key: p.id,
          title: p.title,
          citations: p.numOfReferers,
          pagerank: p.pagerank.toFixed(2),
          year: p.year,
        }))
      : [];

  const tabs = [
    {
      key: "1",
      label: `Authors (${(authors && authors.length) || 0} rows)`,
      children:
        authors && authors.length > 0 ? (
          <div className="tabular">
            <Table
              columns={authorColumns}
              dataSource={authors}
              loading={loading}
              pagination={{ pageSize: DEFAULT_PAGE_SIZE }}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `Cited by (${(referers && referers.length) || 0} rows)`,
      children:
        referers && referers.length > 0 ? (
          <div className="tabular">
            <Table
              columns={paperColumns}
              dataSource={referers}
              loading={loading}
              pagination={{ pageSize: DEFAULT_PAGE_SIZE }}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "3",
      label: `References (${(referees && referees.length) || 0} rows)`,
      children:
        referees && referees.length > 0 ? (
          <div className="tabular">
            <Table
              columns={paperColumns}
              dataSource={referees}
              loading={loading}
              pagination={{ pageSize: DEFAULT_PAGE_SIZE }}
            />
          </div>
        ) : (
          "N/A"
        ),
    },
  ];

  const maxSearchLimit = Math.min(
    MAX_SEARCH_LIMIT,
    Math.max(paper.numOfReferers || 0, paper.numOfReferees || 0)
  );

  const sliderMarks = {
    [DEFAULT_SEARCH_LIMIT]: DEFAULT_SEARCH_LIMIT,
    [maxSearchLimit]: maxSearchLimit,
  };

  return (
    <div id="paper">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
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
        <Descriptions
          title="Paper Info"
          layout="vertical"
          extra={
            <Link to={"/visualizer/" + paper.id}>
              <Button icon={<BulbTwoTone />}>Open Visualization</Button>
            </Link>
          }
        >
          <Descriptions.Item label="Title">{paper.title}</Descriptions.Item>
        </Descriptions>
        {paper.paperAbstract && (
          <Descriptions layout="vertical">
            <Descriptions.Item label="Abstract">
              {paper.paperAbstract}
            </Descriptions.Item>
          </Descriptions>
        )}
        <Descriptions layout="vertical">
          <Descriptions.Item label="Year">{paper.year}</Descriptions.Item>

          {paper.doi && (
            <Descriptions.Item label="DOI">{paper.doi}</Descriptions.Item>
          )}
          {paper.venue && (
            <Descriptions.Item label="Venue">{paper.venue}</Descriptions.Item>
          )}
          {paper.keywords && (
            <Descriptions.Item label="Keywords">
              {paper.keywords}
            </Descriptions.Item>
          )}
          {paper.field && (
            <Descriptions.Item label="Field">{paper.field}</Descriptions.Item>
          )}
          {paper.docType && (
            <Descriptions.Item label="DocType">
              {paper.docType}
            </Descriptions.Item>
          )}
          {paper.volume && (
            <Descriptions.Item label="Volume">{paper.volume}</Descriptions.Item>
          )}
          {paper.issue && (
            <Descriptions.Item label="Issue">{paper.issue}</Descriptions.Item>
          )}
          {paper.issn && (
            <Descriptions.Item label="ISSN">{paper.issn}</Descriptions.Item>
          )}
          {paper.isbn && (
            <Descriptions.Item label="ISBN">{paper.isbn}</Descriptions.Item>
          )}
          <Descriptions.Item
            label={
              <span>
                Citations&nbsp;
                <Tooltip title={`Cited by ${paper.numOfReferers} papers`}>
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {paper.numOfReferers}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                PageRank&nbsp;
                <Tooltip
                  title={`Calculated based on citation edges using PageRank algorithm (average value: 1.0)`}
                >
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {paper.pagerank.toFixed(2)}
          </Descriptions.Item>
          <Descriptions.Item
            label={
              <span>
                References&nbsp;
                <Tooltip
                  title={`This paper has cited ${paper.numOfReferees} papers`}
                >
                  <InfoCircleOutlined />
                </Tooltip>
              </span>
            }
          >
            {paper.numOfReferees}
          </Descriptions.Item>
          <Descriptions.Item label="Authors">
            {paper.authors.length}
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
