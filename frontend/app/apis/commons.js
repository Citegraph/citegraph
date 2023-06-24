export const API_URL =
  process.env.NODE_ENV === "production"
    ? "/apis"
    : "http://localhost:8080/apis";
