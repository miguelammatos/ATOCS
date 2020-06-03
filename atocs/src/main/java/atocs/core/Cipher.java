package atocs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cipher implements Comparable<Cipher> {
    private String name;
    private int preferenceLevel;
    private List<Property> properties;

    Cipher(String name, int preferenceLevel) {
        this.name = name;
        this.preferenceLevel = preferenceLevel;
        this.properties = new ArrayList<>();
    }

    String getName() {
        return name;
    }

    List<Property> getProperties() {
        return properties;
    }

    void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    void addProperty(Property property) {
        this.properties.add(property);
    }

    int getPreferenceLevel() {
        return preferenceLevel;
    }

    void setPreferenceLevel(int preferenceLevel) {
        this.preferenceLevel = preferenceLevel;
    }

    public int compareTo(Cipher c) {
        if (getPreferenceLevel() - c.getPreferenceLevel() != 0) {
            return getPreferenceLevel() - c.getPreferenceLevel();
        } else {
            return c.getProperties().size() - getProperties().size(); // less is better
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cipher cipher = (Cipher) o;
        return getPreferenceLevel() == cipher.getPreferenceLevel() &&
                Objects.equals(getProperties(), cipher.getProperties()) &&
                getName().equals(cipher.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProperties(), getPreferenceLevel(), getName());
    }
}
