package io.citegraph.data.model;

import java.util.List;

public class Paper {
    private String id;
    private String title;
    private int year;
    private List<Author> authors;

    private List<String> references;

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }
}
