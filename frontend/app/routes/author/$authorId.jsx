import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { getAuthor } from "../../apis/authors";
import { getPaper } from "../../apis/papers";
import { DEFAULT_LAYOUT, resetLayout } from "../../common/layout";
import React, { useEffect, useState } from "react";
import { DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT } from "../../apis/commons";
import CytoscapeComponent from "react-cytoscapejs";
import {
  Breadcrumb,
  Collapse,
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
  // control collapse components
  const [activeKey, setActiveKey] = useState(["1"]);

  const [cyRefPub, setCyRefPub] = useState(null);
  const [cyRefReferer, setCyRefReferer] = useState(null);
  const [cyRefReferee, setCyRefReferee] = useState(null);
  const [selectedPub, setSelectedPub] = useState(null);
  const [selectedReferer, setSelectedReferer] = useState(null);
  const [selectedReferee, setSelectedReferee] = useState(null);

  const resetGraph = () => {
    resetLayout(cyRefPub);
    resetLayout(cyRefReferer);
    resetLayout(cyRefReferee);
    setSelectedPub(null);
    setSelectedReferer(null);
    setSelectedReferee(null);
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
            const data = await getPaper(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              false
            );
            setSelectedPub(data);
          } catch (error) {
            console.error("Failed to fetch paper data", error);
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
    if (cyRefReferer) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selectedReferer && selectedReferer.id === target.data().id) {
          setSelectedReferer(null);
        } else {
          try {
            const data = await getAuthor(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              false
            );
            setSelectedReferer(data);
          } catch (error) {
            console.error("Failed to fetch author data", error);
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
            const data = await getAuthor(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              false
            );
            setSelectedReferee(data);
          } catch (error) {
            console.error("Failed to fetch author data", error);
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
      title: "Year",
      dataIndex: "year",
      sorter: (a, b) => a.year - b.year,
      sortDirections: ["descend", "ascend", "descend"],
      defaultSortOrder: "descend",
    },
  ];
  const papers = author.papers.map((p) => ({
    key: p.id,
    title: p.title,
    year: p.year,
  }));

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

  const publicationGraph = [
    { data: { id: author.id, label: author.name } },
  ].concat(
    author.papers.map((p) => ({
      data: {
        id: p.id,
        label:
          p.title && p.title.length > 100
            ? p.title.substring(0, 100) + "..."
            : p.title,
      },
    })),
    author.papers.map((p) => ({
      data: {
        source: author.id,
        target: p.id,
        label: "writes",
      },
    }))
  );

  const refererGraph = [{ data: { id: author.id, label: author.name } }].concat(
    author.referers.map((p) => ({
      data: {
        id: p.author.id,
        label: p.author.name,
      },
    })),
    author.referers.map((p) => ({
      data: {
        source: p.author.id,
        target: author.id,
        label: "cites",
      },
    }))
  );

  const refereeGraph = [{ data: { id: author.id, label: author.name } }].concat(
    author.referees.map((p) => ({
      data: {
        id: p.author.id,
        label: p.author.name,
      },
    })),
    author.referees.map((p) => ({
      data: {
        source: author.id,
        target: p.author.id,
        label: "cites",
      },
    }))
  );
  const tabs = [
    {
      key: "1",
      label: `Publications (${(papers && papers.length) || 0} rows)`,
      children:
        papers && papers.length > 0 ? (
          <div>
            <Collapse
              className="desktop-collapse"
              activeKey={activeKey}
              onChange={setActiveKey}
              style={{ marginBottom: "1rem" }}
              items={[
                {
                  key: "publicationGraph",
                  label: "Show graph visualization",
                  children: (
                    <div className="graph-container">
                      <CytoscapeComponent
                        cy={setCyRefPub}
                        elements={publicationGraph}
                        layout={DEFAULT_LAYOUT}
                        minZoom={0.1}
                        maxZoom={2}
                        style={{ width: "calc(100% - 200px)", height: "600px" }}
                      />
                      {selectedPub && (
                        <div className="node-info-panel">
                          <Descriptions title="Paper Info" layout="vertical">
                            <Descriptions.Item label="Title" span={3}>
                              <Link to={"/paper/" + selectedPub.id}>
                                {selectedPub.title}
                              </Link>
                            </Descriptions.Item>
                            <Descriptions.Item label="Year" span={3}>
                              {selectedPub.year}
                            </Descriptions.Item>
                            <Descriptions.Item label="Citations" span={3}>
                              {selectedPub.numOfReferers}
                            </Descriptions.Item>
                            <Descriptions.Item label="References">
                              {selectedPub.numOfReferees}
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
            <Table columns={paperCols} dataSource={papers} loading={loading} />
          </div>
        ) : (
          "N/A"
        ),
    },
    {
      key: "2",
      label: `Referers (${(referers && referers.length) || 0} rows)`,
      children:
        referers && referers.length > 0 ? (
          <div>
            <Collapse
              className="desktop-collapse"
              activeKey={activeKey}
              onChange={setActiveKey}
              style={{ marginBottom: "1rem" }}
              items={[
                {
                  key: "publicationGraph",
                  label: "Show graph visualization",
                  children: (
                    <div className="graph-container">
                      <CytoscapeComponent
                        cy={setCyRefReferer}
                        elements={refererGraph}
                        layout={DEFAULT_LAYOUT}
                        minZoom={0.1}
                        maxZoom={2}
                        style={{ width: "calc(100% - 200px)", height: "600px" }}
                      />
                      {selectedReferer && (
                        <div className="node-info-panel">
                          <Descriptions title="Author Info" layout="vertical">
                            <Descriptions.Item label="Name" span={3}>
                              <Link to={"/author/" + selectedReferer.id}>
                                {selectedReferer.name}
                              </Link>
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
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
      key: "3",
      label: `Referees (${(referees && referees.length) || 0} rows)`,
      children:
        referees && referees.length > 0 ? (
          <div>
            <Collapse
              className="desktop-collapse"
              activeKey={activeKey}
              onChange={setActiveKey}
              style={{ marginBottom: "1rem" }}
              items={[
                {
                  key: "publicationGraph",
                  label: "Show graph visualization",
                  children: (
                    <div className="graph-container">
                      <CytoscapeComponent
                        cy={setCyRefReferee}
                        elements={refereeGraph}
                        layout={DEFAULT_LAYOUT}
                        minZoom={0.1}
                        maxZoom={2}
                        style={{ width: "calc(100% - 200px)", height: "600px" }}
                      />
                      {selectedReferee && (
                        <div className="node-info-panel">
                          <Descriptions title="Author Info" layout="vertical">
                            <Descriptions.Item label="Name" span={3}>
                              <Link to={"/author/" + selectedReferee.id}>
                                {selectedReferee.name}
                              </Link>
                            </Descriptions.Item>
                          </Descriptions>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
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
