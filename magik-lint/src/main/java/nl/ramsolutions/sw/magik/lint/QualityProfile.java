package nl.ramsolutions.sw.magik.lint;

import java.util.List;

/**
 * A single SonarQube quality profile backup, as read from an XML export.
 *
 * @param language SonarQube language key the profile applies to, e.g. {@code magik},
 *     {@code product_module_def} or {@code load_list}.
 * @param activeRules Active rules found in the profile.
 */
record QualityProfile(String language, List<ActiveRule> activeRules) {}
