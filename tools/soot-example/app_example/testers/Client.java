package testers;
public class Client {
    public static final String CONST = "row7";

    public static String getKey() {
        DB db = new DB();
        db.put("row4", 2);

        String v = "";

        if (v.equals(""))
            v = "row1";
        else
            v = "row100";

        return v;
    }

    public static void main(String[] args) {
        DB db = new DB();
        int value = 123;
        String key = getKey();

        db.put(key, value);
        db.put("row2", 444);

        db.get("row2");

        db.scan(key);

        db.put("row3", db.get("row1"));

        db.delete("row2");

        int v = 0;

        if (v == 0)
            v = 1;
        else
            v = 2;

        db.put("row6", v);

        db.get(CONST); //retrieves value from constant

        db.get(DB.CONSTANT); //retrieves value from constant in another class

        byte[] bytes = "ola".getBytes();
        db.test(bytes);

        db.get(getKey());
    }
}
