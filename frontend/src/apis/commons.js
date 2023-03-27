export const API_URL =
  import.meta.env.MODE === "production"
    ? "/apis"
    : "http://localhost:8080/apis";
