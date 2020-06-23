package pt.ulisboa.tecnico.atocs.plugins.hbasecommon;

import pt.ulisboa.tecnico.atocs.core.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RequirementGenerator {

    private Configurator configurator = Configurator.getInstance();

    /**
     * Generates a Requirement for the table keys with an Equality property.
     *
     * @param operation the operation being performed.
     * @param tableNames all possible table names.
     */
    public void generateKeyEqualityRequirement(String operation, List<String> tableNames) {
        for (String tableName : tableNames)
            configurator.addRequirement(operation, tableName, new HBaseField(tableName, HBaseField.SpecialField.KEYS),
                    Property.EQUALITY);
    }

    /**
     * Generates a Requirement for the table keys with an Order property.
     *
     * @param operation the operation being performed.
     * @param tableNames all possible table names.
     */
    public void generateKeyOrderRequirement(String operation, List<String> tableNames) {
        for (String tableName : tableNames)
            configurator.addRequirement(operation, tableName, new HBaseField(tableName, HBaseField.SpecialField.KEYS),
                    Property.ORDER);
    }

    /**
     * Generates a Requirement for the table keys with a Search property.
     *
     * @param operation the operation being performed.
     * @param tableNames all possible table names.
     */
    public void generateKeySearchRequirement(String operation, List<String> tableNames) {
        for (String tableName : tableNames)
            configurator.addRequirement(operation, tableName, new HBaseField(tableName, HBaseField.SpecialField.KEYS),
                    Property.PARTIAL);
    }

    /**
     * Generates a Requirement for the table column family and qualifier with a Search property.
     *
     * @param operation the operation being performed.
     * @param tableNames all possible table names.
     */
    public void generateColumnSearchRequirement(String operation, List<String> tableNames) {
        for (String tableName : tableNames)
            configurator.addRequirement(operation, tableName, new HBaseField(tableName,
                    HBaseField.SpecialField.COLUMN_NAMES), Property.PARTIAL);
    }

    /**
     * Generates a Requirement for the increment operation for all the tableNames.
     *
     * @param tableNames all possible table names.
     * @param familyNames all possible family names.
     * @param qualifierNames all possible qualifier names.
     */
    public void generateIncrementRequirement(List<String> tableNames, List<String> familyNames,
                                             List<String> qualifierNames) {
        for (String tableName : tableNames) {
            for (String family : familyNames) {
                for (String qualifier : qualifierNames) {
                    configurator.addRequirement("INCREMENT", tableName, new HBaseField(tableName, family, qualifier),
                            Property.ALGEBRAIC_OP);
                }
            }
        }
    }

    /**
     * Generates a Requirement for a filter operation.
     *
     * @param filterName the filter class name.
     * @param tableNames all possible table names.
     * @param familyNames all possible family names.
     * @param qualifierNames all possible qualifier names.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateFilterRequirement(String filterName, List<String> tableNames, List<String> familyNames,
                                                 List<String> qualifierNames, List<String> compareOps,
                                                 String comparatorClass) {
        for (String tableName : tableNames) {
            for (String familyName : familyNames) {
                for (String qualifierName : qualifierNames) {
                    generateFilterRequirementForField(filterName, tableName, new HBaseField(tableName, familyName,
                                    qualifierName), compareOps, comparatorClass);
                }
            }
        }
    }

    /**
     * Generates a Requirement for a filter operation.
     *
     * @param filterName the filter class name.
     * @param tableNames all possible table names.
     * @param familyNames all possible family names.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateFilterRequirement(String filterName, List<String> tableNames, List<String> familyNames,
                                                 List<String> compareOps, String comparatorClass) {
        for (String tableName : tableNames) {
            for (String familyName : familyNames) {
                generateFilterRequirementForField(filterName, tableName, new HBaseField(tableName, familyName),
                        compareOps, comparatorClass);
            }
        }
    }

    /**
     * Generates a Requirement for a table key.
     *
     * @param filterName the filter class name.
     * @param tableNames all possible table names.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateKeyFilterRequirement(String filterName, List<String> tableNames,
                                                    List<String> compareOps, String comparatorClass) {
        for (String tableName : tableNames) {
            generateFilterRequirementForField(filterName, tableName, new HBaseField(tableName,
                            HBaseField.SpecialField.KEYS), compareOps, comparatorClass);
        }
    }

    /**
     * Generates a Requirement for a table qualifier name.
     *
     * @param filterName the filter class name.
     * @param tableNames all possible table names.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateQualifierFilterRequirement(String filterName, List<String> tableNames,
                                                          List<String> compareOps, String comparatorClass) {
        for (String tableName : tableNames) {
            generateFilterRequirementForField(filterName, tableName,
                    new HBaseField(tableName, HBaseField.SpecialField.QUALIFIER_NAMES), compareOps, comparatorClass);
        }
    }

    /**
     * Generates a Requirement for a table family name.
     *
     * @param filterName the filter class name.
     * @param tableNames all possible table names.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateFamilyFilterRequirement(String filterName, List<String> tableNames,
                                                       List<String> compareOps, String comparatorClass) {
        for (String tableName : tableNames) {
            generateFilterRequirementForField(filterName, tableName,
                    new HBaseField(tableName, HBaseField.SpecialField.FAMILY_NAMES), compareOps, comparatorClass);
        }
    }

    /**
     * Generates a Requirement for a filter operation.
     *
     * @param filterName the filter class name.
     * @param tableName the table name.
     * @param field the database field.
     * @param compareOps all possible compare operations.
     * @param comparatorClass the comparator class name.
     */
    public void generateFilterRequirementForField(String filterName, String tableName, HBaseField field,
                                                         List<String> compareOps, String comparatorClass) {
        switch (comparatorClass) {
            case HBaseInfo.BINARY_COMPONENT_COMPARATOR:
            case HBaseInfo.BINARY_PREFIX_COMPARATOR:
            case HBaseInfo.BIT_COMPARATOR:
            case HBaseInfo.SUB_STRING_COMPARATOR:
                configurator.addRequirement(filterName, tableName, field, Property.PARTIAL);
                break;
            case HBaseInfo.REGEX_STRING_COMPARATOR:
                configurator.addRequirement(filterName, tableName, field, Property.REGEX);
                break;
            default:
                for (String compareOp : compareOps) {
                    switch (compareOp) {
                        case "EQUAL":
                        case "NOT_EQUAL":
                            configurator.addRequirement(filterName, tableName, field, Property.EQUALITY);
                            break;
                        case "LESS":
                        case "LESS_OR_EQUAL":
                        case "GREATER_OR_EQUAL":
                        case "GREATER":
                            configurator.addRequirement(filterName, tableName, field, Property.ORDER);
                            break;
                    }
                }
        }
    }

    /**
     * Receives the map containing all the requirements already analysed. It is possible that some of the requirements
     * overlap other. For example, if we have a requirement on a column qualifier and a more restrictive one on a
     * column family, here we can remove the least restrictive requirement an keep the other one.
     *
     * @param requirementsMap all the requirements already analysed.
     */
    public void removeOverlappingRequirements(Map<String, Map<DbField, List<Requirement>>> requirementsMap) {
        for (String table : requirementsMap.keySet()) {
            Map<DbField, List<Requirement>> tableMap = requirementsMap.get(table);
            List<HBaseField> allFamilyFields = tableMap.keySet().stream().map(v -> (HBaseField) v)
                    .filter(HBaseField::isFamilyOnlyField).collect(Collectors.toList());
            List<HBaseField> allQualifierFields = tableMap.keySet().stream().map(v -> (HBaseField) v)
                    .filter(HBaseField::isQualifierField).collect(Collectors.toList());
            for (HBaseField qualifierField : allQualifierFields) {
                List<HBaseField> matchingFamilyFields = allFamilyFields.stream().filter(v -> v.getFamilyName()
                        .equals(qualifierField.getFamilyName())).collect(Collectors.toList());
                int maxWeight = getMaxWeight(matchingFamilyFields, tableMap);
                tableMap.get(qualifierField).removeIf(requirement -> requirement.getWeight() < maxWeight);
            }

            List<HBaseField> specialFields = tableMap.keySet().stream().map(v -> (HBaseField) v)
                    .filter(v -> v.isSpecialField() && (HBaseField.SpecialField.FAMILY_NAMES.equals(v.getSpecialField())
                    || HBaseField.SpecialField.QUALIFIER_NAMES.equals(v.getSpecialField())))
                    .collect(Collectors.toList());
            List<HBaseField> columnSpecialFields = tableMap.keySet().stream().map(v -> (HBaseField) v)
                    .filter(v -> v.isSpecialField() && HBaseField.SpecialField.COLUMN_NAMES.equals(v.getSpecialField()))
                    .collect(Collectors.toList());
            int maxColumnWeight = getMaxWeight(columnSpecialFields, tableMap);
            for (HBaseField specialField : specialFields) {
                tableMap.get(specialField).removeIf(requirement -> requirement.getWeight() < maxColumnWeight);
            }
        }
    }

    /**
     * Obtains the maximum weight of the requirements of the provided HBaseFields.
     *
     * @param fields list of HBaseFields.
     * @param tableMap all the requirements already analysed.
     * @return the maximum weight.
     */
    private int getMaxWeight(List<HBaseField> fields, Map<DbField, List<Requirement>> tableMap) {
        if (fields.isEmpty())
            return -1;
        int max = 0;
        for (HBaseField field : fields) {
            List<Requirement> req = tableMap.get(field);
            int fieldMax = req.stream().mapToInt(Requirement::getWeight).max().getAsInt();
            if (fieldMax > max)
                max = fieldMax;
        }
        return max;
    }

    public void addObtainedField(String tableName) {
        Configurator.getInstance().addObtainedField(tableName, new HBaseField(tableName));
    }

    public void addObtainedField(String tableName, String family) {
        Configurator.getInstance().addObtainedField(tableName, new HBaseField(tableName, family));
    }

    public void addObtainedField(String tableName, String family, String qualifier) {
        Configurator.getInstance().addObtainedField(tableName, new HBaseField(tableName, family, qualifier));
    }

    public void addObtainedField(List<String> tableNames, List<String> families, List<String> qualifiers) {
        for (String tableName : tableNames) {
            for (String family : families) {
                for (String qualifier : qualifiers) {
                    addObtainedField(tableName, family, qualifier);
                }
            }
        }
    }

    public void removeOverlappingObtainedFields(Map<String, Set<DbField>> obtainedFields) {
        for (String table : obtainedFields.keySet()) {
            Set<DbField> tableFields = obtainedFields.get(table);
            if (tableFields.stream().anyMatch(DbField::isAllTableField))
                tableFields.removeIf(v -> !(v.isAllTableField()));
            else {
                Set<HBaseField> familyFields = tableFields.stream().map(v -> (HBaseField)v)
                        .filter(HBaseField::isFamilyOnlyField).collect(Collectors.toSet());
                for (HBaseField familyField : familyFields) {
                    tableFields.removeIf(v -> ((HBaseField)v).isQualifierField()
                            && ((HBaseField)v).getFamilyName().equals(familyField.getFamilyName()));
                }
            }
        }
    }

}
