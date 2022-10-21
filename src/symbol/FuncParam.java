package symbol;

public class FuncParam { // 函数参数
    private String name; // 参数名称
    private int dimension; // 0 变量，1 数组，2 二维数组

    public FuncParam(String name, int dimension) {
        this.name = name;
        this.dimension = dimension;
    }

    public String getName() {
        return name;
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public String toString() {
        return "FuncParam{" +
                "name='" + name + '\'' +
                ", dimension=" + dimension +
                '}';
    }
}
