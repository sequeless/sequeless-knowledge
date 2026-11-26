package org.sequeless.knowledge.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sequeless.kernel.values.ObjectId;

/**
 * Finds the retrieved objects a model reply cites.
 *
 * <p>A citation is a canonical UUID token in the reply, bounded on both sides by a character that is
 * neither hex nor a hyphen, so a UUID inside a longer hex-and-hyphen token is not a citation.
 * Uppercase hex is accepted: the token is parsed with {@link UUID#fromString(String)}, which ignores
 * case, and {@link ObjectId} equality is by UUID value. A token that is not one of the retrieved ids
 * is dropped.
 */
final class CitationParser {

    private static final Pattern UUID_TOKEN = Pattern.compile("(?<![0-9a-fA-F-])"
            + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
            + "(?![0-9a-fA-F-])");

    private CitationParser() {}

    /**
     * @param reply the model's reply text; never {@code null}
     * @param retrieved the ids the retrieval returned; never {@code null}
     * @return the retrieved ids the reply cites, in order of first appearance, without duplicates;
     *     unmodifiable
     * @throws NullPointerException if an argument is {@code null}
     */
    static List<ObjectId> cited(String reply, Set<ObjectId> retrieved) {
        Objects.requireNonNull(reply, "reply must not be null");
        Objects.requireNonNull(retrieved, "retrieved must not be null");
        Set<ObjectId> cited = new LinkedHashSet<>();
        Matcher matcher = UUID_TOKEN.matcher(reply);
        while (matcher.find()) {
            ObjectId id = new ObjectId(UUID.fromString(matcher.group()));
            if (retrieved.contains(id)) {
                cited.add(id);
            }
        }
        return List.copyOf(cited);
    }
}
