package io.citegraph.app.model;

public class VertexDTO {
    private String id;
    private String name;
    private String title;
    private double pagerank;
    // start vertex hsa hops = 0, 1-hop vertex has hops = 1, etc.
    private int hops;

    public VertexDTO(String id, String name, String title, double pagerank) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.pagerank = pagerank;
    }

    public VertexDTO(String id, String name, String title, int hops) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.hops = hops;
    }

    public double getPagerank() {
        return pagerank;
    }

    public void setPagerank(double pagerank) {
        this.pagerank = pagerank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getHops() {
        return hops;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }
}
