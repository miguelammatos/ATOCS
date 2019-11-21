package testers;
public class Client {
    public static String getKey() {
        DB db = new DB();
        db.put("row4", 2);

        return "row1";
    }

    public static void main(String[] args) {
        DB db = new DB();
        int value = 123;
        String key = getKey();

        db.put(key, value);
        db.put("row2", 444);

        db.get("row2");

        db.scan(key);

        db.put("row3", db.get(key));

        db.delete("row2");

        int v = 0;

        if (v == 0)
            v = 1;
        else
            v = 2;

        db.put("row6", v);
    }
}



