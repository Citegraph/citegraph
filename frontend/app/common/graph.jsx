import { Collapse } from "antd";
import CytoscapeComponent from "react-cytoscapejs";
import { DEFAULT_LAYOUT } from "./layout";
import { AuthorInfoPanel, PaperInfoPanel } from "./infoPanel";

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
            <div className="graph-container">
              <CytoscapeComponent
                cy={setCyRef}
                elements={graphElements}
                layout={DEFAULT_LAYOUT}
                minZoom={0.1}
                maxZoom={2}
                style={{ width: "calc(100% - 200px)", height: "600px" }}
              />
              {selectedNode &&
                (selectedNode.name ? (
                  <AuthorInfoPanel author={selectedNode} />
                ) : (
                  <PaperInfoPanel paper={selectedNode} />
                ))}
            </div>
          ),
        },
      ]}
    />
  );
}
