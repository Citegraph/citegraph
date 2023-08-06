import React, { createContext, useContext, useState } from "react";

const ViewedAddressesContext = createContext({
  historyList: [],
  setHistoryList: () => {},
});

export const ViewedAddressesProvider = ({ children }) => {
  const [historyList, setHistoryList] = useState([]);

  return (
    <ViewedAddressesContext.Provider value={{ historyList, setHistoryList }}>
      {children}
    </ViewedAddressesContext.Provider>
  );
};

export const useViewedAddresses = () => {
  const context = useContext(ViewedAddressesContext);
  if (!context) {
    throw new Error(
      "useViewedAddresses must be used within a ViewedAddressesProvider"
    );
  }
  return context;
};
