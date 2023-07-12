import React, { useEffect } from "react";
import { Link } from "react-router-dom";
import { Typography } from "antd";
import { Card, Space } from "antd";

const { Title, Text } = Typography;

export default function Welcome() {
  return (
    <div className="welcome">
      <Space direction="vertical" size="large">
        <Card>
          <Title level={2}>Welcome to Citegraph</Title>
          <Text>
            Citegraph is an open-source online visualizer of 5+ million papers,
            4+ million authors, and various relationships. In total, Citegraph
            has 9.4 million vertices and 274 million edges. At the moment,
            Citegraph only has computer science bibliography.
          </Text>
        </Card>
        <Card>
          <Title level={4}>Paper ---cites--&gt; Paper relationships</Title>
          <Text>
            Citegraph contains 32+ million paper citation relationships
          </Text>
        </Card>
        <Card>
          <Title level={4}>Author ---writes--&gt; Paper relationships</Title>
          <Text>Citegraph contains 16+ million authorship relationships</Text>
        </Card>
        <Card>
          <Title level={4}>Author ---refers--&gt; Author relationships</Title>
          <Text>
            Citegraph contains 224+ million author citation relationships. We
            deduce the relationship that A refers B if A has ever authored a
            paper which cites another paper written by B. For example,{" "}
            <Link to={`/author/53f366a7dabfae4b3499c6fe`}>Geoffrey Hinton</Link>{" "}
            has ever cited 1.8k people, and more than 65k people have cited him.
          </Text>
        </Card>
      </Space>
    </div>
  );
}
