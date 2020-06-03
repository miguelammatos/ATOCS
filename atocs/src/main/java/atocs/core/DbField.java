package atocs.core;

import java.util.Objects;

public abstract class DbField {
    private final String table;
    private final String name;

    DbField(String table, String name) {
        this.table = table;
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DbField)) return false;
        DbField field = (DbField) o;
        return Objects.equals(getName(), field.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
