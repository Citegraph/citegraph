import { Link } from "@remix-run/react";
import { Breadcrumb, Card, Col, Row, Space } from "antd";
import shortPathDemo from "../../assets/short_path_demo.png";
import communityDemo from "../../assets/community_demo.png";
import connectedPapersDemo from "../../assets/connected_papers_demo.png";

const { Meta } = Card;

export const meta = () => {
  return {
    title: `Playground - Explore Citation Networks | Citegraph`,
  };
};

export default function Playground() {
  return (
    <div id="playground">
      <div id="navigation">
        <Breadcrumb
          items={[
            {
              title: <Link to="/">Home</Link>,
            },
            {
              title: <Link to="/playground/">Playground</Link>,
            },
          ]}
        />
      </div>
      <Space direction="vertical">
        <p>
          Playground offers several graph exploration tools. More to come soon!
        </p>
        <Row gutter={16}>
          <Col span={8}>
            <Link to="/playground/shortest-path">
              <Card
                hoverable
                cover={
                  <img
                    src={shortPathDemo}
                    alt="Demo of shortest path finder"
                    style={{ border: "1px solid #f0f0f0" }}
                  />
                }
              >
                <Meta
                  title="Shortest Path Finder"
                  description="Explore how people connect"
                />
              </Card>
            </Link>
          </Col>
          <Col span={8}>
            <Link to="/playground/cluster">
              <Card
                hoverable
                cover={
                  <img
                    src={communityDemo}
                    alt="Demo of community detector"
                    style={{ border: "1px solid #f0f0f0" }}
                  />
                }
              >
                <Meta
                  title="Community Detector"
                  description="Explore collaboration communities"
                />
              </Card>
            </Link>
          </Col>
          <Col span={8}>
            <Link to="/playground/citations">
              <Card
                hoverable
                cover={
                  <img
                    src={connectedPapersDemo}
                    alt="Demo of connected papers"
                    style={{ border: "1px solid #f0f0f0" }}
                  />
                }
              >
                <Meta
                  title="Connected Papers"
                  description="Explore connected papers and find bedrocks"
                />
              </Card>
            </Link>
          </Col>
        </Row>
      </Space>
    </div>
  );
}
