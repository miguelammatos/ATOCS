package atocs.core;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import soot.*;
import atocs.core.api.API;
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
     * @param argumentList list of arguments of the method.
     */
    void addMethodToApi(String className, String methodName, List argumentList, String operation) {
        List<Type> arguments = createArgumentList(argumentList);
        API.getInstance().addMethod(className, methodName, arguments, operation);
    }

    /**
     * Creates an argument list of a certain method from the string in the yaml file.
     *
     * @param arguments list of arguments provided by the yaml file.
     * @return list of argument types.
     */
    List<Type> createArgumentList(List arguments) {
        List<Type> argumentList = new ArrayList<>();
        for (Object argObj : arguments) {
            String type = (String) argObj;
            String typeLower = type.toLowerCase();
            Type arg;
            switch (typeLower){
                case BYTE_TYPE:
                    arg = ByteType.v();
                    break;
                case BYTE_ARRAY_TYPE:
                    arg = ArrayType.v(ByteType.v(), 1);
                    break;
                case INT_TYPE:
                    arg = IntType.v();
                    break;
                case LONG_TYPE:
                    arg = LongType.v();
                    break;
                case DOUBLE_TYPE:
                    arg = DoubleType.v();
                    break;
                case FLOAT_TYPE:
                    arg = FloatType.v();
                    break;
                case CHAR_TYPE:
                    arg = CharType.v();
                    break;
                case STRING_TYPE:
                    arg = RefType.v(STRING_CLASS);
                    break;
                default: // considered to be an Object where type is the complete name of the corresponding class
                    arg = RefType.v(type);
                    break;
            }
            argumentList.add(arg);
        }
        return argumentList;
    }

}
