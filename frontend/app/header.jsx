import { Link } from "@remix-run/react";
import React from "react";
import logo from "./assets/logo.svg";
import { Search } from "./search";

export default function Header() {
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

      <Search />

      <div id="menu-items">
        <Link to="/playground" className="menu-item">
          Playground
        </Link>
        <Link to="/about" className="menu-item">
          About
        </Link>
        <Link to="/faq" className="menu-item">
          FAQ
        </Link>
        <a
          href="https://github.com/Citegraph/citegraph"
          target="_blank"
          rel="noreferrer"
          className="menu-item"
        >
          GitHub
          <svg
            width="13.5"
            height="13.5"
            aria-hidden="true"
            viewBox="0 0 24 24"
            className="external-link-icon"
          >
            <path
              fill="currentColor"
              d="M21 13v10h-21v-19h12v2h-10v15h17v-8h2zm3-12h-10.988l4.035 4-6.977 7.07 2.828 2.828 6.977-7.07 4.125 4.172v-11z"
            ></path>
          </svg>
        </a>
      </div>
    </div>
  );
}
