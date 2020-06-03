package atocs.core;

import atocs.plugins.DatabasePlugin;

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

    void showReport() {
        getUniqueRequirements();
        getUniqueObtainedFields();
        Map<DbField, List<Requirement>> unsupportedFields = mapCiphers();
        Map<DbField, String> optimisations = inferOptimisations();
        printFieldCiphers();
        printUnsupportedFieldRequirements(unsupportedFields);
        printOptimisations(optimisations);
    }

    Map<DbField, String> inferOptimisations() { // TODO fix
        Map<DbField, String> optimisations = new HashMap<>();
        for (String table : fieldCiphers.keySet()) {
            for (DbField field : fieldCiphers.get(table).keySet()) {
                if (fieldCiphers.get(table).get(field).getName().toLowerCase().equals("ope")) { //TODO fix OPE
                    if (field.getName().equals("*keys*") || (!obtainedFields.get(table).contains(field) &&
                            obtainedFields.get(table).stream().noneMatch(v -> v.getName().equals("All Fields"))))
                        optimisations.put(field, "No need to optimize OPE field.");
                    else
                        optimisations.put(field, "Optimize OPE field by duplicating it with a faster cipher for decryption.");
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

    void printFieldCiphers() {
        System.out.println("--- FIELD CIPHERS ---");
        for (String table : fieldCiphers.keySet()) {
            Map<DbField, Cipher> fieldMap = fieldCiphers.get(table);
            for (DbField field : fieldMap.keySet()) {
                System.out.println("Field Cipher:");
                System.out.println("\tTable: " + table);
                System.out.println("\tField: " + field.getName());
                System.out.println("\tCipher: " + fieldMap.get(field).toString());
            }
        }
        System.out.println("All remaining fields should use: " + getBestSecurityCipher());
        System.out.println("--- END FIELD CIPHERS ---\n");
    }

    void printUnsupportedFieldRequirements(Map<DbField, List<Requirement>> unsupportedFields) {
        System.out.println("--- UNSUPPORTED FIELD REQUIREMENTS ---");
        for (DbField field : unsupportedFields.keySet()) {
            System.out.println("Unsupported Field:");
            System.out.println("\tTable: " + field.getTable());
            System.out.println("\tField: " + field.getName());
            System.out.println("\tRequirements:");
            for (Requirement requirement : unsupportedFields.get(field)) {
                System.out.println("\t\t- " + requirement.getProperty());
            }
        }
        System.out.println("--- END UNSUPPORTED FIELD REQUIREMENTS ---\n");
    }

    void printOptimisations(Map<DbField, String> optimisations) {
        System.out.println("--- OPTIMISATIONS ---");
        for (DbField field : optimisations.keySet()) {
            System.out.println("Optimisation:");
            System.out.println("\tTable: " + field.getTable());
            System.out.println("\tField: " + field.getName());
            System.out.println("\tOptimise: " + optimisations.get(field));
        }
        System.out.println("--- END OPTIMISATIONS ---\n");
    }

    void printUniqueRequirements() {
        System.out.println("--- REQUIREMENTS ---");
        for (String table : getUniqueRequirements().keySet()) {
            Map<DbField, List<Requirement>> fieldMap = uniqueRequirementsMap.get(table);
            for (DbField field : fieldMap.keySet()) {
                for (Requirement requirement : fieldMap.get(field)) {
                    System.out.println("Requirement:");
                    System.out.println("\tTable: " + table);
                    System.out.println("\tField: " + field.getName());
                    System.out.println("\tProperty: " + requirement.getProperty());
                }
            }
        }
        System.out.println("--- END REQUIREMENTS ---\n");
    }

    void printAllObtainedFields() {
        System.out.println("--- OBTAINED FIELDS ---");
        for (String table : getUniqueObtainedFields().keySet()) {
            Set<DbField> fieldList = obtainedFields.get(table);
            for (DbField field : fieldList) {
                System.out.println("Obtained Field:");
                System.out.println("\tTable: " + table);
                System.out.println("\tField: " + field.getName());
            }
        }
        System.out.println("--- END OBTAINED FIELDS ---\n");
    }

    void printAllRequirements() {
        System.out.println("--- ALL REQUIREMENTS ---");
        for (Requirement requirement : allRequirementList) {
            System.out.println("Requirement:");
            System.out.println("\tOperation: " + requirement.getOperation());
            System.out.println("\tTable: " + requirement.getTable());
            System.out.println("\tField: " + requirement.getField().getName());
            System.out.println("\tProperty: " + requirement.getProperty());
        }
        System.out.println("--- END ALL REQUIREMENTS ---\n");
    }
}
