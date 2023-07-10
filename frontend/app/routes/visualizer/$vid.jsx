import { Link, useLoaderData } from "@remix-run/react";
import { DEFAULT_SEARCH_LIMIT } from "../../apis/commons";
import React, { useState, useEffect } from "react";
import { getVertex } from "../../apis/graph";
import { resetLayout } from "../../common/layout";
import { GraphContainer } from "../../common/graph";
import { Breadcrumb } from "antd";

export async function loader({ params }) {
  const vertex = await getVertex(params.vid, DEFAULT_SEARCH_LIMIT);
  return { vertex };
}

function getVertexName(vertex) {
  return vertex.title || vertex.name;
}

function getVertexLink(vertex) {
  return "/" + vertex.type + "/" + vertex.id;
}

export const meta = ({ data }) => {
  const vertex = data.vertex;
  const title = getVertexName(vertex.self);
  const numOfNeighbors = vertex.neighbors.length;
  return {
    title: `Graph Visualization of ${title} - Citegraph`,
    description: `Graph Visualization of ${title}, including relationships with other ${numOfNeighbors} vertices (authors, papers)`,
  };
};

export default function Graph() {
  const vertex = useLoaderData().vertex;

  const [cyRef, setCyRef] = useState(null);
  const [selected, setSelected] = useState(null);

  const resetGraph = () => {
    resetLayout(setCyRef);
  };

  useEffect(() => {
    if (cyRef) {
      const nodeHandler = async (event) => {
        const target = event.target;
        if (selected && selected.id === target.data().id) {
          setSelected(null);
        } else {
          try {
            const data = await getVertex(
              target.data().id,
              DEFAULT_SEARCH_LIMIT,
              false
            );
            setSelected(data.self);
          } catch (error) {
            console.error("Failed to fetch vertex data", error);
          }
        }
      };
      const canvasHandler = (event) => {
        if (event.target === cyRef) {
          // If the canvas is clicked, "unselect" any selected node
          setSelected(null);
        }
      };
      const edgeHandler = () => {
        // if an edge is clicked, unselect any selected node
        setSelected(null);
      };
      cyRef.on("tap", "node", nodeHandler);
      cyRef.on("tap", "edge", edgeHandler);
      cyRef.on("tap", canvasHandler);
      return () => {
        cyRef.off("tap", "node", nodeHandler);
        cyRef.off("tap", "edge", edgeHandler);
        cyRef.off("tap", canvasHandler);
      };
    }
  }, [cyRef, selected]);

  const elements = [
    {
      data: {
        id: vertex.self.id,
        label: getVertexName(vertex.self),
        type: vertex.self.type,
      },
    },
  ].concat(
    vertex.neighbors.map((elem) => ({
      data: {
        id: elem.vertex.id,
        label: getVertexName(elem.vertex),
        type: elem.vertex.type,
      },
    })),
    vertex.neighbors.map((elem) => ({
      data: {
        source: elem.edge.OUT.id,
        target: elem.edge.IN.id,
        label: elem.edge.label,
        type: elem.edge.label,
      },
    }))
  );

  return (
    <div id="vertex">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: "Home",
            },
            {
              title: "Visualization",
            },
            {
              title: (
                <Link to={getVertexLink(vertex.self)}>
                  {getVertexName(vertex.self).toUpperCase()}
                </Link>
              ),
            },
          ]}
        />
      </div>
      <GraphContainer
        setCyRef={setCyRef}
        graphElements={elements}
        selectedNode={selected}
        height="800px"
      />
    </div>
  );
}
