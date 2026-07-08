package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for get_global_value() guarded with _isnt _unset. */
@Rule(key = GetGlobalValueUnsetCheck.CHECK_KEY)
public class GetGlobalValueUnsetCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "GetGlobalValueUnset";

  private static final String MESSAGE =
      "Do not guard '%s' with _isnt _unset; let a missing global fail instead.";
  private static final Set<String> GUARDED_CALLS =
      Set.of("get_global_value", "sw:get_global_value");

  @Override
  protected void walkPreEqualityExpression(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    final String operator = children.get(1).getTokenValue();
    if (!operator.equals("_isnt")) {
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

    if (this.hasElseHandling(node)) {
      return;
    }

    this.reportGuardedCalls(node, valueNode);

    final AstNode identifierNode = valueNode.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    final AstNode assignmentNode = this.getAssignmentNode(identifierNode);
    if (assignmentNode != null) {
      this.reportGuardedCalls(node, assignmentNode);
    }
  }

  private boolean isUnset(final AstNode node) {
    return node.is(MagikGrammar.ATOM) && node.getFirstChild(MagikGrammar.UNSET) != null;
  }

  private boolean hasElseHandling(final AstNode equalityNode) {
    final AstNode conditionalExpressionNode =
        equalityNode.getFirstAncestor(MagikGrammar.CONDITIONAL_EXPRESSION);
    if (conditionalExpressionNode == null) {
      return false;
    }

    final AstNode ifNode = conditionalExpressionNode.getParent();
    if (ifNode == null || ifNode.isNot(MagikGrammar.IF)) {
      return false;
    }

    // An _elif/_else means the missing-value case is handled elsewhere.
    return ifNode.hasDirectChildren(MagikGrammar.ELIF, MagikGrammar.ELSE);
  }

  private void reportGuardedCalls(final AstNode equalityNode, final AstNode searchNode) {
    for (final AstNode invocationNode :
        searchNode.getDescendants(MagikGrammar.PROCEDURE_INVOCATION)) {
      final ProcedureInvocationNodeHelper invocationHelper =
          new ProcedureInvocationNodeHelper(invocationNode);
      if (GUARDED_CALLS.stream().noneMatch(invocationHelper::isProcedureInvocationOf)) {
        continue;
      }

      final String identifier = invocationHelper.getInvokedIdentifier();
      this.addIssue(equalityNode, MESSAGE.formatted(identifier));
    }
  }

  @CheckForNull
  private AstNode getAssignmentNode(final AstNode identifierNode) {
    final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(identifierNode);
    if (scope == null) {
      return null;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
    if (scopeEntry == null) {
      return null;
    }

    final AstNode definitionNode = scopeEntry.getDefinitionNode();
    return definitionNode.getFirstAncestor(
        MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.VARIABLE_DEFINITION);
  }
}
