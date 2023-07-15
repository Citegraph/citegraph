package io.citegraph.app.model;

public class CollaborationResponse {
    private AuthorResponse author;
    private int count;

    public CollaborationResponse(AuthorResponse author, int count) {
        this.author = author;
        this.count = count;
    }

    public AuthorResponse getAuthor() {
        return author;
    }

    public void setAuthor(AuthorResponse author) {
        this.author = author;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
