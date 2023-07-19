import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getAuthor } from "../../apis/authors";
import { resetLayout } from "../../common/layout";
import { GraphPanel } from "../../common/graph";
import React, { useEffect, useState } from "react";
import {
  DEFAULT_SEARCH_LIMIT,
  MAX_SEARCH_LIMIT,
  getEntity,
} from "../../apis/commons";
import { BulbTwoTone } from "@ant-design/icons";
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
  // control collapse components
  const [activeKey, setActiveKey] = useState(["1"]);

  const [cyRefPub, setCyRefPub] = useState(null);
  const [cyRefReferer, setCyRefReferer] = useState(null);
  const [cyRefReferee, setCyRefReferee] = useState(null);
  const [cyRefCoauthor, setCyRefCoauthor] = useState(null);
  const [selectedPub, setSelectedPub] = useState(null);
  const [selectedReferer, setSelectedReferer] = useState(null);
  const [selectedReferee, setSelectedReferee] = useState(null);
  const [selectedCoauthor, setSelectedCoauthor] = useState(null);

  const resetGraph = () => {
    resetLayout(cyRefPub);
    resetLayout(cyRefReferer);
    resetLayout(cyRefReferee);
    resetLayout(cyRefCoauthor);
    setSelectedPub(null);
    setSelectedReferer(null);
    setSelectedReferee(null);
    setSelectedCoauthor(null);
  };

  const onLimitChange = (newValue) => {
    if (fetcher.state === "idle") {
      setActiveKey([]);
      setLimitValue(newValue);
      setLoading(true);
      fetcher.load(`/author/${author.id}?limit=${newValue}`);
    }
  };

  useEffect(() => {
    if (cyRefPub) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selectedPub && selectedPub.id === target.data().id) {
          setSelectedPub(null);
        } else {
          try {
            const data = await getEntity(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              target.data().type == "author",
              false
            );
            setSelectedPub(data);
          } catch (error) {
            console.error("Failed to fetch data on publication tab", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRefPub) {
          // If the canvas is clicked, "unselect" any selected node
          setSelectedPub(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelectedPub(null);
      };
      cyRefPub.on("tap", "node", nodeHandler);
      cyRefPub.on("tap", "edge", edgeHandler);
      cyRefPub.on("tap", canvasHandler);
      return () => {
        cyRefPub.off("tap", "node", nodeHandler);
        cyRefPub.off("tap", "edge", edgeHandler);
        cyRefPub.off("tap", canvasHandler);
      };
    }
  }, [cyRefPub, selectedPub]);

  useEffect(() => {
    if (cyRefCoauthor) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selectedCoauthor && selectedCoauthor.id === target.data().id) {
          setSelectedCoauthor(null);
        } else {
          try {
            const data = await getEntity(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              target.data().type == "author",
              false
            );
            setSelectedCoauthor(data);
          } catch (error) {
            console.error("Failed to fetch data on coauthor tab", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRefCoauthor) {
          // If the canvas was clicked, "unselect" any selected node
          setSelectedCoauthor(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelectedCoauthor(null);
      };
      cyRefCoauthor.on("tap", "node", nodeHandler);
      cyRefCoauthor.on("tap", "edge", edgeHandler);
      cyRefCoauthor.on("tap", canvasHandler);
      return () => {
        cyRefCoauthor.off("tap", "node", nodeHandler);
        cyRefCoauthor.off("tap", "edge", edgeHandler);
        cyRefCoauthor.off("tap", canvasHandler);
      };
    }
  }, [cyRefCoauthor, selectedCoauthor]);

  useEffect(() => {
    if (cyRefReferer) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selectedReferer && selectedReferer.id === target.data().id) {
          setSelectedReferer(null);
        } else {
          try {
            const data = await getEntity(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              target.data().type == "author",
              false
            );
            setSelectedReferer(data);
          } catch (error) {
            console.error("Failed to fetch data on referer tab", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRefReferer) {
          // If the canvas was clicked, "unselect" any selected node
          setSelectedReferer(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelectedReferer(null);
      };
      cyRefReferer.on("tap", "node", nodeHandler);
      cyRefReferer.on("tap", "edge", edgeHandler);
      cyRefReferer.on("tap", canvasHandler);
      return () => {
        cyRefReferer.off("tap", "node", nodeHandler);
        cyRefReferer.off("tap", "edge", edgeHandler);
        cyRefReferer.off("tap", canvasHandler);
      };
    }
  }, [cyRefReferer, selectedReferer]);

  useEffect(() => {
    if (cyRefReferee) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selectedReferee && selectedReferee.id === target.data().id) {
          setSelectedReferee(null);
        } else {
          try {
            const data = await getEntity(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              target.data().type == "author",
              false
            );
            setSelectedReferee(data);
          } catch (error) {
            console.error("Failed to fetch data on referee tab", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRefReferee) {
          // If the canvas was clicked, "unselect" any selected node
          setSelectedReferee(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelectedReferee(null);
      };
      cyRefReferee.on("tap", "node", nodeHandler);
      cyRefReferee.on("tap", "edge", edgeHandler);
      cyRefReferee.on("tap", canvasHandler);
      return () => {
        cyRefReferee.off("tap", "node", nodeHandler);
        cyRefReferee.off("tap", "edge", edgeHandler);
        cyRefReferee.off("tap", canvasHandler);
      };
    }
  }, [cyRefReferee, selectedReferee]);

  // invoked when new page is loaded
  useEffect(() => {
    setAuthor(initialData);
    setLimitValue(DEFAULT_SEARCH_LIMIT);
    setLoading(false);
    resetGraph();
  }, [initialData]);

  // invoked when search limit changed
  useEffect(() => {
    if (fetcher.data) {
      setAuthor(fetcher.data.author);
      setLoading(false);
      resetGraph();
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
          count: p.count,
        }))
      : [];

  const referers =
    author && author.referers
      ? author.referers.map((p) => ({
          key: p.author.id,
          name: p.author.name,
          count: p.count,
        }))
      : [];

  const referees =
    author && author.referees
      ? author.referees.map((p) => ({
          key: p.author.id,
          name: p.author.name,
          count: p.count,
        }))
      : [];

  const publicationGraph = [
    { data: { id: author.id, label: author.name, type: "author" } },
  ].concat(
    author && author.papers
      ? author.papers.map((p) => ({
          data: {
            id: p.id,
            type: "paper",
            label:
              p.title && p.title.length > 100
                ? p.title.substring(0, 100) + "..."
                : p.title,
          },
        }))
      : [],
    author && author.papers
      ? author.papers.map((p) => ({
          data: {
            source: author.id,
            target: p.id,
            label: "writes",
            type: "writes",
          },
        }))
      : []
  );

  const coauthorGraph = [
    { data: { id: author.id, label: author.name, type: "author" } },
  ].concat(
    author && author.coauthors
      ? author.coauthors.map((p) => ({
          data: {
            id: p.author.id,
            type: "author",
            label: p.author.name,
          },
        }))
      : [],
    author && author.coauthors
      ? author.coauthors.map((p) => ({
          data: {
            source: author.id,
            target: p.author.id,
            label: "collaborates",
            type: "collaborates",
          },
        }))
      : []
  );

  const refererGraph = [
    { data: { id: author.id, label: author.name, type: "author" } },
  ].concat(
    author && author.referers
      ? author.referers.map((p) => ({
          data: {
            id: p.author.id,
            type: "author",
            label: p.author.name,
          },
        }))
      : [],
    author && author.referers
      ? author.referers.map((p) => ({
          data: {
            source: p.author.id,
            target: author.id,
            label: "cites",
            type: "cites",
          },
        }))
      : []
  );

  const refereeGraph = [
    { data: { id: author.id, label: author.name, type: "author" } },
  ].concat(
    author && author.referees
      ? author.referees.map((p) => ({
          data: {
            id: p.author.id,
            type: "author",
            label: p.author.name,
          },
        }))
      : [],
    author && author.referees
      ? author.referees.map((p) => ({
          data: {
            source: author.id,
            target: p.author.id,
            label: "cites",
            type: "cites",
          },
        }))
      : []
  );
  const tabs = [
    {
      key: "1",
      label: `Publications (${(papers && papers.length) || 0} rows)`,
      children:
        papers && papers.length > 0 ? (
          <div>
            <GraphPanel
              activeKey={activeKey}
              setActiveKey={setActiveKey}
              setCyRef={setCyRefPub}
              graphElements={publicationGraph}
              selectedNode={selectedPub}
            />
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
          <div>
            <GraphPanel
              activeKey={activeKey}
              setActiveKey={setActiveKey}
              setCyRef={setCyRefCoauthor}
              graphElements={coauthorGraph}
              selectedNode={selectedCoauthor}
            />
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
          <div>
            <GraphPanel
              activeKey={activeKey}
              setActiveKey={setActiveKey}
              setCyRef={setCyRefReferer}
              graphElements={refererGraph}
              selectedNode={selectedReferer}
            />
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
          <div>
            <GraphPanel
              activeKey={activeKey}
              setActiveKey={setActiveKey}
              setCyRef={setCyRefReferee}
              graphElements={refereeGraph}
              selectedNode={selectedReferee}
            />
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
            <a
              href={"/visualizer/" + author.id}
              target="_blank"
              rel="noreferrer"
            >
              <Button icon={<BulbTwoTone />}>Open Visualization</Button>
            </a>
          }
        >
          <Descriptions.Item label="Name">
            {author.name.toUpperCase()}
          </Descriptions.Item>
          <Descriptions.Item label="Papers">
            {author.numOfPapers}
          </Descriptions.Item>
          <Descriptions.Item label="Collaborators">
            {/* Due to a bug in numOfCoauthors ingestion, we wrongly counted author themself, so we need to subtract one here */}
            {Math.max(author.numOfCoauthors - 1, 0)}
          </Descriptions.Item>
          <Descriptions.Item label="Citations">
            <Tooltip title={`Cited by ${author.numOfPaperReferers} papers`}>
              {author.numOfPaperReferers}
            </Tooltip>
          </Descriptions.Item>
          <Descriptions.Item label="Referers">
            <Tooltip title={`Cited by ${author.numOfReferers} people`}>
              {author.numOfReferers}
            </Tooltip>
          </Descriptions.Item>
          <Descriptions.Item label="Referees">
            <Tooltip
              title={`This author has cited ${author.numOfReferees} people`}
            >
              {author.numOfReferees}
            </Tooltip>
          </Descriptions.Item>
          <Descriptions.Item label="References">
            <Tooltip
              title={`This author has cited ${author.numOfPaperReferees} papers`}
            >
              {author.numOfPaperReferees}
            </Tooltip>
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
