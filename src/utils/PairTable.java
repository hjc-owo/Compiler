package utils;

import java.util.ArrayList;
import java.util.List;

public class PairTable<First, Second> {
    private List<Pair<First, Second>> pairs;

    public PairTable() {
        this.pairs = new ArrayList<>();
    }

    public boolean containsKey(First first) {
        for (Pair<First, Second> pair : pairs) {
            if (pair.getFirst().equals(first))
                return true;
        }
        return false;
    }

    public Second get(First first) {
        for (Pair<First, Second> pair : pairs) {
            if (pair.getFirst().equals(first)) {
                return pair.getSecond();
            }
        }
        return null;
    }

    public First getKey(int i) {
        if (i < size()) {
            return pairs.get(i).getFirst();
        }
        return null;
    }

    public void put(First first, Second second) {
        this.pairs.add(new Pair<>(first, second));
    }

    public int size() {
        return this.pairs.size();
    }

    public void clear() {
        this.pairs = new ArrayList<>();
    }

    public void replace(First first, Second second) {
        boolean replaced = false;
        for (Pair<First, Second> pair : pairs) {
            if (pair.getFirst().equals(first)) {
                pair.setSecond(second);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            put(first, second);
        }
    }
}
