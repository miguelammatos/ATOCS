package atocs.core.ciphers;

import atocs.core.Requirement;

import java.util.ArrayList;

public class HOM extends Cipher {
    private static HOM instance;

    private HOM() {
        properties = new ArrayList<>();
        properties.add(Requirement.Property.ALGEBRAIC_OP);
        securityLevel = 3;
    }

    public static HOM getInstance() {
        if (instance == null)
            instance = new HOM();
        return instance;
    }

    @Override
    String getName() {
        return "HOM";
    }
}
