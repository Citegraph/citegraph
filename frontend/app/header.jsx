import { Link } from "@remix-run/react";
import logo from "./assets/logo.svg";
import { Select, Input, Space, Menu } from "antd";

const { Option } = Select;
const { Search } = Input;

export default function Header() {
  const items = [
    {
      label: "Navigation One",
      key: "mail",
    },
    {
      label: "Navigation Two",
      key: "app",
      disabled: true,
    },
  ];

  return (
    <div style={{ display: "flex", justifyContent: "space-between" }}>
      {/* Left section for logo */}
      <div style={{ padding: "16px 0" }}>
        <Link to="/">
          <img
            src={logo}
            alt="Your Logo"
            style={{ height: "32px", width: "auto" }}
          />
        </Link>
      </div>

      {/* Center section for search bar */}
      <div style={{ alignSelf: "center" }}>
        <Input.Search
          placeholder="input search text"
          onSearch={(value) => console.log(value)}
          style={{ width: 200 }}
        />
      </div>

      {/* Right section for other menu items */}
      <Menu mode="horizontal" items={items} />
    </div>
  );
}
