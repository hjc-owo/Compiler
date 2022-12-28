package ir.values;

import ir.IRModule;
import ir.analysis.LoopInfo;
import ir.types.FunctionType;
import ir.types.Type;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.PhiInst;
import utils.IList;
import utils.INode;

import java.util.*;

public class Function extends Value {
    private final IList<BasicBlock, Function> list;
    private final INode<Function, IRModule> node;
    private final List<Argument> arguments;
    private final List<Function> predecessors;
    private final List<Function> successors;
    private final boolean isLibraryFunction;
    // 过程间分析
    private boolean hasSideEffect = false;
    private boolean useGlobalVar = false;
    private final Set<GlobalVar> storeGVs = new HashSet<>();
    private final Set<GlobalVar> loadGVs = new HashSet<>();
    // 函数内联
    private boolean successorsNotAllLibrary = false;
    private Map<BasicBlock, BasicBlock> idom = new HashMap<>();
    private Map<BasicBlock, Set<BasicBlock>> dom = new HashMap<>();
    private Map<BasicBlock, List<BasicBlock>> idoms = new HashMap<>();
    private final LoopInfo loopInfo = new LoopInfo(this);

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
        if (!successor.isLibraryFunction) {
            successorsNotAllLibrary = true;
        }
    }

    public boolean isLibraryFunction() {
        return isLibraryFunction;
    }

    public boolean hasSideEffect() {
        return hasSideEffect || isLibraryFunction;
    }

    public void setHasSideEffect(boolean hasSideEffect) {
        this.hasSideEffect = hasSideEffect;
    }

    public boolean isUseGlobalVar() {
        return useGlobalVar;
    }

    public void setUseGlobalVar(boolean useGlobalVar) {
        this.useGlobalVar = useGlobalVar;
    }

    public Set<GlobalVar> getStoreGVs() {
        return storeGVs;
    }

    public Set<GlobalVar> getLoadGVs() {
        return loadGVs;
    }

    public boolean isSuccessorsNotAllLibrary() {
        return successorsNotAllLibrary;
    }

    public Map<BasicBlock, BasicBlock> getIdom() {
        return idom;
    }

    public void setIdom(Map<BasicBlock, BasicBlock> idom) {
        this.idom = idom;
    }

    public Map<BasicBlock, Set<BasicBlock>> getDom() {
        return dom;
    }

    public void setDom(Map<BasicBlock, Set<BasicBlock>> dom) {
        this.dom = dom;
    }

    public Map<BasicBlock, List<BasicBlock>> getIdoms() {
        return idoms;
    }

    public void setIdoms(Map<BasicBlock, List<BasicBlock>> idoms) {
        this.idoms = idoms;
    }

    public LoopInfo getLoopInfo() {
        return loopInfo;
    }

    public void copyAllFrom(Function sourceFunction) {
        Map<Value, Value> replaceMap = new HashMap<>();
        Set<BasicBlock> visitedMap = new HashSet<>();
        IRModule irModule = sourceFunction.getNode().getParent().getValue();
        List<Value> sourceArgs = sourceFunction.getArguments();
        // initial
        for (int i = 0; i < sourceArgs.size(); i++) {
            replaceMap.put(sourceArgs.get(i), arguments.get(i));
        }
        for (GlobalVar gv : irModule.getGlobalVars()) {
            replaceMap.put(gv, gv);
        }
        for (INode<BasicBlock, Function> basicBlockINode : sourceFunction.getList()) {
            BasicBlock basicBlock = basicBlockINode.getValue();
            replaceMap.put(basicBlock, BuildFactory.getInstance().buildBasicBlock(this));
        }
        Stack<BasicBlock> dfsStack = new Stack<>();
        dfsStack.push(sourceFunction.getList().getBegin().getValue());
        List<PhiInst> phiArrayList = new ArrayList<>();
        while (!dfsStack.isEmpty()) {
            BasicBlock loopBlock = dfsStack.pop();
            ((BasicBlock) replaceMap.get(loopBlock)).copyAllFrom(loopBlock, replaceMap);
            if (!loopBlock.getSuccessors().isEmpty()) {
                for (BasicBlock basicBlock : new HashSet<>(loopBlock.getSuccessors())) {
                    if (!visitedMap.contains(basicBlock)) {
                        visitedMap.add(basicBlock);
                        dfsStack.push(basicBlock);
                    }
                }
            }
        }
        for (INode<BasicBlock, Function> basicBlockINode : sourceFunction.getList()) {
            BasicBlock basicBlock = basicBlockINode.getValue();
            for (INode<Instruction, BasicBlock> instNode : basicBlock.getInstructions()) {
                Instruction instruction = instNode.getValue();
                if (instruction instanceof PhiInst) {
                    phiArrayList.add((PhiInst) instruction);
                }
            }
        }

        for (PhiInst phi : phiArrayList) {
            for (int i = 0; i < phi.getOperands().size(); i++) {
                BasicBlock preBB = phi.getParent().getPredecessors().get(i);
                BasicBlock nowBB = (BasicBlock) replaceMap.get(preBB);
                int index = ((PhiInst) replaceMap.get(phi)).getParent().getPredecessors().indexOf(nowBB);
                ((PhiInst) replaceMap.get(phi)).replaceOperands(index, phi.findValue(replaceMap, phi.getOperands().get(i)));
            }
        }
    }

    public void computeSimpLoopInfo() {
        loopInfo.computeLoopInfo(this);
    }

    public void computeLoopInfo() {
        loopInfo.run();
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
