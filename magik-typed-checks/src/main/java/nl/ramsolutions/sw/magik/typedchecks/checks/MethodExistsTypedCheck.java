package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check if method exists on type. */
@Rule(key = MethodExistsTypedCheck.CHECK_KEY)
public class MethodExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodExists";

  private static final String MESSAGE = "Unknown method: %s";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Get type.
    final TypeString calledTypeStr = this.getTypeInvokedOn(node);
    if (calledTypeStr.isUndefined()) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get method.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();

    final TypeString combinedTypeString2 = TypeString.combine(calledTypeStr);
    Objects.requireNonNull(combinedTypeString2);
    for (final TypeString typeString : combinedTypeString2.getCombinedTypes()) {
      final TypeStringResolver resolver = this.getTypeStringResolver();
      final Collection<MethodDefinition> methodDefs =
          resolver.getRespondingMethodDefinitions(typeString, methodName);

      // Add issue if no method is found.
      if (methodDefs.isEmpty()) {
        final String fullName = typeString.getFullString() + "." + methodName;
        final String message = String.format(MESSAGE, fullName);
        final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final AstNode issueNode = firstIdentifierNode != null ? firstIdentifierNode : node;
        this.addIssue(issueNode, message);
      }
    }
  }
}
