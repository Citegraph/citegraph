import { Link, useLoaderData, useNavigate, useLocation } from "@remix-run/react";
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
import { Breadcrumb, Empty, Space, Spin, Result } from "antd";
import { SimpleSearch } from "../../search";

export async function loader({ request }) {
  const searchParams = new URL(request.url).searchParams;
  const start = searchParams.get("fromId");
  const end = searchParams.get("toId");
  const paths = await getPath(start, end);
  const path = paths[0];
  return { path, startId: start, endId: end };
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
  const loadedData = useLoaderData();
  const initialData = loadedData.path;
  const [path, setPath] = useState(initialData);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [startId, setStartId] = useState(loadedData.startId);
  const [endId, setEndId] = useState(loadedData.endId);
  // we also need to maintain the names of the authors selected
  const location = useLocation();
  // TODO: move this to loader. when state is not available, fetch from backend
  const [startValue, setStartValue] = useState(location.state?.startValue || "");
  const [endValue, setEndValue] = useState(location.state?.endValue || "");

  const navigate = useNavigate();

  const resetGraph = () => {
    setSelected(null);
  };

  // invoked when new page is loaded
  useEffect(() => {
    setPath(initialData);
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
    navigate(`/playground/shortest-path?fromId=${fromId}&toId=${toId}`, { state: { startValue: startValue, endValue: endValue } });
  };

  const onSelectStart = (value, option) => {
    setStartId(option.key);
    setStartValue(value);
  };

  const onSelectEnd = (value, option) => {
    setEndId(option.key);
    setEndValue(value);
  };

  useEffect(() => {
    const searchParams = new URL(window.location.href).searchParams;
    const currentStart = searchParams.get("fromId");
    const currentEnd = searchParams.get("toId");

    // TODO: alert if startId == endId
    if (startId && endId && (startId !== currentStart || endId !== currentEnd)) {
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
        <Space>
          <p>Find shortest path between </p>
          <SimpleSearch
            onSelect={onSelectStart}
            placeholderText={"Enter start author"}
            includePrefix={false}
            initialValue={startValue}
          />
          <p>and</p>
          <SimpleSearch
            onSelect={onSelectEnd}
            placeholderText={"Enter end author"}
            includePrefix={false}
            initialValue={endValue}
          />
        </Space>
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
        startId == null || endId == null ? (
          <Empty description={<p>no data yet</p>} />
        ) : (
          <Result
            status="warning"
            title="No path is found within 10 seconds."
          />
        )
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
