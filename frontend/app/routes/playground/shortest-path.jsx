import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT } from "../../apis/commons";
import React, {
  useState,
  useEffect,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { getVertex, getPath } from "../../apis/graph";
import { GraphContainerSigma } from "../../common/graph";
import {
  Breadcrumb,
  Checkbox,
  Row,
  Col,
  Slider,
  Spin,
  InputNumber,
} from "antd";

export async function loader({ request }) {
  const searchParams = new URL(request.url).searchParams;
  const start = searchParams.get("fromId");
  const end = searchParams.get("toId");
  const paths = await getPath(start, end);
  const path = paths[0];
  return { path };
}

function getVertexName(vertex) {
  return vertex.title || vertex.name;
}

function getVertexType(vertex) {
  return vertex.title ? "paper" : "author";
}

function getVertexLink(vertex, type) {
  return "/" + type + "/" + vertex.id;
}

export const meta = ({ data }) => {
  return {
    title: `Find shortest path - Citegraph`,
  };
};

export default function ShortestPath() {
  const fetcher = useFetcher();
  const initialData = useLoaderData().path;
  const [path, setPath] = useState(initialData);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);

  const resetGraph = () => {
    setSelected(null);
  };

  // invoked when new page is loaded
  useEffect(() => {
    setPath(initialData);
    setLoading(false);
    resetGraph();
  }, [initialData]);

  const selectedRef = useRef();
  selectedRef.current = selected;

  const nodeHandler = useCallback(async (event) => {
    const id = event.node;
    if (selectedRef.current && selectedRef.current.id === id) {
      setSelected(null);
    } else {
      try {
        const data = await getVertex(id, DEFAULT_SEARCH_LIMIT, true);
        setSelected(data.self);
      } catch (error) {
        console.error("Failed to fetch vertex data", error);
      }
    }
  }, []);

  const canvasHandler = useCallback(() => {
    // If the canvas is clicked, "unselect" any selected node
    setSelected(null);
  }, []);

  const edgeHandler = useCallback(() => {
    // if an edge is clicked, unselect any selected node
    setSelected(null);
  }, []);

  const elements = useMemo(() => {
    return [].concat(
      path.vertices.map((elem) => ({
        data: {
          id: elem.id,
          label: getVertexName(elem),
          type: getVertexType(elem),
          pagerank: elem.pagerank,
        },
      })),
      path.edges.map((elem) => ({
        data: {
          source: elem.from,
          target: elem.to,
          label: elem.label,
          type: elem.label,
        },
      }))
    );
  }, [path]);

  return (
    <div id="path">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
            },
            {
              title: "Playground",
            },
            {
              title: "Shortested Path",
            },
          ]}
        />
      </div>

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
        <GraphContainerSigma
          graphElements={elements}
          selectedNode={selected}
          nodeClickHandler={nodeHandler}
          edgeClickHandler={edgeHandler}
          canvasClickHandler={canvasHandler}
        />
      )}
    </div>
  );
}
