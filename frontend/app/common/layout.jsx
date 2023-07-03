export const DEFAULT_LAYOUT = {
  name: "concentric",
  fit: true,
  minNodeSpacing: 100,
};

export const resetLayout = (cyRef) => {
  if (cyRef) {
    cyRef.on("add", () => {
      cyRef.elements().layout(DEFAULT_LAYOUT).run();
    });
  }
};
