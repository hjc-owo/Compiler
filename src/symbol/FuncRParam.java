package symbol;

public class FuncRParam {

    private String name; // 实参名称
    private int dimension; // 0 变量，1 数组，2 二维数组

    public FuncRParam(String name, int dimension) {
        this.name = name;
        this.dimension = dimension;
    }

    public String getName() {
        return name;
    }

    public int getDimension() {
        return dimension;
    }

}
