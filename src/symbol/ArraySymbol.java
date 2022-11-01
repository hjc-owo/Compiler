package symbol;

public class ArraySymbol extends Symbol {

    private boolean isConst; // 是否是常量
    private int dimension; // 0 变量，1 数组，2 二维数组

    public ArraySymbol(String name, boolean isConst, int dimension) { // 数组
        super(name);
        this.isConst = isConst;
        this.dimension = dimension;
    }

    public boolean isConst() {
        return isConst;
    }

    public int getDimension() {
        return dimension;
    }
}
