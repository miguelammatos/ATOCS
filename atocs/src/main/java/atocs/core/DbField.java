package atocs.core;

import java.util.Objects;

public abstract class DbField {
    private final String table;
    private final String name;
    private boolean isSpecialField;
    private boolean isAllTableField;

    public DbField(String table, String name, boolean isSpecialField, boolean isAllTableField) {
        this.table = table;
        this.name = name;
        this.isSpecialField = isSpecialField;
        this.isAllTableField = isAllTableField;
    }

    public String getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    public boolean isSpecialField() {
        return isSpecialField;
    }

    public boolean isAllTableField() {
        return isAllTableField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbField dbField = (DbField) o;
        return isSpecialField() == dbField.isSpecialField() &&
                isAllTableField() == dbField.isAllTableField() &&
                Objects.equals(getTable(), dbField.getTable()) &&
                Objects.equals(getName(), dbField.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTable(), getName(), isSpecialField(), isAllTableField());
    }
}
