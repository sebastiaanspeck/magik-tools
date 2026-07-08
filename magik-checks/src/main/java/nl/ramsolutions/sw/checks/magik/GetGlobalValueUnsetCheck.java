package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for comparing the result of get_global_value() against _unset. */
@Rule(key = GetGlobalValueUnsetCheck.CHECK_KEY)
public class GetGlobalValueUnsetCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "GetGlobalValueUnset";

  private static final String MESSAGE =
      "Do not silently test the result of '%s' against _unset, let it fail instead.";
  private static final Set<String> GUARDED_CALLS =
      Set.of("get_global_value", "sw:get_global_value");

  @Override
  protected void walkPreEqualityExpression(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    final String operator = children.get(1).getTokenValue();
    if (!operator.equals("_is") && !operator.equals("_isnt")) {
      return;
    }

    final AstNode leftNode = children.get(0);
    final AstNode rightNode = children.get(2);
    final AstNode valueNode;
    if (this.isUnset(leftNode)) {
      valueNode = rightNode;
    } else if (this.isUnset(rightNode)) {
      valueNode = leftNode;
    } else {
      return;
    }

    for (final AstNode invocationNode :
        valueNode.getDescendants(MagikGrammar.PROCEDURE_INVOCATION)) {
      final ProcedureInvocationNodeHelper invocationHelper =
          new ProcedureInvocationNodeHelper(invocationNode);
      if (GUARDED_CALLS.stream().noneMatch(invocationHelper::isProcedureInvocationOf)) {
        continue;
      }

      final String identifier = invocationHelper.getInvokedIdentifier();
      this.addIssue(node, MESSAGE.formatted(identifier));
    }
  }

  private boolean isUnset(final AstNode node) {
    return node.is(MagikGrammar.ATOM) && node.getFirstChild(MagikGrammar.UNSET) != null;
  }
}
