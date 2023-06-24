import { ConfigProvider } from "antd";
import { extractStyle } from "@ant-design/static-style-extract";

export const loader = async () => {
  const css = extractStyle((node) => <ConfigProvider>{node}</ConfigProvider>);

  return new Response(css, {
    status: 200,
    headers: {
      "Content-Type": "text/css",
    },
  });
};
