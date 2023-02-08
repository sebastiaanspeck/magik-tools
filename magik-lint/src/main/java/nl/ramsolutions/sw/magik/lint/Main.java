package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.logging.LogManager;
import nl.ramsolutions.sw.magik.lint.output.MessageFormatReporter;
import nl.ramsolutions.sw.magik.lint.output.NullReporter;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * Main entry point for magik linter.
 */
public final class Main {

    private static final Options OPTIONS;
    private static final Option OPTION_MSG_TEMPLATE = Option.builder()
        .longOpt("msg-template")
        .desc("Output pattern")
        .hasArg()
        .type(PatternOptionBuilder.STRING_VALUE)
        .build();
    private static final Option OPTION_RCFILE = Option.builder()
        .longOpt("rcfile")
        .desc("Configuration file")
        .hasArg()
        .type(PatternOptionBuilder.FILE_VALUE)
        .build();
    private static final Option OPTION_SHOW_CHECKS = Option.builder()
        .longOpt("show-checks")
        .desc("Show checks and quit")
        .build();
    private static final Option OPTION_COLUMN_OFFSET = Option.builder()
        .longOpt("column-offset")
        .desc("Set column offset, positive or negative")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build();
    private static final Option OPTION_MAX_INFRACTIONS = Option.builder()
        .longOpt("max-infractions")
        .desc("Set max number of reporter infractions")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build();
    private static final Option OPTION_UNTABIFY = Option.builder()
        .longOpt("untabify")
        .desc("Expand tabs to N spaces")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build();
    private static final Option OPTION_DEBUG = Option.builder()
        .longOpt("debug")
        .desc("Enable showing of debug information")
        .build();
    private static final Option OPTION_HELP = Option.builder()
        .longOpt("help")
        .desc("Show this help")
        .build();

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(OPTION_HELP);
        OPTIONS.addOption(OPTION_MSG_TEMPLATE);
        OPTIONS.addOption(OPTION_RCFILE);
        OPTIONS.addOption(OPTION_SHOW_CHECKS);
        OPTIONS.addOption(OPTION_UNTABIFY);
        OPTIONS.addOption(OPTION_COLUMN_OFFSET);
        OPTIONS.addOption(OPTION_MAX_INFRACTIONS);
        OPTIONS.addOption(OPTION_DEBUG);
    }

    private static final Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING = Map.of(
        "Critical", 2,
        "Major", 4,
        "Minor", 8);

    private Main() {
    }

    /**
     * Parse the command line.
     *
     * @param args Command line arguments.
     * @return Parsed command line.
     * @throws ParseException -
     */
    private static CommandLine parseCommandline(final String[] args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(Main.OPTIONS, args);
    }

    /**
     * Initialize logger from logging.properties.
     */
    private static void initDebugLogger() {
        final ClassLoader classLoader = Main.class.getClassLoader();
        final InputStream stream = classLoader.getResourceAsStream("debug-logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create the reporter.
     *
     * @param configuration Configuration.
     * @return Reporter.
     */
    private static Reporter createReporter(final Configuration configuration) {
        final String msgTemplateOptName = OPTION_MSG_TEMPLATE.getLongOpt();
        final String template = configuration.hasProperty(msgTemplateOptName)
            ? configuration.getPropertyString(msgTemplateOptName)
            : MessageFormatReporter.DEFAULT_FORMAT;

        final String columnOffsetOptName = OPTION_COLUMN_OFFSET.getLongOpt();
        final String columnOffsetStr = configuration.getPropertyString(columnOffsetOptName);
        final Long columnOffset = configuration.hasProperty(columnOffsetOptName)
            ? Long.parseLong(columnOffsetStr)
            : null;

        return new MessageFormatReporter(System.out, template, columnOffset);
    }

    /**
     * Main entry point.
     *
     * @param args Arguments.
     * @throws IOException -
     * @throws ParseException -
     * @throws ReflectiveOperationException -
     */
    public static void main(final String[] args) throws ParseException, IOException, ReflectiveOperationException {
        final CommandLine commandLine;
        try {
            commandLine = Main.parseCommandline(args);
        } catch (UnrecognizedOptionException exception) {
            System.err.println("Unrecognized option: " + exception.getMessage());

            System.exit(1);
            return;  // Keep inferer happy.
        }

        if (commandLine.hasOption(OPTION_DEBUG)) {
            Main.initDebugLogger();
        }

        // Read configuration.
        final Configuration config;
        if (commandLine.hasOption(OPTION_RCFILE)) {
            final File rcfile = (File) commandLine.getParsedOptionValue(OPTION_RCFILE);
            final Path path = rcfile.toPath();
            if (!Files.exists(path)) {
                System.err.println("RC File does not exist: " + path);

                System.exit(1);
            }
            config = new Configuration(path);
        } else {
            final Path path = ConfigurationLocator.locateConfiguration();
            config = path != null
                ? new Configuration(path)
                : new Configuration();
        }

        // Fill configuration from command line.
        Main.copyOptionToConfig(commandLine, config, OPTION_UNTABIFY);
        Main.copyOptionToConfig(commandLine, config, OPTION_MAX_INFRACTIONS);
        Main.copyOptionToConfig(commandLine, config, OPTION_COLUMN_OFFSET);
        Main.copyOptionToConfig(commandLine, config, OPTION_MSG_TEMPLATE);

        // Show checks.
        if (commandLine.hasOption(OPTION_SHOW_CHECKS)) {
            final Reporter reporter = new NullReporter();
            final MagikLint lint = new MagikLint(config, reporter);
            final Writer writer = new PrintWriter(System.out);
            lint.showChecks(writer);
            writer.flush();
            System.exit(0);
        }

        // Help.
        if (commandLine.hasOption(OPTION_HELP)
            || commandLine.getArgs().length == 0) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("magik-lint", Main.OPTIONS);

            System.exit(0);
        }

        // Actual linting.
        final Reporter reporter = Main.createReporter(config);
        final MagikLint lint = new MagikLint(config, reporter);
        final String[] leftOverArgs = commandLine.getArgs();
        final Collection<Path> paths = MagikFileScanner.getFilesFromArgs(leftOverArgs);
        lint.run(paths);

        final int exitCode = reporter.reportedSeverities().stream()
            .map(severity -> SEVERITY_EXIT_CODE_MAPPING.get(severity))
            .reduce(0, (partial, sum) -> sum | partial);
        System.exit(exitCode);
    }

    private static void copyOptionToConfig(
            final CommandLine commandLine,
            final Configuration config,
            final Option option) {
        final String key = option.getLongOpt();
        if (commandLine.hasOption(key)) {
            final String value = commandLine.getOptionValue(key);
            config.setProperty(key, value);
        }
    }

}