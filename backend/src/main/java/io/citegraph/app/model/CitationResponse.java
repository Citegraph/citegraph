package io.citegraph.app.model;

import io.citegraph.data.model.Author;

public class CitationResponse {
    // depends on the context, this might be referer or referee
    private AuthorResponse author;
    private int count;

    public CitationResponse(AuthorResponse author, int count) {
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
