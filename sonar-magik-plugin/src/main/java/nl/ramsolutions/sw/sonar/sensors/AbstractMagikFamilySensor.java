package nl.ramsolutions.sw.sonar.sensors;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.squidbridge.ProgressReport;

/**
 * Base for Magik-family squid Sensors.
 *
 * <p>Every subclass scans exactly one kind of file (Magik, product.def, module.def, or
 * load_list.txt/patch_list.txt). The parsing, metrics, and highlighting are inherently different
 * per file kind and stay in the subclass; file discovery, check execution, and issue/CPD reporting
 * are shared here.
 *
 * @param <F> Concrete {@link OpenedFile} type this sensor scans.
 */
public abstract class AbstractMagikFamilySensor<F extends OpenedFile> implements Sensor {

  private static final Logger LOGGER = Loggers.get(AbstractMagikFamilySensor.class);
  private static final long SLEEP_PERIOD = 100;

  private final CheckFactory checkFactory;
  protected final FileLinesContextFactory fileLinesContextFactory;
  protected final NoSonarFilter noSonarFilter;

  protected AbstractMagikFamilySensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    this.checkFactory = checkFactory;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  protected abstract String sensorName();

  protected abstract String languageKey();

  protected abstract String repositoryKey();

  protected abstract List<Class<? extends Check>> checkClasses();

  /**
   * Additional predicate to narrow down the files scanned by this sensor, beyond "is a main file of
   * this language". E.g. a specific filename or path pattern.
   *
   * @param predicates Predicates factory.
   * @return Additional predicate.
   */
  protected abstract FilePredicate extraPredicate(FilePredicates predicates);

  protected abstract F parseFile(URI uri, String content);

  protected abstract void saveMetrics(SensorContext context, InputFile inputFile, F file);

  protected abstract void saveHighlighting(SensorContext context, InputFile inputFile, F file);

  protected abstract void saveCpdTokens(CpdTokenSaver cpdTokenSaver, InputFile inputFile, F file);

  @Override
  public void describe(final @NonNull SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(this.languageKey()).name(this.sensorName());
  }

  @Override
  public void execute(final @NonNull SensorContext context) {
    LOGGER.debug("Executing {}", this.sensorName());

    final FileSystem fileSystem = context.fileSystem();
    final FilePredicates predicates = fileSystem.predicates();
    final FilePredicate filePredicate =
        predicates.and(
            predicates.hasType(InputFile.Type.MAIN),
            predicates.hasLanguage(this.languageKey()),
            this.extraPredicate(predicates));

    final List<InputFile> inputFiles = new ArrayList<>();
    fileSystem.inputFiles(filePredicate).forEach(inputFiles::add);

    final ProgressReport progressReport =
        new ProgressReport("Report about progress of " + this.sensorName(), SLEEP_PERIOD);
    final List<String> filenames = inputFiles.stream().map(InputFile::toString).toList();
    progressReport.start(filenames);

    for (final InputFile inputFile : inputFiles) {
      this.scanFile(context, inputFile);
      progressReport.nextFile();
    }

    progressReport.stop();
  }

  private void scanFile(final SensorContext context, final InputFile inputFile) {
    LOGGER.debug("Scanning file: {}", inputFile);

    final URI uri = inputFile.uri();
    final String fileContent;
    try {
      fileContent = inputFile.contents();
    } catch (final IOException ex) {
      throw new IllegalStateException("Cannot read " + inputFile, ex);
    }

    final F file = this.parseFile(uri, fileContent);

    LOGGER.debug("Save measures");
    this.saveMetrics(context, inputFile, file);

    LOGGER.debug("Running checks");
    final Checks<Check> checks =
        this.checkFactory
            .<Check>create(this.repositoryKey())
            .addAnnotatedChecks(this.checkClasses());
    for (final Check check : checks.all()) {
      LOGGER.debug("Running check: {}", check);
      final List<Issue> issues = check.scanFileForIssues(file);
      final RuleKey ruleKey = checks.ruleKey(check);
      if (ruleKey == null) {
        continue;
      }

      this.saveIssues(context, ruleKey, issues, inputFile);
    }

    LOGGER.debug("Saving highlighted tokens");
    this.saveHighlighting(context, inputFile, file);

    LOGGER.debug("Saving CPD tokens");
    final CpdTokenSaver cpdTokenSaver = new CpdTokenSaver(context);
    this.saveCpdTokens(cpdTokenSaver, inputFile, file);
  }

  private void saveIssues(
      final SensorContext context,
      final RuleKey ruleKey,
      final List<Issue> issues,
      final InputFile inputFile) {
    for (final Issue issue : issues) {
      LOGGER.debug("Saving issue, file: {}, issue: {}", inputFile, issue);

      final NewIssue newIssue = context.newIssue();
      final NewIssueLocation location =
          newIssue.newLocation().on(inputFile).message(issue.message());
      final Integer line = issue.startLine();
      if (line != null) {
        location.at(inputFile.selectLine(line));
      }
      newIssue.at(location).forRule(ruleKey).save();
    }
  }

  /**
   * Save a single metric on the given file.
   *
   * @param context Sensor context.
   * @param inputFile File.
   * @param metric Metric.
   * @param value Value.
   */
  protected static void saveMetric(
      final SensorContext context,
      final InputFile inputFile,
      final Metric<Integer> metric,
      final Integer value) {
    LOGGER.debug("Saving metric, file: {}, metric: {} value: {}", inputFile, metric, value);

    context.<Integer>newMeasure().withValue(value).forMetric(metric).on(inputFile).save();
  }
}
