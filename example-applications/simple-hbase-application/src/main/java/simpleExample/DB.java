package simpleExample;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DB {
    public static final String CONSTANT = "row8";

    public HashMap<String, Integer> db = new HashMap<String, Integer>();

    public void put(String key, Integer value){
        db.put(key, value);
    }

    public int get(String key){
        return db.get(key);
    }

    public ArrayList<Integer> scan(String key){
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (Map.Entry<String, Integer> entry : db.entrySet()) {
            if(entry.getValue().equals(key))
                out.add(entry.getValue());
        }
        return out;
    }

    public void delete(String key){
        db.remove(key);
    }

    public void test(byte[] bytes) {

    }
}
