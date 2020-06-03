package atocs.core;

import java.util.Objects;

public class Requirement {
    private String operation;
    private String table;
    private DbField field;
    private Property property;

    Requirement(String operation, String table, DbField field, Property property) {
        this.operation = operation;
        this.table = table;
        this.field = field;
        this.property = property;
    }

    public String getOperation() {
        return operation;
    }

    public String getTable() {
        return table;
    }

    public DbField getField() {
        return field;
    }

    public Property getProperty() {
        return property;
    }

    public int getWeight() {
        return getProperty().getWeight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Requirement that = (Requirement) o;
        return Objects.equals(getTable(), that.getTable()) &&
                Objects.equals(getField(), that.getField()) &&
                getProperty() == that.getProperty();
    }

}
