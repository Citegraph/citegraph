import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./index.css";
import Root, { loader as rootLoader } from "../app/routes/root";
import ErrorPage from "../app/error-page";
import Author, { loader as authorLoader } from "../app/routes/author/$authorId";
import Paper, { loader as paperLoader } from "../app/routes/paper/$paperId";
import Welcome from "../app/routes";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Root />,
    errorElement: <ErrorPage />,
    loader: rootLoader,
    children: [
      {
        path: "",
        element: <Welcome />,
      },
      {
        path: "author/:authorId",
        element: <Author />,
        loader: authorLoader,
      },
      {
        path: "paper/:paperId",
        element: <Paper />,
        loader: paperLoader,
      },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
