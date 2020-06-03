package atocs.core.ciphers;

import java.util.ArrayList;

public class STD extends Cipher {
    private static STD instance;

    private STD() {
        properties = new ArrayList<>();
        securityLevel = 5;
    }

    public static STD getInstance() {
        if (instance == null)
            instance = new STD();
        return instance;
    }

    @Override
    String getName() {
        return "STD";
    }
}
