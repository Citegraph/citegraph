package io.citegraph.app.model;

import java.util.List;

public class AuthorResponse {
    private String name;
    private String id;
    private String org;
    private int numOfPapers;
    private int numOfReferees;
    private int numOfReferers;
    private int numOfPaperReferees;
    private int numOfPaperReferers;
    private int numOfCoauthors;
    private double pagerank;

    // author order; only applicable in the context of a particular paper
    private int order;

    // paper written by this author
    private List<PaperResponse> papers;

    // authors that are cited by this author
    private List<CitationResponse> referees;

    // authors that cite this author
    private List<CitationResponse> referers;

    // authors that collaborate with this author
    private List<CollaborationResponse> coauthors;

    public AuthorResponse(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public AuthorResponse(String name, String org, String id) {
        this.name = name;
        this.org = org;
        this.id = id;
    }

    public AuthorResponse(String name, String id, double pagerank) {
        this.name = name;
        this.id = id;
        this.pagerank = pagerank;
    }

    public AuthorResponse(String name, String id, int numOfPaperReferers, double pagerank, int order) {
        this.name = name;
        this.id = id;
        this.numOfPaperReferers = numOfPaperReferers;
        this.pagerank = pagerank;
        this.order = order;
    }

    public AuthorResponse(String name, String id, int numOfPapers, int numOfReferees, int numOfReferers,
                          int numOfPaperReferees, int numOfPaperReferers, int numOfCoauthors, double pagerank) {
        this.name = name;
        this.id = id;
        this.numOfPapers = numOfPapers;
        this.numOfReferees = numOfReferees;
        this.numOfReferers = numOfReferers;
        this.numOfPaperReferees = numOfPaperReferees;
        this.numOfPaperReferers = numOfPaperReferers;
        this.numOfCoauthors = numOfCoauthors;
        this.pagerank = pagerank;
    }

    public List<PaperResponse> getPapers() {
        return papers;
    }

    public void setPapers(List<PaperResponse> papers) {
        this.papers = papers;
    }

    public List<CitationResponse> getReferees() {
        return referees;
    }

    public void setReferees(List<CitationResponse> referees) {
        this.referees = referees;
    }

    public List<CitationResponse> getReferers() {
        return referers;
    }

    public void setReferers(List<CitationResponse> referers) {
        this.referers = referers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumOfPapers() {
        return numOfPapers;
    }

    public void setNumOfPapers(int numOfPapers) {
        this.numOfPapers = numOfPapers;
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

    public int getNumOfPaperReferees() {
        return numOfPaperReferees;
    }

    public void setNumOfPaperReferees(int numOfPaperReferees) {
        this.numOfPaperReferees = numOfPaperReferees;
    }

    public int getNumOfPaperReferers() {
        return numOfPaperReferers;
    }

    public void setNumOfPaperReferers(int numOfPaperReferers) {
        this.numOfPaperReferers = numOfPaperReferers;
    }

    public int getNumOfCoauthors() {
        return numOfCoauthors;
    }

    public void setNumOfCoauthors(int numOfCoauthors) {
        this.numOfCoauthors = numOfCoauthors;
    }

    public double getPagerank() {
        return pagerank;
    }

    public void setPagerank(double pagerank) {
        this.pagerank = pagerank;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<CollaborationResponse> getCoauthors() {
        return coauthors;
    }

    public void setCoauthors(List<CollaborationResponse> coauthors) {
        this.coauthors = coauthors;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }
}
