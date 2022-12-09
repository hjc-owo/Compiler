package ir.values;

import ir.IRModule;
import ir.types.FunctionType;
import ir.types.Type;
import utils.IList;
import utils.INode;

import java.util.ArrayList;
import java.util.List;

public class Function extends Value {
    private final IList<BasicBlock, Function> list;
    private final INode<Function, IRModule> node;
    private final List<Argument> arguments;
    private final List<Function> predecessors;
    private final List<Function> successors;
    private final boolean isLibraryFunction;

    public Function(String name, Type type, boolean isLibraryFunction) {
        super(name, type);
        REG_NUMBER = 0;
        this.list = new IList<>(this);
        this.node = new INode<>(this);
        this.arguments = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.isLibraryFunction = isLibraryFunction;
        for (Type t : ((FunctionType) type).getParametersType()) {
            arguments.add(new Argument(t, ((FunctionType) type).getParametersType().indexOf(t), isLibraryFunction));
        }
        this.node.insertAtEnd(IRModule.getInstance().getFunctions());
    }

    public IList<BasicBlock, Function> getList() {
        return list;
    }

    public INode<Function, IRModule> getNode() {
        return node;
    }

    public List<Value> getArguments() {
        return new ArrayList<>(arguments);
    }

    public List<Function> getPredecessors() {
        return predecessors;
    }

    public void addPredecessor(Function predecessor) {
        this.predecessors.add(predecessor);
    }

    public List<Function> getSuccessors() {
        return successors;
    }

    public void addSuccessor(Function successor) {
        this.successors.add(successor);
    }

    public boolean isLibraryFunction() {
        return isLibraryFunction;
    }

    public void refreshArgReg() {
        for (Argument arg : arguments) {
            arg.setName("%" + REG_NUMBER++);
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(((FunctionType) this.getType()).getReturnType()).append(" @").append(this.getName()).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            s.append(arguments.get(i).getType());
            if (i != arguments.size() - 1) {
                s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }

    public static class Argument extends Value {
        private int index;

        public Argument(String name, Type type, int index) {
            super(name, type);
            this.index = index;
        }

        public Argument(Type type, int index, boolean isLibraryFunction) {
            super(isLibraryFunction ? "" : "%" + REG_NUMBER++, type);
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return this.getType().toString() + " " + this.getName();
        }

    }
}
