import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT } from "../../apis/commons";
import React, { useState, useEffect } from "react";
import { getVertex } from "../../apis/graph";
import { resetLayout } from "../../common/layout";
import { GraphContainer } from "../../common/graph";
import { Breadcrumb, Row, Col, Slider, Spin, InputNumber } from "antd";

export async function loader({ params, request }) {
  const limit =
    new URL(request.url).searchParams.get("limit") || DEFAULT_SEARCH_LIMIT;
  const vertex = await getVertex(params.vid, limit);
  return { vertex };
}

function getVertexName(vertex) {
  return vertex.title || vertex.name;
}

function getVertexLink(vertex) {
  return "/" + vertex.type + "/" + vertex.id;
}

export const meta = ({ data }) => {
  const vertex = data.vertex;
  const title = getVertexName(vertex.self);
  const numOfNeighbors = vertex.neighbors.length;
  return {
    title: `Graph Visualization of ${title} - Citegraph`,
    description: `Graph Visualization of ${title}, including relationships with other ${numOfNeighbors} vertices (authors, papers)`,
  };
};

export default function Graph() {
  const fetcher = useFetcher();
  const initialData = useLoaderData().vertex;
  const [vertex, setVertex] = useState(initialData);

  const [limitValue, setLimitValue] = useState(DEFAULT_SEARCH_LIMIT);
  const [loading, setLoading] = useState(false);
  const [cyRef, setCyRef] = useState(null);
  const [selected, setSelected] = useState(null);

  const resetGraph = () => {
    resetLayout(cyRef);
    setSelected(null);
  };

  const onLimitChange = (newValue) => {
    if (fetcher.state === "idle") {
      setLimitValue(newValue);
      setLoading(true);
      fetcher.load(
        `/visualizer/${vertex.self.id}?limit=${newValue}&getEdges=true`
      );
    }
  };

  // invoked when search limit changed
  useEffect(() => {
    if (fetcher.data) {
      setVertex(fetcher.data.vertex);
      setLoading(false);
      resetGraph();
    }
  }, [fetcher.data, setLoading]);

  useEffect(() => {
    if (cyRef) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selected && selected.id === target.data().id) {
          setSelected(null);
        } else {
          try {
            const data = await getVertex(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              false
            );
            setSelected(data.self);
          } catch (error) {
            console.error("Failed to fetch vertex data", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRef) {
          // If the canvas is clicked, "unselect" any selected node
          setSelected(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelected(null);
      };
      cyRef.on("tap", "node", nodeHandler);
      cyRef.on("tap", "edge", edgeHandler);
      cyRef.on("tap", canvasHandler);
      return () => {
        cyRef.off("tap", "node", nodeHandler);
        cyRef.off("tap", "edge", edgeHandler);
        cyRef.off("tap", canvasHandler);
      };
    }
  }, [cyRef, selected]);

  const elements = [
    {
      data: {
        id: vertex.self.id,
        label: getVertexName(vertex.self),
        type: vertex.self.type,
      },
    },
  ].concat(
    vertex.neighbors.map((elem) => ({
      data: {
        id: elem.vertex.id,
        label: getVertexName(elem.vertex),
        type: elem.vertex.type,
      },
    })),
    vertex.neighbors.map((elem) => ({
      data: {
        source: elem.edge.OUT.id,
        target: elem.edge.IN.id,
        label: elem.edge.label,
        type: elem.edge.label,
      },
    }))
  );

  const maxSearchLimit = Math.min(
    MAX_SEARCH_LIMIT,
    Math.max(
      vertex.self.numOfPaperReferees || 0,
      vertex.self.numOfPaperReferers || 0,
      vertex.self.numOfAuthorReferees || 0,
      vertex.self.numOfAuthorReferers || 0,
      vertex.self.numOfCoworkers || 0,
      vertex.self.numOfPapers || 0
    )
  );

  const sliderMarks = {
    [0]: 0,
    [DEFAULT_SEARCH_LIMIT]: DEFAULT_SEARCH_LIMIT,
    [maxSearchLimit]: maxSearchLimit,
  };

  return (
    <div id="vertex">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
            },
            {
              title: "Visualization",
            },
            {
              title: (
                <Link to={getVertexLink(vertex.self)}>
                  {getVertexName(vertex.self).toUpperCase()}
                </Link>
              ),
            },
          ]}
        />
      </div>
      {maxSearchLimit > DEFAULT_SEARCH_LIMIT && (
        <div id="searchLimitConfig">
          <Row>
            <Col span={8}>
              <Slider
                min={0}
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
                min={0}
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

      {loading ? (
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            height: "800px",
          }}
        >
          <Spin size="large" />
        </div>
      ) : (
        <GraphContainer
          setCyRef={setCyRef}
          graphElements={elements}
          selectedNode={selected}
          height={"800px"}
        />
      )}
    </div>
  );
}
