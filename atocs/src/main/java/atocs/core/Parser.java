package atocs.core;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import soot.*;
import atocs.core.api.API;
import atocs.core.api.Argument;
import atocs.core.exceptions.FileException;
import atocs.core.exceptions.ParsingException;
import atocs.core.exceptions.SystemException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private static Parser instance;

    private static final String BYTE_TYPE = "byte";
    private static final String BYTE_ARRAY_TYPE = "byte[]";
    private static final String INT_TYPE = "int";
    private static final String LONG_TYPE = "long";
    private static final String DOUBLE_TYPE = "double";
    private static final String FLOAT_TYPE = "float";
    private static final String CHAR_TYPE = "char";
    private static final String STRING_TYPE = "string";
    private static final String STRING_CLASS = "java.lang.String";

    private Parser() {}

    public static Parser getInstance() {
        if (instance == null)
            instance = new Parser();
        return instance;
    }

    /**
     * Parses the yaml config file with information about the application to be analysed.
     *
     * @param configFile path of the yaml file with the application information.
     */
    AtocsConfig parseConfigFile(String configFile) throws SystemException {
        try {
            YamlReader reader = new YamlReader(new FileReader(configFile));
            Map configMap = (Map) reader.read();
            String databaseName = (String) configMap.get("database");
            List<String> directoriesToAnalyse = new ArrayList<>();
            if (configMap.get("directoriesToAnalyse") != null) {
                for (Object directory : (List)configMap.get("directoriesToAnalyse")) {
                    directoriesToAnalyse.add((String) directory);
                }
            }
            List<String> entryPoints = new ArrayList<>();
            if (configMap.get("entryPoints") != null) {
                for (Object entryPoint : (List)configMap.get("entryPoints")) {
                    entryPoints.add((String) entryPoint);
                }
            }
            List<String> cipherPreferences = new ArrayList<>();
            if (configMap.get("cipherPreferences") != null) {
                for (Object cipher : (List)configMap.get("cipherPreferences")) {
                    cipherPreferences.add((String) cipher);
                }
            }
            Map<String, List<String>> supportedCiphers = new HashMap<>();
            if (configMap.get("supportedCiphers") != null) {
                for (Object cipherObj : (List)configMap.get("supportedCiphers")) {
                    Map cipherMap = (Map) cipherObj;
                    String cipherName = (String) cipherMap.get("name");
                    List propertiesList = (List) cipherMap.get("properties");
                    List<String> properties = new ArrayList<>();
                    if (propertiesList != null) {
                        for (Object prop : propertiesList) {
                            properties.add((String) prop);
                        }
                    }
                    supportedCiphers.put(cipherName, properties);
                }
            }
            return new AtocsConfig(databaseName, directoriesToAnalyse, entryPoints, cipherPreferences, supportedCiphers);
        } catch (FileNotFoundException fnfe) {
            throw new FileException(configFile);
        } catch (YamlException ye) {
            throw new ParsingException(configFile);
        }
    }

    /**
     * Parses the yaml api file with the database API in use and loads the API class with the parsed data.
     *
     * @param apiFile path of the yaml file with the database API.
     */
    public void parseApiFile(String apiFile) throws SystemException {
        try {
            YamlReader reader = new YamlReader(new FileReader(apiFile));
            while (true) {
                Map classDef = (Map) reader.read();
                if (classDef == null) break;
                String className = (String) classDef.get("className"); //field className
                List methods = (List) classDef.get("methods"); //field methods
                for (Object method : methods) {
                    Map methodmap = (Map) method;
                    String methodName = (String) methodmap.get("name");  //field methods.name
                    String operation = (String) methodmap.get("operation"); //field methods.operation
                    List argList = (List) methodmap.get("args"); //field methods.args . This is a List<List<Map>>>
                    // representing all possible sets of arguments for the given method
                    for (Object args : argList) {
                        List arguments = (List) args; //List<Map> representing a set of arguments
                        addMethodToApi(className, methodName, arguments, operation);
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            throw new FileException(apiFile);
        } catch (YamlException ye) {
            throw new ParsingException(apiFile);
        }
    }

    /**
     * Adds a method to the API.
     *
     * @param className name of the declaring class of the method.
     * @param methodName name of the method.
     * @param arguments list of arguments of the method.
     */
    void addMethodToApi(String className, String methodName, List arguments, String operation) {
        List<Argument> argumentList = createArgumentList(arguments);
        API.getInstance().addMethod(className, methodName, argumentList, operation);
    }

    /**
     * Creates an argument list of a certain method from the string in the yaml file. Each
     * argument is an Argument object.
     *
     * @param arguments list of arguments provided by the yaml file.
     * @return  list of arguments.
     */
    List<Argument> createArgumentList(List arguments) {
        List<Argument> argumentList = new ArrayList<>();
        for (Object argObj : arguments) {
            Map argMap = (Map) argObj; //Map representing an argument, with type and tag
            String type = (String) argMap.get("type"); //field methods.args.type
            String typeLower = type.toLowerCase();
            String tag = (String) argMap.get("tag"); //field methods.args.tag
            Argument arg = null;
            switch (typeLower){
                case BYTE_TYPE:
                    arg = new Argument(ByteType.v(), tag);
                    break;
                case BYTE_ARRAY_TYPE:
                    arg = new Argument(ArrayType.v(ByteType.v(), 1), tag);
                    break;
                case INT_TYPE:
                    arg = new Argument(IntType.v(), tag);
                    break;
                case LONG_TYPE:
                    arg = new Argument(LongType.v(), tag);
                    break;
                case DOUBLE_TYPE:
                    arg = new Argument(DoubleType.v(), tag);
                    break;
                case FLOAT_TYPE:
                    arg = new Argument(FloatType.v(), tag);
                    break;
                case CHAR_TYPE:
                    arg = new Argument(CharType.v(), tag);
                    break;
                case STRING_TYPE:
                    arg = new Argument(RefType.v(STRING_CLASS), tag);
                    break;
                default: // considered to be an Object where type is the complete name of the corresponding class
                    arg = new Argument(RefType.v(type), tag);
                    break;
            }
            argumentList.add(arg);
        }
        return argumentList;
    }

}
