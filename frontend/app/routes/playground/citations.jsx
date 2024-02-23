import { Link, useLoaderData, useNavigate, useLocation } from "@remix-run/react";
import { DEFAULT_SEARCH_LIMIT } from "../../apis/commons";
import React, {
  useState,
  useEffect,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { getVertex, getCitationNetwork } from "../../apis/graph";
import { getPaper } from "../../apis/papers";
import { GraphContainerSigma } from "../../common/graph";
import { Breadcrumb, Empty, Space, Spin, Result, notification } from "antd";
import { SimpleSearch } from "../../search";

export async function loader({ request }) {
  const searchParams = new URL(request.url).searchParams;
  const vid = searchParams.get("id");
  const subgraph = await getCitationNetwork(vid);
  return { subgraph, id: vid };
}

function getVertexName(vertex) {
  return vertex.title || vertex.name;
}

function getVertexType(vertex) {
  return vertex.title ? "paper" : "author";
}

export const meta = ({ data }) => {
  return {
    title: `Citation Network Explorer - Citegraph`,
  };
};

export default function CitationNetwork() {
  const loadedData = useLoaderData();
  const initialData = loadedData.subgraph;
  const [subgraph, setSubgraph] = useState(initialData);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [id, setId] = useState(loadedData.id);
  // show no path found warning
  const [showWarning, setShowWarning] = useState(true);
  // we also need to maintain the names of the authors selected
  const location = useLocation();
  // when state is not available, fetch from backend
  const [title, setTitle] = useState(location.state?.title || getPaper(id, 0, false)?.title);

  const navigate = useNavigate();

  const resetGraph = () => {
    setSelected(null);
  };

  // invoked when new page is loaded
  useEffect(() => {
    setSubgraph(initialData);
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
    return [
      ...(subgraph && subgraph.vertices
        ? subgraph.vertices.map((elem) => ({
            data: {
              id: elem.id,
              label: getVertexName(elem),
              type: getVertexType(elem),
              pagerank: elem.pagerank,
            },
          }))
        : []),
      ...(subgraph && subgraph.edges
        ? subgraph.edges.map((elem) => ({
            data: {
              source: elem.from,
              target: elem.to,
              label: elem.label,
              type: elem.label,
            },
          }))
        : []),
    ];
  }, [subgraph]);

  const findPath = (id) => {
    navigate(`/playground/citations?id=${id}`, { state: { title } });
  };

  const onSelectId = (value, option) => {
    setShowWarning(false);
    setId(option.key);
    setTitle(value);
  };

  useEffect(() => {
    const searchParams = new URL(window.location.href).searchParams;
    const currentId = searchParams.get("id");

    if (id && (id !== currentId)) {
      findPath(id);
    }
}, [id]);

  return (
    <div id="path">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
            },
            {
              title: <Link to="/playground/">Playground</Link>,
            },
            {
              title: <Link to="/playground/citations/">Explore Citation Graph</Link>,
            },
          ]}
        />
      </div>
      <div id="graph-config">
        <Space>
          <p>Find citation graph of </p>
          {/* TODO: add search by DOI */}
          <SimpleSearch
            initialSearchType={"paper"}
            onSelect={onSelectId}
            placeholderText={"Enter paper title"}
            includePrefix={false}
            initialValue={title}
          />
        </Space>
      </div>

      {loading ? (
        <div className="loading-spin">
          <Spin size="large" />
        </div>
      ) : elements == null || elements.length == 0 ? (
        id == null || !showWarning ? (
          <div className="landing-no-data">
            <Empty description={<p>no data yet</p>} />
          </div>
        ) : (
          <div className="warning-no-data">
            <Result
              status="warning"
              title="Search timeout (20 seconds)."
              extra={
                <div>
                  <p>Citegraph uses Breadth-First Search to construct a citation graph</p>
                </div>
              }
            />
          </div>
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
