package io.citegraph.app.model;

public class CollaborationResponse {
    private AuthorResponse coworker;
    private int count;

    public CollaborationResponse(AuthorResponse coworker, int count) {
        this.coworker = coworker;
        this.count = count;
    }

    public AuthorResponse getCoworker() {
        return coworker;
    }

    public void setCoworker(AuthorResponse coworker) {
        this.coworker = coworker;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
