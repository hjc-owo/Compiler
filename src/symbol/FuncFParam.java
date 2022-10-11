package symbol;

import java.util.ArrayList;
import java.util.List;

public class FuncFParam { // 函数形参
    private String name; // 形参名称
    private int dimension; // 0 变量，1 数组，2 二维数组
    private List<Integer> dimLengths = new ArrayList<>(3); // 每一维数组长度

    public FuncFParam(String name, int dimension) {
        this.name = name;
        this.dimension = dimension;
    }

    public FuncFParam(String name, int dimension, List<Integer> dimLengths) {
        this.name = name;
        this.dimension = dimension;
        this.dimLengths = dimLengths;
    }

    public String getName() {
        return name;
    }

    public int getDimension() {
        return dimension;
    }

    public List<Integer> getDimLengths() {
        return dimLengths;
    }

    @Override
    public String toString() {
        return "FuncFParam{" +
                "name='" + name + '\'' +
                ", dimension=" + dimension +
                ", dimLengths=" + dimLengths +
                '}';
    }
}
