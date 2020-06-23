package pt.ulisboa.tecnico.atocs.plugins.hbasecommon;

import pt.ulisboa.tecnico.atocs.core.DbField;

import java.util.Objects;

public class HBaseField extends DbField {
    public enum SpecialField {
        KEYS("*keys*"),
        QUALIFIER_NAMES ("*all qualifier names*"),
        FAMILY_NAMES ("*all family names*"),
        COLUMN_NAMES ("*all family and qualifier names*");

        private final String value;
        SpecialField(String s) {
            value = s;
        }
        public String toString() {
            return this.value;
        }
    }

    private String familyName;
    private String qualifierName;
    private SpecialField specialField;

    public HBaseField(String table) {
        super(table, "All Fields", false, true);
        this.familyName = "";
        this.qualifierName = "";
    }

    public HBaseField(String table, String familyName) {
        super(table, familyName, false, false);
        this.familyName = familyName;
        this.qualifierName = "";
    }

    public HBaseField(String table, String familyName, String qualifierName) {
        super(table, familyName + ":" + qualifierName, false, false);
        this.familyName = familyName;
        this.qualifierName = qualifierName;
    }

    public HBaseField(String table, SpecialField specialField) {
        super(table, specialField.toString(), true, false);
        this.specialField = specialField;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getQualifierName() {
        return qualifierName;
    }

    public SpecialField getSpecialField() {
        return specialField;
    }

    public boolean isFamilyOnlyField() {
        return !isSpecialField() && !familyName.isEmpty() && qualifierName.isEmpty();
    }

    public boolean isQualifierField() {
        return !isSpecialField() && !familyName.isEmpty() && !qualifierName.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HBaseField that = (HBaseField) o;
        return Objects.equals(getFamilyName(), that.getFamilyName()) &&
                Objects.equals(getQualifierName(), that.getQualifierName()) &&
                Objects.equals(getSpecialField(), that.getSpecialField());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFamilyName(), getQualifierName(), getSpecialField());
    }
}
