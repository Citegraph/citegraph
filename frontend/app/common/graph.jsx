import React, { useEffect, useState } from "react";
import { Collapse } from "antd";
import { MultiDirectedGraph } from "graphology";
import { DEFAULT_LAYOUT } from "./layout";
import { AuthorInfoPanel, PaperInfoPanel } from "./infoPanel";
import '@react-sigma/core/lib/react-sigma.min.css';

const SigmaGraph = React.memo(function SigmaGraph({
  graphElements,
  nodeClickHandler,
  edgeClickHandler,
  canvasClickHandler
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
    return <div>Loading...</div>;
  }

  const LoadGraph = () => {
    const loadGraph = useLoadGraph();
    const { assign } = useLayout();

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
    <SigmaContainer
      graph={MultiDirectedGraph}
      settings={{ renderEdgeLabels: true, defaultEdgeType: "arrow" }}
      style={{ width: "100%" }}>
      <LoadGraph />
      <GraphEvents />
    </SigmaContainer>
  );
});

export function GraphContainerSigma({
  graphElements,
  selectedNode,
  nodeClickHandler,
  edgeClickHandler,
  canvasClickHandler,
}) {
  return (
    <div className="graph-container">
      <SigmaGraph
        graphElements={graphElements}
        nodeClickHandler={nodeClickHandler}
        edgeClickHandler={edgeClickHandler}
        canvasClickHandler={canvasClickHandler}/>
      {selectedNode &&
        (selectedNode.name ? (
          <AuthorInfoPanel author={selectedNode} detailPage={false} />
        ) : (
          <PaperInfoPanel paper={selectedNode} detailPage={false} />
        ))}
    </div>
  );
}
