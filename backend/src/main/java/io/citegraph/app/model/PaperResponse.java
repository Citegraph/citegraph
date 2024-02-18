package io.citegraph.app.model;

import java.util.List;

public class PaperResponse {
    private String id;
    private String title;
    private int year;

    private int numOfReferees;

    private int numOfReferers;
    private double pagerank;
    private String venue;
    private String keywords;
    private String field;
    private String docType;
    private String volume;
    private String issue;
    private String issn;
    private String isbn;
    private String doi;
    private String paperAbstract;


    private List<AuthorResponse> authors;

    private List<PaperResponse> referees;

    private List<PaperResponse> referers;

    public PaperResponse(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public PaperResponse(String id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public PaperResponse(String id, String title, int year, int numOfReferees, int numOfReferers, double pagerank) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.numOfReferees = numOfReferees;
        this.numOfReferers = numOfReferers;
        this.pagerank = pagerank;
    }

    public int getNumOfReferees() {
        return numOfReferees;
    }

    public void setNumOfReferees(int numOfReferees) {
        this.numOfReferees = numOfReferees;
    }

    public int getNumOfReferers() {
        return numOfReferers;
    }

    public void setNumOfReferers(int numOfReferers) {
        this.numOfReferers = numOfReferers;
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

    public double getPagerank() {
        return pagerank;
    }

    public void setPagerank(double pagerank) {
        this.pagerank = pagerank;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getPaperAbstract() {
        return paperAbstract;
    }

    public void setPaperAbstract(String paperAbstract) {
        this.paperAbstract = paperAbstract;
    }
}
