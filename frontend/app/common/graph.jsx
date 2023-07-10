import { Collapse } from "antd";
import CytoscapeComponent from "react-cytoscapejs";
import { DEFAULT_LAYOUT } from "./layout";
import { AuthorInfoPanel, PaperInfoPanel } from "./infoPanel";

export function GraphContainer({
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
        style={{ width: "calc(100% - 200px)", height: h }}
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
            <GraphContainer
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
