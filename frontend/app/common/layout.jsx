export const DEFAULT_LAYOUT = {
  name: "concentric",
  fit: true,
  minNodeSpacing: 100,
};

export const resetLayout = (cyRef) => {
  if (cyRef.current) {
    cyRef.current.on("add", () => {
      cyRef.current.elements().layout(DEFAULT_LAYOUT).run();
    });
  }
};
