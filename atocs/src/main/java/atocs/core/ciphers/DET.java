package atocs.core.ciphers;

import atocs.core.Requirement;

import java.util.ArrayList;

public class DET extends Cipher {
    private static DET instance;

    private DET() {
        properties = new ArrayList<>();
        properties.add(Requirement.Property.EQUALITY);
        securityLevel = 4;
    }

    public static DET getInstance() {
        if (instance == null)
            instance = new DET();
        return instance;
    }

    @Override
    String getName() {
        return "DET";
    }
}
