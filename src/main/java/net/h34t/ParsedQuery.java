package net.h34t;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ParsedQuery {

    private final String query;
    private final List<Modifier> modifiers;

    ParsedQuery(String query) {
        this.query = query;
        this.modifiers = new ArrayList<>();
    }

    ParsedQuery addModifier(String name, String body) {
        if (this.modifiers.stream().anyMatch(m -> m.name.equals(name)))
            throw new RuntimeException("Modifier \"" + name + "\" is already defined.");

        this.modifiers.add(new Modifier(name, body));
        return this;
    }

    List<Modifier> getModifiers() {
        return modifiers.size() > 0
                ? this.modifiers
                : Collections.singletonList(new Modifier("base", ""))
                ;
    }

    String getFirstQuery() {
        return getModifiers().get(0).getFullBody();
    }

    class Modifier {

        final String name;
        final String body;

        Modifier(String name, String body) {
            this.name = name;
            this.body = body;
        }

        String getFullBody() {
            return query + "\n" + body;
        }
    }
}
