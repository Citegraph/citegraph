package io.citegraph.data.model;

public class Venue {
    // paper venue name
    private String raw;

    public Venue(String raw) {
        this.raw = raw;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }
}
