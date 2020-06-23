package pt.ulisboa.tecnico.atocs.core;

import pt.ulisboa.tecnico.atocs.core.exceptions.ReportException;

import java.io.*;
import java.util.*;

public class Configurator {
    private static Configurator instance;

    private DatabasePlugin db;
    private Map<String, Map<DbField, List<Requirement>>> uniqueRequirementsMap = new HashMap<>();
    private Map<String, Set<DbField>> obtainedFields = new HashMap<>();
    private List<Cipher> ciphers;
    private Map<String, Map<DbField, Cipher>> fieldCiphers = new HashMap<>();

    private List<Requirement> allRequirementList = new ArrayList<>();

    private Configurator() {}

    public static Configurator getInstance() {
        if (instance == null)
            instance = new Configurator();
        return instance;
    }

    void init(DatabasePlugin db, List<Cipher> ciphers) {
        this.db = db;
        this.ciphers = ciphers;
    }

    public void addRequirement(String operation, String tableName, DbField field, Property property) {
        Requirement newRequirement = new Requirement(operation, tableName, field, property);
        Map<DbField, List<Requirement>> tableMap = uniqueRequirementsMap.get(tableName);
        if (tableMap == null)
            tableMap = new HashMap<>();
        List<Requirement> fieldList = tableMap.get(field);
        if (fieldList == null)
            fieldList = new ArrayList<>();
        if (!fieldList.contains(newRequirement))
            fieldList.add(newRequirement);
        tableMap.putIfAbsent(field, fieldList);
        uniqueRequirementsMap.putIfAbsent(tableName, tableMap);

        allRequirementList.add(newRequirement);
    }

    public void addObtainedField(String tableName, DbField field) {
        obtainedFields.putIfAbsent(tableName, new HashSet<>());
        Set<DbField> tableList = obtainedFields.get(tableName);
        tableList.add(field);
    }

    Map<String, Map<DbField, List<Requirement>>> getUniqueRequirements() {
        db.removeOverlappingRequirements(uniqueRequirementsMap);
        return uniqueRequirementsMap;
    }

    Map<String, Set<DbField>> getUniqueObtainedFields() {
        db.removeOverlappingObtainedFields(obtainedFields);
        return obtainedFields;
    }

    void showReport() throws ReportException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter(Constants.REPORT_FILE_NAME, true)))) {
            getUniqueRequirements();
            getUniqueObtainedFields();
            Map<DbField, List<Requirement>> unsupportedFields = mapCiphers();
            Map<DbField, String> optimisations = inferOptimisations();
            printFieldCiphers(writer);
            printUnsupportedFieldRequirements(unsupportedFields, writer);
            printOptimisations(optimisations, writer);
            printAllObtainedFields(writer);
        } catch (IOException e) {
            throw new ReportException();
        }
    }

    Map<DbField, String> inferOptimisations() {
        Map<DbField, String> optimisations = new HashMap<>();
        for (String table : fieldCiphers.keySet()) {
            for (DbField field : fieldCiphers.get(table).keySet()) {
                if (fieldCiphers.get(table).get(field).getName().equalsIgnoreCase("ope")) {
                    if (field.isSpecialField() || (!obtainedFields.get(table).contains(field) &&
                            obtainedFields.get(table).stream().noneMatch(DbField::isAllTableField)))
                        optimisations.put(field, "No need to optimize OPE field.");
                    else
                        optimisations.put(field,
                                "Optimize OPE field by duplicating it with a faster cipher for decryption.");
                }
            }
        }
        return optimisations;
    }

    Map<DbField, List<Requirement>> mapCiphers() {
        fieldCiphers.clear();
        Map<DbField, List<Requirement>> unsupportedFields = new HashMap<>();
        for (String table : uniqueRequirementsMap.keySet()) {
            for (DbField field : uniqueRequirementsMap.get(table).keySet()) {
                List<Requirement> fieldRequirements = uniqueRequirementsMap.get(table).get(field);
                Cipher cipher = getBestCipher(fieldRequirements);
                if (cipher == null) {
                    unsupportedFields.put(field, fieldRequirements);
                } else {
                    fieldCiphers.putIfAbsent(table, new HashMap<>());
                    Map<DbField, Cipher> fieldCipher = fieldCiphers.get(table);
                    fieldCipher.put(field, cipher);
                }
            }
        }
        return unsupportedFields;
    }

    Cipher getBestCipher(List<Requirement> requirements) {
        List<Cipher> possibleCiphers = new ArrayList<>();
        for (Cipher cipher : ciphers) {
            boolean hasAllProp = true;
            for (Requirement requirement : requirements) {
                if (!cipher.getProperties().contains(requirement.getProperty())) {
                    hasAllProp = false;
                    break;
                }
            }
            if (hasAllProp) possibleCiphers.add(cipher);
        }
        if (possibleCiphers.isEmpty())
            return null;
        else {
            Collections.sort(possibleCiphers);
            return possibleCiphers.get(possibleCiphers.size()-1);
        }
    }

    Cipher getBestSecurityCipher() {
        Collections.sort(ciphers);
        return ciphers.get(ciphers.size()-1);
    }

    void printFieldCiphers(PrintWriter writer) {
        writer.println("--- FIELD CIPHERS ---");
        for (String table : fieldCiphers.keySet()) {
            Map<DbField, Cipher> fieldMap = fieldCiphers.get(table);
            for (DbField field : fieldMap.keySet()) {
                writer.println("Field Cipher:");
                writer.println("\tTable: " + table);
                writer.println("\tField: " + field.getName());
                writer.println("\tCipher: " + fieldMap.get(field).toString());
            }
        }
        writer.println("All remaining fields should use: " + getBestSecurityCipher());
        writer.println("--- END FIELD CIPHERS ---\n");
    }

    void printUnsupportedFieldRequirements(Map<DbField, List<Requirement>> unsupportedFields,
                                           PrintWriter writer) {
        if (!unsupportedFields.isEmpty()) {
            writer.println("--- UNSUPPORTED FIELD REQUIREMENTS ---");
            for (DbField field : unsupportedFields.keySet()) {
                writer.println("Unsupported Field:");
                writer.println("\tTable: " + field.getTable());
                writer.println("\tField: " + field.getName());
                writer.println("\tRequirements:");
                for (Requirement requirement : unsupportedFields.get(field)) {
                    writer.println("\t\t- " + requirement.getProperty());
                }
            }
            writer.println("--- END UNSUPPORTED FIELD REQUIREMENTS ---\n");
        }
    }

    void printOptimisations(Map<DbField, String> optimisations, PrintWriter writer) {
        if (!optimisations.isEmpty()) {
            writer.println("--- OPTIMISATIONS ---");
            for (DbField field : optimisations.keySet()) {
                writer.println("Optimisation:");
                writer.println("\tTable: " + field.getTable());
                writer.println("\tField: " + field.getName());
                writer.println("\tOptimise: " + optimisations.get(field));
            }
            writer.println("--- END OPTIMISATIONS ---\n");
        }
    }

    void printUniqueRequirements(PrintWriter writer) {
        writer.println("--- REQUIREMENTS ---");
        for (String table : getUniqueRequirements().keySet()) {
            Map<DbField, List<Requirement>> fieldMap = uniqueRequirementsMap.get(table);
            for (DbField field : fieldMap.keySet()) {
                for (Requirement requirement : fieldMap.get(field)) {
                    writer.println("Requirement:");
                    writer.println("\tTable: " + table);
                    writer.println("\tField: " + field.getName());
                    writer.println("\tProperty: " + requirement.getProperty());
                }
            }
        }
        writer.println("--- END REQUIREMENTS ---\n");
    }

    void printAllObtainedFields(PrintWriter writer) {
        writer.println("--- OBTAINED FIELDS ---");
        for (String table : getUniqueObtainedFields().keySet()) {
            Set<DbField> fieldList = obtainedFields.get(table);
            for (DbField field : fieldList) {
                writer.println("Obtained Field:");
                writer.println("\tTable: " + table);
                writer.println("\tField: " + field.getName());
            }
        }
        writer.println("--- END OBTAINED FIELDS ---\n");
    }

    void printAllRequirements(PrintWriter writer) {
        writer.println("--- ALL REQUIREMENTS ---");
        for (Requirement requirement : allRequirementList) {
            writer.println("Requirement:");
            writer.println("\tOperation: " + requirement.getOperation());
            writer.println("\tTable: " + requirement.getTable());
            writer.println("\tField: " + requirement.getField().getName());
            writer.println("\tProperty: " + requirement.getProperty());
        }
        writer.println("--- END ALL REQUIREMENTS ---\n");
    }
}
