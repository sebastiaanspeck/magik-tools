package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public class ExemplarDefinitionVisitor extends MagikVisitor {

  private int count = 0;

  public int getCount() {
    return count;
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    count = 0;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.PROCEDURE_INVOCATION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode previousSibling = node.getPreviousSibling();
    if (previousSibling == null) {
      return;
    }

    String tokenValue = previousSibling.getTokenValue();
    if (!"def_slotted_exemplar".equalsIgnoreCase(tokenValue)
        && !"def_indexed_exemplar".equalsIgnoreCase(tokenValue)) {
      return;
    }

    AstNode args = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (args == null) {
      return;
    }

    AstNode arg = args.getFirstChild(MagikGrammar.ARGUMENT);
    if (arg == null) {
      return;
    }
    AstNode symbol = arg.getFirstDescendant(MagikGrammar.SYMBOL);
    if (symbol == null) {
      return;
    }

    // exemplar found
    count++;
  }
}
