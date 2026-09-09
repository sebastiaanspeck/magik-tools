package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;

/**
 * Parses SW-style method doc comments.
 *
 * <p>Unlike {@link TypeDocParser}, which parses structured {@code @param}/{@code @return} tags,
 * SW-style method doc comments reference parameters by their upper-cased name in free-form text,
 * e.g.:
 *
 * <pre>
 * _method example.method(p1, p2)
 *     ## Example method. P1 is used for foo, P2 for bar.
 * _endmethod
 * </pre>
 */
public final class SwMethodDocParser {

  /** Regexp used to find upper-cased parameter name references in a doc comment line. */
  public static final String PARAMETER_REGEXP = "[ \t]?([\\p{Lu}\\d_?]+)[^\\p{Lu}\\d_?]?";

  /** Pattern used to find upper-cased parameter name references in a doc comment line. */
  public static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEXP);

  private final AstNode node;

  public SwMethodDocParser(final AstNode node) {
    this.node = node;
  }

  /**
   * Get ranges of upper-cased parameter name references in the doc comment, mapped to the
   * (upper-cased) name referenced.
   *
   * @return Map of {@link Range} to referenced (upper-cased) name.
   */
  public Map<Range, String> getParameterNameRanges() {
    final Map<Range, String> ranges = new LinkedHashMap<>();
    MagikCommentExtractor.extractDocCommentTokens(this.node)
        .forEach(token -> this.addParameterNameRanges(token, ranges));
    return ranges;
  }

  private void addParameterNameRanges(final Token token, final Map<Range, String> ranges) {
    final String value = token.getValue();
    final Matcher matcher = PARAMETER_PATTERN.matcher(value);
    while (matcher.find()) {
      final String name = matcher.group(1);
      final int startColumn = token.getColumn() + matcher.start(1);
      final int endColumn = token.getColumn() + matcher.end(1);
      final Position startPosition = new Position(token.getLine(), startColumn);
      final Position endPosition = new Position(token.getLine(), endColumn);
      final Range range = new Range(startPosition, endPosition);
      ranges.put(range, name);
    }
  }
}
