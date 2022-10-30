package ir.types;

import java.util.ArrayList;
import java.util.List;

public class FunctionType implements Type {
    private List<Type> parametersType;
    private Type returnType;

    public FunctionType() {
        this.parametersType = new ArrayList<>();
    }

    public FunctionType(Type returnType) {
        this.parametersType = new ArrayList<>();
        this.returnType = returnType;
        arrayTypeNoLength();
    }

    public FunctionType(Type returnType, List<Type> parametersType) {
        this.returnType = returnType;
        this.parametersType = parametersType;
        arrayTypeNoLength();
    }

    public List<Type> getParametersType() {
        return parametersType;
    }

    public void setParametersType(List<Type> parametersType) {
        this.parametersType = parametersType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    private void arrayTypeNoLength() {
        List<Integer> target = new ArrayList<>();
        for (Type type : parametersType) {
            if (type instanceof ArrayType) {
                if (((ArrayType) type).getLength() == -1) {
                    target.add(parametersType.indexOf(type));
                }
            }
        }
        for (int index : target) {
            parametersType.set(index, new PointerType(((ArrayType) parametersType.get(index)).getElementType()));
        }
    }
}
