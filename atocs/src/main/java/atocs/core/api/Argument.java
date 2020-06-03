package atocs.core.api;

import soot.Type;

public class Argument {
    private Type type;
    private String tag;

    public Argument(Type type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    public Type getType() {
        return type;
    }

    public String getTag() {
        return tag;
    }

}
