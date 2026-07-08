package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;

/** LoadList Sonar Way profile. */
public final class LoadListSonarWayProfile extends AbstractMagikSonarWayProfile {

  private static final String PROFILE_LOCATION =
      LoadListCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  protected String languageKey() {
    return LoadListLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return LoadListRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String profileLocation() {
    return LoadListSonarWayProfile.PROFILE_LOCATION;
  }
}
