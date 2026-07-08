package nl.ramsolutions.sw.magik.lint;

import java.util.Map;

/**
 * A single active rule, as read from a SonarQube quality profile backup XML.
 *
 * @param key Rule key, e.g. {@code CommentedCode}.
 * @param parameters Parameter values, keyed by the (space-separated) SonarQube parameter key, e.g.
 *     {@code min lines}.
 */
record ActiveRule(String key, Map<String, String> parameters) {}
