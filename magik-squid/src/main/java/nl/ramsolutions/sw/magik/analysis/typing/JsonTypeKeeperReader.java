package nl.ramsolutions.sw.magik.analysis.typing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.GenericDeclaration;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper reader.
 */
public final class JsonTypeKeeperReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeKeeperReader.class);

    private static final String SW_PAKKAGE = "sw";

    private final ITypeKeeper typeKeeper;
    private final TypeReader typeParser;

    private JsonTypeKeeperReader(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeReader(this.typeKeeper);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void run(final Path path) throws IOException {
        LOGGER.debug("Reading type database from path: {}", path);

        final File file = path.toFile();
        int lineNo = 1;
        try (FileReader fileReader = new FileReader(file, StandardCharsets.ISO_8859_1);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                try {
                    this.processLine(line);
                } catch (final RuntimeException exception) {
                    LOGGER.error("Error parsing line {}, line data: {}", lineNo, line);
                    LOGGER.error(exception.getMessage(), exception);
                }

                ++lineNo;
                line = bufferedReader.readLine();
            }
        } catch (final JSONException exception) {
            // This will never be hit, the catch block above handles it.
            LOGGER.error("JSON Error reading line no: {}", lineNo);
            throw new IllegalStateException(exception);
        }
    }

    private void processLine(final String line) {
        if (line.startsWith("//")) {
            // Ignore comments.
            return;
        }

        final JSONTokener tokener = new JSONTokener(line);
        final JSONObject obj = new JSONObject(tokener);
        final String instruction = obj.getString("instruction");
        switch (instruction) {
            case "package":
                this.handlePackage(obj);
                break;

            case "type":
                this.handleType(obj);
                break;

            case "global":
                this.handleGlobal(obj);
                break;

            case "method":
                this.handleMethod(obj);
                break;

            case "procedure":
                this.handleProcedure(obj);
                break;

            case "condition":
                this.handleCondition(obj);
                break;

            case "binary_operator":
                this.handleBinaryOperator(obj);
                break;

            default:
                break;
        }
    }

    private void handlePackage(final JSONObject instruction) {
        final String pakkageName = instruction.getString("name");
        final Package pakkage = this.ensurePackage(pakkageName);

        instruction.getJSONArray("uses").forEach(useObj -> {
            final String use = (String) useObj;
            this.ensurePackage(use);
            pakkage.addUse(use);
        });
    }

    private void handleType(final JSONObject instruction) {
        final String typeFormat = instruction.getString("type_format");
        final String name = instruction.getString("type_name");
        final TypeString typeString = TypeStringParser.parseTypeString(name);
        final MagikType type;
        if (this.typeKeeper.getType(typeString) != UndefinedType.INSTANCE) {
            type = (MagikType) this.typeKeeper.getType(typeString);
        } else if ("intrinsic".equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.INTRINSIC, typeString);
        } else if ("slotted".equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.SLOTTED, typeString);
        } else if ("indexed".equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.INDEXED, typeString);
        } else {
            throw new InvalidParameterException("Unknown type: " + typeFormat);
        }

        if (instruction.has("doc")) {
            final String doc = instruction.getString("doc");
            type.setDoc(doc);
        }

        instruction.getJSONArray("slots").forEach(slotObj -> {
            final JSONObject slot = (JSONObject) slotObj;
            final String slotName = slot.getString("name");
            final String slotTypeName = slot.getString("type_name");
            final TypeString slotTypeString = TypeStringParser.parseTypeString(slotTypeName);
            type.addSlot(null, slotName, slotTypeString);
        });

        if (instruction.has("generics")) {
            instruction.getJSONArray("generics").forEach(genericObj -> {
                final JSONObject generic = (JSONObject) genericObj;
                final String genericName = generic.getString("name");
                final String genericDoc = generic.getString("doc");
                final GenericDeclaration genericDeclaration = type.addGeneric(null, genericName);
                genericDeclaration.setDoc(genericDoc);
            });
        }

        final JSONArray parents = instruction.getJSONArray("parents");
        parents.forEach(parentObj -> {
            final String parentStr = (String) parentObj;
            final TypeString parentRef = TypeString.ofIdentifier(parentStr, SW_PAKKAGE);
            type.addParent(parentRef);
        });
    }

    private void handleGlobal(final JSONObject instruction) {
        final String name = instruction.getString("name");
        final TypeString typeString = TypeString.ofIdentifier(name, TypeString.DEFAULT_PACKAGE);

        final String typeName = instruction.getString("type_name");
        final TypeString aliasedRef = TypeStringParser.parseTypeString(typeName);

        final AliasType global = new AliasType(this.typeKeeper, typeString, aliasedRef);
        this.typeKeeper.addType(global);
    }

    private void handleMethod(final JSONObject instruction) {
        final String typeName = instruction.getString("type_name");
        final TypeString typeRef = TypeString.ofIdentifier(typeName, TypeString.DEFAULT_PACKAGE);
        final AbstractType abstractType = this.typeKeeper.getType(typeRef);
        if (abstractType == UndefinedType.INSTANCE) {
            throw new IllegalStateException("Unknown type: " + typeName);
        }
        final MagikType type = (MagikType) abstractType;

        // What if method already exists? Might be improved by taking the line into account as well.
        final String methodName = instruction.getString("method_name");
        final Location location = instruction.get("source_file") != JSONObject.NULL
            ? new Location(Path.of(instruction.getString("source_file")).toUri())
            : null;
        final boolean isAlreadyKnown = type.getLocalMethods(methodName).stream()
            .anyMatch(method ->
                method.getLocation() == null && location == null
                || method.getLocation() != null && method.getLocation().equals(location));
        if (isAlreadyKnown) {
            LOGGER.debug("Skipping already known method: {}.{}", typeName, methodName);
            return;
        }

        final JSONArray modifiersArray = (JSONArray) instruction.getJSONArray("modifiers");
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final List<Parameter> parameters = this.parseParameters(instruction.getJSONArray("parameters"));
        final Parameter assignmentParameter;
        if (methodName.contains("<<")) {
            int lastIdx = parameters.size() - 1;
            assignmentParameter = parameters.get(lastIdx);
            parameters.remove(lastIdx);
        } else {
            assignmentParameter = null;
        }
        final ExpressionResultString result =  this.parseExpressionResultString(instruction.get("return_types"));
        final ExpressionResultString loopResult = this.parseExpressionResultString(instruction.get("loop_types"));
        final String methodDoc = instruction.get("doc") != JSONObject.NULL
            ? instruction.getString("doc")
            : null;
        type.addMethod(location, modifiers, methodName, parameters, assignmentParameter, methodDoc, result, loopResult);
    }

    private void handleCondition(final JSONObject instruction) {
        final String name = instruction.getString("name");
        final String doc = instruction.getString("doc");
        final String parent = instruction.getString("parent");
        final Location location = instruction.get("source_file") != JSONObject.NULL
            ? new Location(Path.of(instruction.getString("source_file")).toUri())
            : null;
        final JSONArray dataNameListArray = (JSONArray) instruction.getJSONArray("data_name_list");
        final List<String> dataNameList = StreamSupport.stream(dataNameListArray.spliterator(), false)
            .map(String.class::cast)
            .collect(Collectors.toList());
        final Condition condition = new Condition(location, name, parent, dataNameList, doc);
        this.typeKeeper.addCondition(condition);
    }

    private void handleBinaryOperator(final JSONObject instruction) {
        final BinaryOperator.Operator operator =
            BinaryOperator.Operator.valueFor(instruction.getString("operator"));
        final String lhsTypeStr = instruction.getString("lhs_type");
        final TypeString lhsTypeRef = TypeStringParser.parseTypeString(lhsTypeStr);
        final String rhsTypeStr = instruction.getString("rhs_type");
        final TypeString rhsTypeRef = TypeStringParser.parseTypeString(rhsTypeStr);
        final String returnTypeStr = instruction.getString("return_type");
        final TypeString resultTypeRef = TypeStringParser.parseTypeString(returnTypeStr);
        final BinaryOperator binaryOperator = new BinaryOperator(operator, lhsTypeRef, rhsTypeRef, resultTypeRef);
        this.typeKeeper.addBinaryOperator(binaryOperator);
    }

    private List<Parameter> parseParameters(final JSONArray parametersArray) {
        return StreamSupport.stream(parametersArray.spliterator(), false)
            .map(parameterThing -> {
                final JSONObject parameterObj = (JSONObject) parameterThing;
                final String name = parameterObj.getString("name");
                final Parameter.Modifier modifier = parameterObj.get("modifier") != JSONObject.NULL
                    ? Parameter.Modifier.valueOf(parameterObj.getString("modifier").toUpperCase())
                    : Parameter.Modifier.NONE;
                final String typeName = parameterObj.getString("type_name");
                final TypeString typeString = TypeStringParser.parseTypeString(typeName);
                final AbstractType type = this.typeParser.parseTypeString(typeString);
                return new Parameter(name, modifier, type);
            })
            .collect(Collectors.toList());
    }

    private ExpressionResultString parseExpressionResultString(final Object obj) {
        if (obj == JSONObject.NULL) {
            return ExpressionResultString.UNDEFINED;
        }

        if (obj instanceof String) {
            final String expressionResultString = (String) obj;
            return TypeStringParser.parseExpressionResultString(expressionResultString, TypeString.DEFAULT_PACKAGE);
        }

        if (obj instanceof JSONArray) {
            final JSONArray array = (JSONArray) obj;
            return StreamSupport.stream(array.spliterator(), false)
                .map(String.class::cast)
                .map(TypeStringParser::parseTypeString)
                .collect(ExpressionResultString.COLLECTOR);
        }

        throw new InvalidParameterException("Don't know what to do with: " + obj);
    }

    private void handleProcedure(final JSONObject instruction) {
        final JSONArray modifiersArray = (JSONArray) instruction.getJSONArray("modifiers");
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final Location location = instruction.get("source_file") != JSONObject.NULL
            ? new Location(Path.of(instruction.getString("source_file")).toUri())
            : null;
        final List<Parameter> parameters = this.parseParameters(instruction.getJSONArray("parameters"));
        final ExpressionResultString resultStr = this.parseExpressionResultString(instruction.get("return_types"));
        final ExpressionResultString loopResultStr = this.parseExpressionResultString(instruction.get("loop_types"));

        final String procedureName = instruction.getString("procedure_name");
        final TypeString procedureRef = TypeString.ofIdentifier("procedure", "sw");
        final MagikType procedureType = (MagikType) this.typeKeeper.getType(procedureRef);
        final String methodDoc = instruction.get("doc") != JSONObject.NULL
            ? instruction.getString("doc")
            : null;
        final ProcedureInstance instance = new ProcedureInstance(
            procedureType,
            location,
            procedureName,
            modifiers,
            parameters,
            methodDoc,
            resultStr,
            loopResultStr);

        // Create alias to instance.
        final String name = instruction.getString("name");
        final TypeString typeString = TypeString.ofIdentifier(name, TypeString.DEFAULT_PACKAGE);
        new AliasType(this.typeKeeper, typeString, instance);
    }

    private Package ensurePackage(final String pakkageName) {
        if (!this.typeKeeper.hasPackage(pakkageName)) {
            new Package(this.typeKeeper, pakkageName);
        }
        return this.typeKeeper.getPackage(pakkageName);
    }

    /**
     * Read types from a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {@link TypeKeeper} to fill.
     * @throws IOException -
     */
    public static void readTypes(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonTypeKeeperReader reader = new JsonTypeKeeperReader(typeKeeper);
        reader.run(path);
    }

}