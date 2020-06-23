package pt.ulisboa.tecnico.atocs.core;

public enum Property {
    EQUALITY ("Equality", 0),
    ORDER ("Order", 1),
    REGEX ("Regex Search", 1),
    PARTIAL ("Partial Search", 1),
    WORD_SEARCH ("Word Search", 1),
    FORMAT ("Format", 1),
    ALGEBRAIC_OP ("Algebraic Op", 1);

    private final int weight;
    private final String value;

    Property(String s, int w) {
        value = s;
        weight = w;
    }

    public int getWeight() {
        return this.weight;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
