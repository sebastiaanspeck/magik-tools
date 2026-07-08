package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses a SonarQube quality profile backup XML, as produced by the "Back up" action on a quality
 * profile (or {@code api/qualityprofiles/backup}).
 */
final class QualityProfileImporter {

  private QualityProfileImporter() {
    // Utility class.
  }

  /**
   * Parse the active rules from a quality profile backup XML file.
   *
   * @param xmlPath Path to the XML file.
   * @return Active rules found in the file.
   * @throws IOException -
   */
  static List<ActiveRule> parse(final Path xmlPath) throws IOException {
    final Document document;
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      final DocumentBuilder builder = factory.newDocumentBuilder();
      final File file = xmlPath.toFile();
      document = builder.parse(file);
    } catch (final ParserConfigurationException | SAXException exception) {
      throw new IOException("Cannot parse quality profile: " + xmlPath, exception);
    }

    final List<ActiveRule> activeRules = new ArrayList<>();
    final NodeList ruleNodes = document.getElementsByTagName("rule");
    for (int i = 0; i < ruleNodes.getLength(); i++) {
      final Element ruleElement = (Element) ruleNodes.item(i);
      final String key = QualityProfileImporter.childText(ruleElement, "key");
      if (key == null) {
        continue;
      }

      final Map<String, String> parameters = new LinkedHashMap<>();
      final NodeList parameterNodes = ruleElement.getElementsByTagName("parameter");
      for (int j = 0; j < parameterNodes.getLength(); j++) {
        final Element parameterElement = (Element) parameterNodes.item(j);
        final String parameterKey = QualityProfileImporter.childText(parameterElement, "key");
        final String parameterValue = QualityProfileImporter.childText(parameterElement, "value");
        if (parameterKey != null && parameterValue != null) {
          parameters.put(parameterKey, parameterValue);
        }
      }

      activeRules.add(new ActiveRule(key, parameters));
    }

    return activeRules;
  }

  private static String childText(final Element parent, final String tagName) {
    final NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      final Node child = children.item(i);
      if (child instanceof Element element && element.getTagName().equals(tagName)) {
        return element.getTextContent();
      }
    }

    return null;
  }
}
