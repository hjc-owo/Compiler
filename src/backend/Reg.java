package backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reg {
    private static final Reg instance = new Reg();

    private Map<String, Integer> regMap = new HashMap<>();
    private int size = 0;
    private static final int max = 8;

    public static Reg getInstance() {
        return instance;
    }

    public Map<String, Integer> getRegMap() {
        return regMap;
    }

    public String getReg(int i) {
        if (i >= 0) return "$s" + i;
        else return null;
    }

    public String loadFromRegMap(String name) {
        return getReg(regMap.getOrDefault(name, -1));
    }

    public List<String> storeToRegMap(String name) {
        if (regMap.containsKey(name)) {
            return new ArrayList<String>() {{
                add(getReg(regMap.get(name)));
            }};
        } else if (size < max) {
            regMap.put(name, size);
            return new ArrayList<String>() {{
                add(getReg(size++));
            }};
        } else {
            int spill = (int) (Math.random() * max);
            String s = "";
            for (Map.Entry<String, Integer> entry : regMap.entrySet()) {
                if (entry.getValue() == spill) {
                    s = entry.getKey();
                    regMap.remove(s);
                    break;
                }
            }
            regMap.put(name, spill);
            String finalS = s;
            return new ArrayList<String>() {{
                add(getReg(spill));
                add(finalS);
            }};
        }
    }

    private String removeValue(int value) {

        return null;
    }

    public void clear() {
        regMap.clear();
        size = 0;
    }
}
