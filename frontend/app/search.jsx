import React, { useState, useEffect } from "react";
import { useNavigate } from "@remix-run/react";
import { debounce } from "lodash";
import { Select, Input, Space, AutoComplete } from "antd";
import { API_URL } from "./apis/commons";

const { Option } = Select;

// simple search for a certain vertex type
export function SimpleSearch({
  initialSearchType = "author",
  placeholderText,
  onSelect: externalOnSelect,
  clearResults,
  includePrefix = true,
  initialValue = "",
}) {
  const [searchResults, setSearchResults] = useState([]);
  const [hasSearched, setHasSearched] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (clearResults) {
      setSearchResults([]);
      setHasSearched(false);
    }
  }, [clearResults]);

  const generatedPlaceholder =
    initialSearchType === "author"
      ? "Search author name"
      : "Search paper title";

  const handleSearch = debounce((query) => {
    if (!query) {
      setSearchResults([]);
      setHasSearched(false);
      return;
    }
    setHasSearched(true);
    fetch(`${API_URL}/search/${initialSearchType}/${query}`)
      .then((response) => response.json())
      .then((data) => {
        const results = data.map((result) => {
          const isAuthor = result.name != null;
          return {
            key:
              (includePrefix ? (isAuthor ? "/author/" : "/paper/") : "") +
              result.id,
            value: isAuthor ? result.name : result.title,
          };
        });
        let prev = null;
        let counter = 0;
        for (let result of results) {
          if (result.value === prev) {
            counter++;
            result.value = result.value + " (" + counter + ")";
          } else {
            counter = 0;
            prev = result.value;
          }
          result.label = result.value;
        }
        setSearchResults(results);
      })
      .catch((error) => {
        console.error("Error fetching search results: ", error);
      });
  }, 500);

  const onSelect = (value, option) => {
    if (externalOnSelect) {
      externalOnSelect(value, option);
    } else {
      navigate(option.key);
    }
  };

  return (
    <AutoComplete
      className="search-bar"
      options={searchResults}
      onSelect={onSelect}
      onSearch={handleSearch}
      notFoundContent={
        hasSearched && !searchResults.length ? "Not found" : null
      }
      defaultValue={initialValue}
    >
      <Input.Search
        size="medium"
        placeholder={placeholderText || generatedPlaceholder}
        onSearch={handleSearch}
        enterButton
        allowClear
      />
    </AutoComplete>
  );
}

// full search functionality with vertex type switch
export function Search() {
  const [searchType, setSearchType] = useState("author");
  const [resetToggle, setResetToggle] = useState(false);

  const handleSearchTypeChange = (value) => {
    setSearchType(value);
    setResetToggle((prev) => !prev); // Flip the toggle
  };

  return (
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
        <SimpleSearch
          initialSearchType={searchType}
          clearResults={resetToggle}
        />
      </Space.Compact>
    </div>
  );
}
