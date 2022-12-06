package ir.values;

/**
 * 维护了这条边的两个结点以及使用和被使用的关系
 * 从 User 能够通过 OperandList 找到这个 User 使用的 Value
 * 从 Value 也能找到对应的使用这个 Value 的 User
 */
public class Use {
    private User user; // 使用这个 Use 的 Value
    private Value value; // 这个 Use 使用的 Value
    private int posOfOperand; // 在 OperandList 中的位置

    public Use(Value value, User user, int posOfOperand) {
        this.user = user;
        this.value = value;
        this.posOfOperand = posOfOperand;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public int getPosOfOperand() {
        return posOfOperand;
    }

    public void setPosOfOperand(int posOfOperand) {
        this.posOfOperand = posOfOperand;
    }
}
