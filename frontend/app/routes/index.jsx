import React from "react";
import { Link } from "react-router-dom";
import { Typography } from "antd";
import { Card, Row, Col, Space } from "antd";

const { Title } = Typography;

export default function Welcome() {
  return (
    <div className="welcome">
      <Space direction="vertical" size="large">
        <Card>
          <Title level={2}>Welcome to Citegraph</Title>
          <p>
            Citegraph is an open-source online visualizer of 5+ million papers,
            4+ million authors, and various relationships. In total, Citegraph
            has 9.4 million vertices and 294 million edges. At the moment,
            Citegraph only has computer science bibliography.
          </p>
        </Card>
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Card>
              <Title level={4}>Paper ---CITES--&gt; Paper</Title>
              <p>Citegraph contains 32+ million paper citation edges.</p>
              <p>
                Fun fact:{" "}
                <Link to={`/paper/53e9986eb7602d97020ab93b`}>
                  Distinctive Image Features from Scale-Invariant Keypoints
                </Link>{" "}
                is the most cited paper.
              </p>
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card>
              <Title level={4}>Author ---WRITES--&gt; Paper</Title>
              <p>Citegraph contains 16+ million authorship edges.</p>
              <p>
                Fun fact:{" "}
                <Link to={`/author/54055927dabfae8faa5c5dfa`}>H. V. POOR</Link>{" "}
                is the most productive researcher - he has authored more than
                1.6k papers!
              </p>
            </Card>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Card>
              <Title level={4}>Author ---REFERS--&gt; Author</Title>
              <p>Citegraph contains 224+ million author citation edges.</p>
              <p>
                Fun fact:{" "}
                <Link to={`/author/53f366a7dabfae4b3499c6fe`}>
                  Geoffrey Hinton
                </Link>{" "}
                is the most-cited person - more than 66k people have cited his
                work at least once!
              </p>
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card>
              <Title level={4}>Author ---COLLABORATES with--&gt; Author</Title>
              <p>Citegraph contains 19+ million author collaboration edges.</p>
              <p></p>
              <p>
                Fun fact:{" "}
                <Link to={`/author/562b15e745cedb3398979350`}>
                  Radu Timofte
                </Link>{" "}
                has the most collaborators - more than 2k people have coauthored
                at least one paper with him!
              </p>
            </Card>
          </Col>
        </Row>
      </Space>
    </div>
  );
}
