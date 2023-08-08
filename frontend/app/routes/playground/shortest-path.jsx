import { Link, useLoaderData, useFetcher } from "@remix-run/react";
import { DEFAULT_SEARCH_LIMIT } from "../../apis/commons";
import React, {
  useState,
  useEffect,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { getVertex, getPath } from "../../apis/graph";
import { GraphContainerSigma } from "../../common/graph";
import { Breadcrumb, Spin, Result } from "antd";
import { SimpleSearch } from "../../search";

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
  const [startId, setStartId] = useState(null);
  const [endId, setEndId] = useState(null);

  const resetGraph = () => {
    setSelected(null);
  };

  // invoked when new page is loaded
  useEffect(() => {
    setPath(initialData);
    setLoading(false);
    resetGraph();
  }, [initialData]);

  // invoked when search box updated
  useEffect(() => {
    if (fetcher.data) {
      setPath(fetcher.data.path);
      setLoading(false);
      resetGraph();
    }
  }, [fetcher.data, setLoading]);

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
      path && path.vertices
        ? path.vertices.map((elem) => ({
            data: {
              id: elem.id,
              label: getVertexName(elem),
              type: getVertexType(elem),
              pagerank: elem.pagerank,
            },
          }))
        : [],
      path && path.edges
        ? path.edges.map((elem) => ({
            data: {
              source: elem.from,
              target: elem.to,
              label: elem.label,
              type: elem.label,
            },
          }))
        : []
    );
  }, [path]);

  const findPath = (fromId, toId) => {
    if (fetcher.state === "idle") {
      setLoading(true);
      fetcher.load(`/playground/shortest-path?fromId=${fromId}&toId=${toId}`);
    }
  };

  const onSelectStart = (value, option) => {
    setStartId(option.key);
  };

  const onSelectEnd = (value, option) => {
    setEndId(option.key);
  };

  useEffect(() => {
    if (startId && endId) {
      // TODO: alert if startId == endId
      findPath(startId, endId);
    }
  }, [startId, endId]);

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
      <div id="graph-config">
        <SimpleSearch onSelect={onSelectStart} includePrefix={false} />
        <SimpleSearch onSelect={onSelectEnd} includePrefix={false} />
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
      ) : elements == null || elements.length == 0 ? (
        <Result status="warning" title="No path is found within 10 seconds." />
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
