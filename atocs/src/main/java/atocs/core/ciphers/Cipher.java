package atocs.core.ciphers;

import atocs.core.Requirement;

import java.util.List;
import java.util.Objects;

public abstract class Cipher implements Comparable<Cipher> {
    protected List<Requirement.Property> properties;
    protected int securityLevel;

    public List<Requirement.Property> getProperties() {
        return properties;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public int compareTo(Cipher c) {
        if (getSecurityLevel() - c.getSecurityLevel() != 0) {
            return getSecurityLevel() - c.getSecurityLevel();
        } else {
            return c.getProperties().size() - getProperties().size(); // less is better
        }
    }

    abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cipher cipher = (Cipher) o;
        return getSecurityLevel() == cipher.getSecurityLevel() &&
                Objects.equals(getProperties(), cipher.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProperties(), getSecurityLevel());
    }
}
