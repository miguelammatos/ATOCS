package atocs.plugins.hbasecommon.datastructures;

import atocs.core.datastructures.StringValueState;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ColumnFamilyAndQualifier {
    private Set<StringValueState> families;
    private Set<StringValueState> qualifiers;

    public ColumnFamilyAndQualifier() {
        this.families = new HashSet<>();
        this.qualifiers = new HashSet<>();
    }

    public ColumnFamilyAndQualifier(List<StringValueState> families, List<StringValueState> qualifiers) {
        this.families = new HashSet<>(families);
        this.qualifiers = new HashSet<>(qualifiers);
    }

    public Set<StringValueState> getFamilies() {
        return families;
    }

    public List<String> getStringFamilies() {
        return families.stream().map(StringValueState::getStringValue).collect(Collectors.toList());
    }

    public void setFamilies(List<StringValueState> families) {
        this.families = new HashSet<>(families);
    }

    public Set<StringValueState> getQualifiers() {
        return qualifiers;
    }

    public List<String> getStringQualifiers() {
        return qualifiers.stream().map(StringValueState::getStringValue).collect(Collectors.toList());
    }

    public void setQualifiers(List<StringValueState> qualifiers) {
        this.qualifiers = new HashSet<>(qualifiers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnFamilyAndQualifier that = (ColumnFamilyAndQualifier) o;
        return Objects.equals(getFamilies(), that.getFamilies()) &&
                Objects.equals(getQualifiers(), that.getQualifiers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFamilies(), getQualifiers());
    }
}
