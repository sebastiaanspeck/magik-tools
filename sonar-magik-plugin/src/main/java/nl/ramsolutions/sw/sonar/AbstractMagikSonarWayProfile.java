package nl.ramsolutions.sw.sonar;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/**
 * Base for Magik-family "Sonar way" quality profiles.
 *
 * <p>Every subclass registers the profile for exactly one SonarQube language, since SonarQube ties
 * a {@link NewBuiltInQualityProfile} to a single language.
 */
public abstract class AbstractMagikSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Sonar way";

  /**
   * SonarQube language key this profile belongs to.
   *
   * @return Language key.
   */
  protected abstract String languageKey();

  /**
   * Repository key the active rules belong to.
   *
   * @return Repository key.
   */
  protected abstract String repositoryKey();

  /**
   * Resource path of the {@code Sonar_way_profile.json} listing the active rule keys.
   *
   * @return Profile location.
   */
  protected abstract String profileLocation();

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(
            AbstractMagikSonarWayProfile.PROFILE_NAME, this.languageKey());
    BuiltInQualityProfileJsonLoader.load(profile, this.repositoryKey(), this.profileLocation());
    profile.done();
  }
}
