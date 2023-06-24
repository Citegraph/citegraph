import { ConfigProvider, theme } from "antd";
import { extractStyle } from "@ant-design/static-style-extract";

const themeLight = {
  token: {
    colorPrimary: "#4cb75b",
    colorPrimaryBg: "#dbf1db",
  },
  algorithm: [theme.defaultAlgorithm],
};

export const loader = async ({ request }) => {
  const css = extractStyle((node) => (
    <ConfigProvider theme={themeLight}>{node}</ConfigProvider>
  ));

  return new Response(css, {
    status: 200,
    headers: {
      "Content-Type": "text/css",
    },
  });
};
