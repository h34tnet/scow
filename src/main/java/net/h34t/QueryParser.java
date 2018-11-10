package net.h34t;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class QueryParser {

    private static final Pattern MOD_IDENTIFIER = Pattern.compile("^\\s*---:\\s*(\\w+)\\s*$");

    ParsedQuery parse(String text) {
        String[] lines = text.split("[\r\n]+");

        String modName = null;

        StringBuilder part = new StringBuilder();

        ParsedQuery pq = null;

        for (String line : lines) {
            Matcher m = MOD_IDENTIFIER.matcher(line);
            if (m.matches()) {
                String newModName = m.group(1);
                if (pq == null) {
                    pq = new ParsedQuery(part.toString());
                } else {
                    pq.addModifier(modName, part.toString());
                }
                modName = newModName;
                part = new StringBuilder();
            } else {
                part.append(line).append("\n");
            }
        }

        if (!part.toString().isEmpty()) {
            if (pq == null)
                pq = new ParsedQuery(part.toString());
            else
                pq.addModifier(modName, part.toString());
        }

        return pq;
    }

}
