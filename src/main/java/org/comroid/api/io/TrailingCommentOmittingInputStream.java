package org.comroid.api.io;

import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.IOException;
import java.io.InputStream;

@Value
public class TrailingCommentOmittingInputStream extends InputStream {
    InputStream base;
    char        commentStart;
    char        commentEnd;
    @NonFinal boolean omitting;

    public TrailingCommentOmittingInputStream(InputStream base) {
        this(base, '#');
    }

    public TrailingCommentOmittingInputStream(InputStream base, char commentStart) {
        this(base, commentStart, '\n');
    }

    public TrailingCommentOmittingInputStream(InputStream base, char commentStart, char commentEnd) {
        this.base         = base;
        this.commentStart = commentStart;
        this.commentEnd   = commentEnd;
        this.omitting     = false;
    }

    @Override
    public int read() throws IOException {
        var c = base.read();
        if (omitting) {
            if (c == commentEnd) {
                omitting = false;
                c        = '\n';
            }
        } else omitting = c == commentStart;
        return omitting ? read() : c;
    }
}
