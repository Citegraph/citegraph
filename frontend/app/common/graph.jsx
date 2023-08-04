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
      style={{ width: "100%", height: sigmaHeight }}>
      <LoadGraph />
      <GraphEvents />
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
