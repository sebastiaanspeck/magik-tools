package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;

/**
 * ProductDef and ModuleDef Sonar Way profile.
 *
 * <p>Combined into one profile, because they share the same file suffix (and therefore the same
 * SonarQube language).
 */
public final class ProductModuleDefSonarWayProfile extends AbstractMagikSonarWayProfile {

  private static final String PROFILE_LOCATION =
      "nl/ramsolutions/sw/sonar/l10n/product_module_def/rules/Sonar_way_profile.json";

  @Override
  protected String languageKey() {
    return ProductModuleDefLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return ProductModuleDefRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String profileLocation() {
    return ProductModuleDefSonarWayProfile.PROFILE_LOCATION;
  }
}
