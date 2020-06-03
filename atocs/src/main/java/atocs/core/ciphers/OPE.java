package atocs.core.ciphers;

import atocs.core.Requirement;

import java.util.ArrayList;

public class OPE extends Cipher {
    private static OPE instance;

    private OPE() {
        properties = new ArrayList<>();
        properties.add(Requirement.Property.EQUALITY);
        properties.add(Requirement.Property.ORDER);
        securityLevel = 2;
    }

    public static OPE getInstance() {
        if (instance == null)
            instance = new OPE();
        return instance;
    }

    @Override
    String getName() {
        return "OPE";
    }
}
