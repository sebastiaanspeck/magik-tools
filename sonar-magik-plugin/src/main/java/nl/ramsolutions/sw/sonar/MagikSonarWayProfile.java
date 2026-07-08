package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.sonar.language.MagikLanguage;

/** Magik Sonar Way profile. */
public final class MagikSonarWayProfile extends AbstractMagikSonarWayProfile {

  private static final String PROFILE_LOCATION =
      MagikCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  protected String languageKey() {
    return MagikLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return MagikCheckList.REPOSITORY_KEY;
  }

  @Override
  protected String profileLocation() {
    return MagikSonarWayProfile.PROFILE_LOCATION;
  }
}
