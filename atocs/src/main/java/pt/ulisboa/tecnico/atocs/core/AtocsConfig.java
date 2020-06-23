package pt.ulisboa.tecnico.atocs.core;

import pt.ulisboa.tecnico.atocs.core.exceptions.*;
import pt.ulisboa.tecnico.atocs.plugins.hbase2.HBase2Plugin;
import pt.ulisboa.tecnico.atocs.plugins.hbase98.HBase98Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtocsConfig {
    private String database;
    private List<String> directoriesToAnalyse;
    private List<String> entryPoints;
    private String classPath;
    private String apiFilePath;
    private List<String> databaseLibPaths;
    private DatabasePlugin databasePlugin;

    AtocsConfig(String database, List<String> directoriesToAnalyse, List<String> entryPoints,
                List<String> orderedCiphers, Map<String, List<String>> supportedCiphers) throws SystemException {
        assertArguments(database, directoriesToAnalyse, entryPoints, orderedCiphers, supportedCiphers);
        this.database = database;
        this.directoriesToAnalyse = directoriesToAnalyse;
        this.entryPoints = entryPoints;

        setDatabaseInfo(database);
        assertDatabaseInfo(database);
        classPath = appendDoubleDotsToStrings(this.directoriesToAnalyse) + appendDoubleDotsToStrings(this.databaseLibPaths);

        if (supportedCiphers.isEmpty())
            setDefaultCiphers(orderedCiphers);
        else
            setSupportedCiphers(supportedCiphers, orderedCiphers);

        createOutputFile();
    }

    void setDatabaseInfo(String database) throws SystemException {
        switch (database.toLowerCase()) {
            case "hbase2":
                databasePlugin = new HBase2Plugin(HBase2Plugin.HBaseDbName.HBASE2);
                break;
            case "hbase98":
                databasePlugin = new HBase98Plugin(HBase98Plugin.HBaseDbName.HBASE98);
                break;
            case "safenosql":
                databasePlugin = new HBase2Plugin(HBase2Plugin.HBaseDbName.SAFENOSQL);
                break;
            default:
                throw new DatabaseModuleNotFoundException(database);
        }
        apiFilePath = databasePlugin.getApiFilePath();
        databaseLibPaths = databasePlugin.getLibPaths();
    }

    void setDefaultCiphers(List<String> orderedCiphers) throws UnknownEncryptionSchemeException {
        List<Cipher> ciphers = new ArrayList<>();
        int count = orderedCiphers.size();
        for (String cipherName : orderedCiphers) {
            Cipher cipher = new Cipher(cipherName, count);
            switch (cipherName.toLowerCase()) {
                case "std":
                    break;
                case "det":
                    cipher.addProperty(Property.EQUALITY);
                    break;
                case "ope":
                    cipher.addProperty(Property.EQUALITY);
                    cipher.addProperty(Property.ORDER);
                    break;
                case "fpe":
                    cipher.addProperty(Property.EQUALITY);
                    cipher.addProperty(Property.FORMAT);
                    break;
                case "hom":
                    cipher.addProperty(Property.ALGEBRAIC_OP);
                    break;
                case "se":
                    cipher.addProperty(Property.EQUALITY);
                    cipher.addProperty(Property.PARTIAL);
                    cipher.addProperty(Property.REGEX);
                    cipher.addProperty(Property.WORD_SEARCH);
                    break;
                default:
                    throw new UnknownEncryptionSchemeException(cipherName);
            }
            ciphers.add(cipher);
            count--;
        }
        Configurator.getInstance().init(databasePlugin, ciphers);
    }

    void setSupportedCiphers(Map<String, List<String>> cipherPreferences, List<String> orderedCiphers) throws EncryptionSchemePropertyException {
        List<Cipher> ciphers = new ArrayList<>();
        int count = orderedCiphers.size();
        for (String cipherName : orderedCiphers) {
            Cipher cipher = new Cipher(cipherName, count);
            List<String> propertiesString = cipherPreferences.get(cipherName);
            if (propertiesString == null)
                throw new EncryptionSchemePropertyException("Unable to obtain the properties for cipher " + cipher);
            for (String propertyString : propertiesString) {
                switch (propertyString.toLowerCase()) {
                    case "equality":
                        cipher.addProperty(Property.EQUALITY);
                        break;
                    case "order":
                        cipher.addProperty(Property.ORDER);
                        break;
                    case "algebraic":
                        cipher.addProperty(Property.ALGEBRAIC_OP);
                        break;
                    case "format":
                        cipher.addProperty(Property.FORMAT);
                        break;
                    case "regex":
                        cipher.addProperty(Property.REGEX);
                        break;
                    case "partial":
                        cipher.addProperty(Property.PARTIAL);
                        break;
                    case "word":
                        cipher.addProperty(Property.WORD_SEARCH);
                        break;
                    default:
                        throw new EncryptionSchemePropertyException("Unknown cipher property: " + propertyString);
                }
            }
            ciphers.add(cipher);
            count--;
        }
        Configurator.getInstance().init(databasePlugin, ciphers);
    }

    void createOutputFile() throws ReportException {
        try {
            File myObj = new File(Constants.REPORT_FILE_NAME);
            if (!myObj.createNewFile()) {
                FileWriter writer = new FileWriter(Constants.REPORT_FILE_NAME, false);
                writer.write("");
                writer.close();
            }
        } catch (IOException e) {
            throw new ReportException();
        }
    }

    String getDatabase() {
        return database;
    }

    List<String> getDirectoriesToAnalyse() {
        return directoriesToAnalyse;
    }

    List<String> getEntryPoints() {
        return entryPoints;
    }

    String getClassPath() {
        return classPath;
    }

    String getApiFilePath() {
        return apiFilePath;
    }

    DatabasePlugin getDatabasePlugin() {
        return databasePlugin;
    }

    private void assertArguments(String database, List<String> directoriesToAnalyse, List<String> entryPoints,
                                 List<String> orderedCiphers, Map<String, List<String>> supportedCiphers) throws SystemException {
        if (database == null)
            throw new ApplicationConfigurationException("Missing database name.");
        if (directoriesToAnalyse == null || directoriesToAnalyse.isEmpty())
            throw new ApplicationConfigurationException("Missing directories to analyse.");
        if (entryPoints == null)
            throw new ApplicationConfigurationException("Error parsing entry points.");
        if (orderedCiphers == null || orderedCiphers.isEmpty())
            throw new ApplicationConfigurationException("Missing ordered cipher preferences.");
        if (supportedCiphers == null)
            throw new ApplicationConfigurationException("Error parsing supported ciphers.");
    }

    private void assertDatabaseInfo(String database) throws DatabaseConfigurationException {
        if (apiFilePath == null || apiFilePath.isEmpty() || databaseLibPaths == null || databaseLibPaths.isEmpty())
            throw new DatabaseConfigurationException(database);
    }

    private String appendDoubleDotsToStrings(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String libPath : list)
            builder.append(libPath).append(":");
        return builder.toString();
    }
}
