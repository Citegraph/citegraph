package io.citegraph.app.model;

import java.util.List;

public class PaperResponse {
    private String id;
    private String title;
    private int year;

    private List<AuthorResponse> authors;

    private List<PaperResponse> referees;

    private List<PaperResponse> referers;

    public PaperResponse(String id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<AuthorResponse> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorResponse> authors) {
        this.authors = authors;
    }

    public List<PaperResponse> getReferees() {
        return referees;
    }

    public void setReferees(List<PaperResponse> referees) {
        this.referees = referees;
    }

    public List<PaperResponse> getReferers() {
        return referers;
    }

    public void setReferers(List<PaperResponse> referers) {
        this.referers = referers;
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
}
