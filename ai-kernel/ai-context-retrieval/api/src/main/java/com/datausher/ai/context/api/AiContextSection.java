package com.datausher.ai.context.api;

import java.util.List;
import java.util.Map;

public record AiContextSection(
        String title,
        List<AiContextItem> items,
        Map<String, String> attributes
) {
    public AiContextSection {
        title = AiContextValues.text(title, "title");
        items = items == null ? List.of() : List.copyOf(items);
        attributes = AiContextValues.attributes(attributes);
    }
}
