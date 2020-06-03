package atocs;

import atocs.core.Configurator;
import atocs.core.ciphers.*;
import atocs.core.exceptions.ApplicationConfigurationException;
import atocs.core.exceptions.DatabaseConfigurationException;
import atocs.core.exceptions.DatabaseModuleNotFoundException;
import atocs.core.exceptions.SystemException;
import atocs.plugins.DatabasePlugin;
import atocs.plugins.hbase2.HBase2Plugin;
import atocs.plugins.hbase98.HBase98Plugin;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private String database;
    private List<String> directoriesToAnalyse;
    private List<String> entryPoints;
    private String classPath;
    private String apiFilePath;
    private List<String> databaseLibPaths;
    private DatabasePlugin databasePlugin;

    public Config(String database, List<String> directoriesToAnalyse, List<String> entryPoints) throws SystemException {
        if (database == null || directoriesToAnalyse == null || entryPoints == null)
            throw new ApplicationConfigurationException();
        this.database = database;
        this.directoriesToAnalyse = directoriesToAnalyse;
        this.entryPoints = entryPoints;
        setDatabaseInfo(database);
        if (apiFilePath == null || apiFilePath.isEmpty() || databaseLibPaths == null || databaseLibPaths.isEmpty())
            throw new DatabaseConfigurationException(database);
        classPath = appendDoubleDotsToStrings(this.directoriesToAnalyse) + appendDoubleDotsToStrings(this.databaseLibPaths);
//        this.directoriesToAnalyse.addAll(databaseLibPaths); // TODO check if this is the best option

        List<Cipher> ciphers = new ArrayList<>();
        ciphers.add(STD.getInstance());
        ciphers.add(DET.getInstance());
        ciphers.add(OPE.getInstance());
        ciphers.add(FPE.getInstance());
        ciphers.add(HOM.getInstance());
        Configurator.getInstance().init(databasePlugin, ciphers);
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

    public String getDatabase() {
        return database;
    }

    public List<String> getDirectoriesToAnalyse() {
        return directoriesToAnalyse;
    }

    public List<String> getEntryPoints() {
        return entryPoints;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getApiFilePath() {
        return apiFilePath;
    }

    public DatabasePlugin getDatabasePlugin() {
        return databasePlugin;
    }

    private String appendDoubleDotsToStrings(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String libPath : list)
            builder.append(libPath).append(":");
        return builder.toString();
    }
}
