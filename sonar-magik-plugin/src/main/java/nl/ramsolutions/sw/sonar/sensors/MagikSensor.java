package nl.ramsolutions.sw.sonar.sensors;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.language.MagikLanguage;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import nl.ramsolutions.sw.sonar.visitors.MagikHighlighterVisitor;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

/** Magik squid Sensor. */
public class MagikSensor extends AbstractMagikFamilySensor<MagikFile> {

  public MagikSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    super(checkFactory, fileLinesContextFactory, noSonarFilter);
  }

  @Override
  protected String sensorName() {
    return "Magik Sensor";
  }

  @Override
  protected String languageKey() {
    return MagikLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return MagikCheckList.REPOSITORY_KEY;
  }

  @Override
  protected List<Class<? extends Check>> checkClasses() {
    return MagikCheckList.INSTANCE.getBaseChecks();
  }

  @Override
  protected FilePredicate extraPredicate(final FilePredicates predicates) {
    return predicates.all();
  }

  @Override
  protected MagikFile parseFile(final URI uri, final String content) {
    return new MagikFile(uri, content);
  }

  @Override
  protected void saveMetrics(
      final SensorContext context, final InputFile inputFile, final MagikFile file) {
    final FileMetrics metrics = new FileMetrics(file, true);

    saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfCode().size());
    saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLineCount());
    saveMetric(context, inputFile, CoreMetrics.CLASSES, metrics.numberOfExemplars());
    saveMetric(
        context,
        inputFile,
        CoreMetrics.FUNCTIONS,
        metrics.numberOfMethods() + metrics.numberOfProcedures());
    saveMetric(context, inputFile, CoreMetrics.STATEMENTS, metrics.numberOfStatements());
    saveMetric(context, inputFile, CoreMetrics.COMPLEXITY, metrics.fileComplexity());

    final FileLinesContext fileLinesContext = this.fileLinesContextFactory.createFor(inputFile);
    metrics
        .linesOfCode()
        .forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    metrics
        .executableLines()
        .forEach(
            line -> fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1));
    fileLinesContext.save();

    this.noSonarFilter.noSonarInFile(inputFile, metrics.nosonarLines());
  }

  @Override
  protected void saveHighlighting(
      final SensorContext context, final InputFile inputFile, final MagikFile file) {
    final MagikVisitor tokensVisitor = new MagikHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(file);
  }

  @Override
  protected void saveCpdTokens(
      final CpdTokenSaver cpdTokenSaver, final InputFile inputFile, final MagikFile file) {
    cpdTokenSaver.saveCpdTokens(inputFile, file);
  }
}
