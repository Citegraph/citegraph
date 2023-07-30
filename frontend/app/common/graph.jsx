import React, { useEffect, useState } from "react";
import { Collapse } from "antd";
import { MultiDirectedGraph } from "graphology";
import CytoscapeComponent from "react-cytoscapejs";
import { DEFAULT_LAYOUT } from "./layout";
import { AuthorInfoPanel, PaperInfoPanel } from "./infoPanel";
import '@react-sigma/core/lib/react-sigma.min.css';

export function GraphContainerSigma({
  graphElements,
  selectedNode,
  nodeClickHandler,
  edgeClickHandler,
  canvasClickHandler,
}) {
  const [SigmaContainer, setSigmaContainer] = useState(null);
  const [useLoadGraph, setUseLoadGraph] = useState(null);
  const [useLayout, setUseLayout] = useState(null);
  const [useRegisterEvents, setUseRegisterEvents] = useState(null);

  useEffect(() => {
    import('@react-sigma/core')
      .then((sigmaModule) => {
        setSigmaContainer(() => sigmaModule.SigmaContainer);
        setUseLoadGraph(() => sigmaModule.useLoadGraph);
        setUseRegisterEvents(() => sigmaModule.useRegisterEvents);
      })
      .catch((error) => console.error('Error loading module', error));
    import('@react-sigma/layout-random')
      .then((sigmaModule) => {
        setUseLayout(() => sigmaModule.useLayoutRandom);
      })
      .catch((error) => console.error('Error loading module', error));
  }, []);

  if (!SigmaContainer || !useLoadGraph) {
    return <div className="graph-container">Loading...</div>;
  }

  const LoadGraph = () => {
    const loadGraph = useLoadGraph();
    const { positions, assign } = useLayout();

    useEffect(() => {
      const graph = new MultiDirectedGraph();
      const scores = graphElements
        .filter(element => element.data.id)
        .map(element => element.data.pagerank);
      const minScore = Math.min(...scores);
      const maxScore = Math.max(...scores);
      const MIN_NODE_SIZE = 5;
      const MAX_NODE_SIZE = 20;

      graphElements.forEach(element => {
        const data = element.data;
        if (data.id) {
          graph.mergeNode(data.id, {
            x: 0,
            y: 0,
            size: ((data.pagerank - minScore) / (maxScore - minScore)) *
            (MAX_NODE_SIZE - MIN_NODE_SIZE) +
            MIN_NODE_SIZE,
            label: data.label,
            color: data.type == "author" ? "red" : "purple",
          })
        } else {
          graph.addDirectedEdge(data.source, data.target, {
            label: data.label
          });
        }
      });
      loadGraph(graph);
      assign();
    }, [assign, loadGraph]);

    return null;
  };

  const GraphEvents = () => {
    const registerEvents = useRegisterEvents();

    useEffect(() => {
      registerEvents({
        clickNode: nodeClickHandler,
        clickEdge: edgeClickHandler,
        clickStage: canvasClickHandler,
      });
    }, [registerEvents]);

    return null;
  };

  return (
    <div className="graph-container">
      <SigmaContainer
        graph={MultiDirectedGraph}
        settings={{ renderEdgeLabels: true, defaultEdgeType: "arrow" }}
        style={{ width: "100%" }}>
        <LoadGraph />
        <GraphEvents />
      </SigmaContainer>
      {selectedNode &&
        (selectedNode.name ? (
          <AuthorInfoPanel author={selectedNode} detailPage={false} />
        ) : (
          <PaperInfoPanel paper={selectedNode} detailPage={false} />
        ))}
    </div>
  );
}

export function GraphContainerCytoScape({
  setCyRef,
  graphElements,
  selectedNode,
  height,
}) {
  const stylesheet = [
    {
      selector: "node",
      style: {
        label: "data(label)",
      },
    },
    {
      selector: "edge",
      style: {
        label: "data(label)",
        "curve-style": "bezier",
        "target-arrow-shape": "triangle",
        "target-arrow-color": "black",
        "arrow-scale": 2,
      },
    },
    {
      selector: 'node[type = "author"]',
      style: {
        "background-color": "red",
      },
    },
    {
      selector: 'node[type = "paper"]',
      style: {
        "background-color": "purple",
      },
    },
  ];

  const h = height || "600px";
  return (
    <div className="graph-container">
      <CytoscapeComponent
        cy={setCyRef}
        elements={graphElements}
        stylesheet={stylesheet}
        layout={DEFAULT_LAYOUT}
        minZoom={0.1}
        maxZoom={2}
        className="cytoscape-component"
        style={{ height: h }}
      />
      {selectedNode &&
        (selectedNode.name ? (
          <AuthorInfoPanel author={selectedNode} />
        ) : (
          <PaperInfoPanel paper={selectedNode} />
        ))}
    </div>
  );
}

export function GraphPanel({
  activeKey,
  setActiveKey,
  setCyRef,
  graphElements,
  selectedNode,
}) {
  return (
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
            <GraphContainerCytoScape
              setCyRef={setCyRef}
              graphElements={graphElements}
              selectedNode={selectedNode}
            />
          ),
        },
      ]}
    />
  );
}
