import { Link, useNavigate } from "@remix-run/react";
import React, { useState } from "react";
import { debounce } from "lodash";
import logo from "./assets/logo.svg";
import { Select, Input, Space, AutoComplete } from "antd";
import { API_URL } from "./apis/commons";

const { Option } = Select;

export default function Header() {
  const [searchResults, setSearchResults] = useState([]);

  const [searchType, setSearchType] = useState("author");
  const [placeholder, setPlaceholder] = useState("Search author name");

  const handleSearchTypeChange = (value) => {
    setSearchType(value);
    if (value === "author") {
      setPlaceholder("Search author name");
    } else {
      setPlaceholder("Search paper title");
    }
  };

  const navigate = useNavigate();
  const onSelect = (value, option) => {
    navigate(option.key);
  };

  const handleSearch = debounce((event) => {
    const query = event.target ? event.target.value : event;
    if (!query) {
      setSearchResults([]);
      return;
    }
    fetch(`${API_URL}/search/${searchType}/${query}`)
      .then((response) => response.json())
      .then((data) => {
        setSearchResults(
          data.map((result) => {
            const isAuthor = result.name != null;
            return {
              key: (isAuthor ? "/author/" : "/paper/") + result.id,
              value: isAuthor ? result.name : result.title,
              label: isAuthor ? result.name : result.title,
            };
          })
        );
      })
      .catch((error) => {
        console.error("Error fetching search results: ", error);
      });
  }, 500);

  return (
    <div id="header">
      <div id="logo">
        <Link to="/">
          <img
            src={logo}
            alt="Citegraph Logo"
            style={{ height: "32px", width: "auto" }}
          />
        </Link>
      </div>

      <div id="search">
        <Space.Compact>
          <Select
            defaultValue="author"
            style={{ width: 80 }}
            onChange={handleSearchTypeChange}
          >
            <Option value="author">Name</Option>
            <Option value="paper">Title</Option>
          </Select>
          <AutoComplete
            options={searchResults}
            style={{ width: 400 }}
            onSelect={onSelect}
            onSearch={handleSearch}
          >
            <Input.Search
              size="medium"
              placeholder={placeholder}
              enterButton
              allowClear
            />
          </AutoComplete>
        </Space.Compact>
      </div>

      <div id="menu-items"></div>
    </div>
  );
}
