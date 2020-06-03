package atocs.core.ciphers;

import atocs.core.Requirement;

import java.util.ArrayList;

public class FPE extends Cipher {
    private static FPE instance;

    private FPE() {
        properties = new ArrayList<>();
        properties.add(Requirement.Property.EQUALITY);
        properties.add(Requirement.Property.FORMAT);
        securityLevel = 3;
    }

    public static FPE getInstance() {
        if (instance == null)
            instance = new FPE();
        return instance;
    }

    @Override
    String getName() {
        return "FPE";
    }
}
