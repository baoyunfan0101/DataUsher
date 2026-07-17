package com.datausher.governance.access.api;

public record SubjectQuery(SubjectType type, SubjectStatus status, String text) {
    public SubjectQuery {
        text = text == null ? null : text.trim().toLowerCase();
        if (text != null && text.isEmpty()) {
            text = null;
        }
    }

    public static SubjectQuery all() {
        return new SubjectQuery(null, null, null);
    }
}
