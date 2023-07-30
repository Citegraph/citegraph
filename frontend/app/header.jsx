import { Link } from "@remix-run/react";
import logo from "./assets/logo.svg";
import { Select, Input, Space } from "antd";

const { Option } = Select;
const { Search } = Input;

export default function Header() {
  return (
    <div id="header">
      <div id="logo">
        <Link to="/">
          <img
            src={logo}
            alt="Your Logo"
            style={{ height: "32px", width: "auto" }}
          />
        </Link>
      </div>

      <div id="search">
        <Input.Search
          placeholder="input search text"
          onSearch={(value) => console.log(value)}
          style={{ width: 200 }}
        />
      </div>

      <div id="menu-items"></div>
    </div>
  );
}
