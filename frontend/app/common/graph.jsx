import React, { useEffect, useState, useRef } from "react";
import { MultiDirectedGraph } from "graphology";
import { AuthorInfoPanel, PaperInfoPanel } from "./infoPanel";
import '@react-sigma/core/lib/react-sigma.min.css';

const SigmaGraph = React.memo(function SigmaGraph({
  containerRef,
  graphElements,
  nodeClickHandler,
  edgeClickHandler,
  canvasClickHandler
}) {
  const [sigmaHeight, setSigmaHeight] = useState('600px'); // default height
  const [SigmaContainer, setSigmaContainer] = useState(null);
  const [useLoadGraph, setUseLoadGraph] = useState(null);
  const [useLayout, setUseLayout] = useState(null);
  const [useRegisterEvents, setUseRegisterEvents] = useState(null);
  const [ControlsContainer, setControlsContainer] = useState(null);
  const [ZoomControl, setZoomControl] = useState(null);
  const [FullScreenControl, setFullScreenControl] = useState(null);
  const graphRef = useRef(null);  // Store the graph in a ref
  const sigmaRef = useRef(null);

  useEffect(() => {
    import('@react-sigma/core')
      .then((sigmaModule) => {
        setSigmaContainer(() => sigmaModule.SigmaContainer);
        setUseLoadGraph(() => sigmaModule.useLoadGraph);
        setUseRegisterEvents(() => sigmaModule.useRegisterEvents);
        setControlsContainer(() => sigmaModule.ControlsContainer);
        setZoomControl(() => sigmaModule.ZoomControl);
        setFullScreenControl(() => sigmaModule.FullScreenControl);
      })
      .catch((error) => console.error('Error loading module', error));
    import('@react-sigma/layout-force')
      .then((sigmaModule) => {
        setUseLayout(() => sigmaModule.useLayoutForce);
      })
      .catch((error) => console.error('Error loading module', error));

    function updateSize() {
      if (document.fullscreenElement ||
        document.mozFullScreenElement ||
        document.webkitFullscreenElement ||
        document.msFullscreenElement) {
          setSigmaHeight(`${window.innerHeight}px`);
          return;
      }
      // Get the heights of the other elements
      const headerStyle = window.getComputedStyle(document.getElementById('header'));
      const headerHeight = document.getElementById('header').offsetHeight + parseInt(headerStyle.marginTop) + parseInt(headerStyle.marginBottom);

      const navStyle = window.getComputedStyle(document.getElementById('navigation'));
      const navHeight = document.getElementById('navigation').offsetHeight + parseInt(navStyle.marginTop) + parseInt(navStyle.marginBottom);

      const searchStyle = window.getComputedStyle(document.getElementById('searchLimitConfig'));
      const searchHeight = document.getElementById('searchLimitConfig').offsetHeight + parseInt(searchStyle.marginTop) + parseInt(searchStyle.marginBottom);

      const detailPagePadding = 48; // 3rem
      // Subtract those heights from the viewport height
      const newSigmaHeight = window.innerHeight - headerHeight - navHeight - searchHeight - detailPagePadding;

      // Update the state
      setSigmaHeight(`${newSigmaHeight}px`);
    }

    // Update the height when the component mounts and when the window resizes
    window.addEventListener('resize', updateSize);
    updateSize();

    // Clean up the event listener when the component unmounts
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  if (!SigmaContainer || !useLoadGraph) {
    return <div>Loading...</div>;
  }

  const LoadGraph = ({ graphRef }) => {
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

      graphElements.forEach((element, i, arr) => {
        const data = element.data;
        if (data.id) {
          const x = i == 0 ? 0 : Math.cos(Math.PI * 2 * i / arr.length);
          const y = i == 0 ? 0.5 : Math.sin(Math.PI * 2 * i / arr.length);
          graph.mergeNode(data.id, {
            x: x,
            y: y,
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
      graphRef.current = graph;
      assign();
    }, [assign, loadGraph]);

    return null;
  };

  const GraphEvents = ({ graphRef }) => {
    const registerEvents = useRegisterEvents();
    const [highlightedNodes, setHighlightedNodes] = useState(new Set());

    useEffect(() => {
      registerEvents({
        clickNode: nodeClickHandler,
        clickEdge: edgeClickHandler,
        clickStage: canvasClickHandler,
        enterNode: handleNodeHover,
        leaveNode: handleNodeOut
      });
    }, [registerEvents]);

    const handleNodeHover = e => {
      if (graphRef.current) {
        const neighbors = new Set(graphRef.current.neighbors(e.node));
        neighbors.add(e.node);
        setHighlightedNodes(neighbors);
        updateNodeColors(neighbors);
        updateEdgeVisibility(e.node);
        sigmaRef.current.refresh();
      }
    };

    const handleNodeOut = () => {
      setHighlightedNodes(new Set());
      resetNodeColors();
      resetEdgeVisibility();
      sigmaRef.current.refresh();
    };

    const updateEdgeVisibility = (node) => {
      if (graphRef.current) {
        const allEdges = graphRef.current._edges;
        for (const edge of allEdges.values()) {
          if (edge.source.key != node && edge.target.key != node) {
            edge.attributes.hidden = true;
          }
        }
      }
    }

    const resetEdgeVisibility = () => {
      if (graphRef.current) {
        const allEdges = graphRef.current._edges;
        for (const edge of allEdges.values()) {
          edge.attributes.hidden = false;
        }
      }
    }

    const updateNodeColors = (neighbors) => {
      if (graphRef.current) {
        const allNodes = graphRef.current._nodes;
        for (const node of allNodes.values()) {
          if (neighbors.has(node.key)) {
            node.prevColor = node.attributes.color;
            node.attributes.color = '#ff0000';  // highlighted color
          } else {
            node.prevColor = node.attributes.color;
            node.attributes.color = '#d3d3d3';  // grayed out color
          }
        }
      }
    };

    const resetNodeColors = () => {
      if (graphRef.current) {
        const allNodes = graphRef.current._nodes;
        for (const node of allNodes.values()) {
          node.attributes.color = node.prevColor;
        }
      }
    };

    return null;
  };

  return (
    <SigmaContainer
      ref={sigmaRef}
      graph={MultiDirectedGraph}
      settings={{ renderEdgeLabels: true, defaultEdgeType: "arrow" }}
      style={{ width: "100%", height: sigmaHeight }}>
      <LoadGraph graphRef={graphRef}/>
      <GraphEvents graphRef={graphRef}/>
      <ControlsContainer position={"bottom-right"}>
        <ZoomControl />
        <FullScreenControl container={containerRef}/>
      </ControlsContainer>
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
  const containerRef = useRef(null);
  return (
    <div className="graph-container" ref={containerRef}>
      <SigmaGraph
        containerRef={containerRef}
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
