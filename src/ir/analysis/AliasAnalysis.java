package ir.analysis;

import ir.IRModule;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.VoidType;
import ir.values.*;
import ir.values.instructions.ConstArray;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.*;
import ir.values.instructions.terminator.CallInst;
import utils.INode;

import java.util.*;
import java.util.stream.Collectors;

public class AliasAnalysis {
    // 得到Array的Value 有可能是GV的数组，Alloca的数组，参数的数组
    public static Value getArrayValue(Value pointer) {
        Value pt = pointer;
        while (pt instanceof GEPInst || pt instanceof LoadInst) {
            if (pt instanceof GEPInst) {
                pt = ((GEPInst) pt).getPointer();
            } else {
                pt = ((LoadInst) pt).getPointer();
            }
        }

        if (pt instanceof AllocaInst || pt instanceof GlobalVar) {
            if (pt instanceof AllocaInst && ((AllocaInst) pt).getAllocaType() instanceof PointerType) {
                for (Use use : pt.getUsesList()) {
                    if (use.getUser() instanceof StoreInst) {
                        pt = use.getUser().getOperands().get(1);
                    }
                }
            }
            return pt;
        } else {
            return null;
        }
    }

    public static boolean isGlobal(Value array) {
        return array instanceof GlobalVar;
    }

    public static boolean isParam(Value array) {
        if (array instanceof AllocaInst) {
            return ((AllocaInst) array).getAllocaType() instanceof PointerType;
        } else {
            return false;
        }
    }

    public static boolean isLocal(Value array) {
        return !isGlobal(array) && !isParam(array);
    }

    public static boolean alias(Value a1, Value a2) {
        if ((isGlobal(a1) && isGlobal(a2)) || (isParam(a1) && isParam(a2)) || (isLocal(a1) && isLocal(a2))) {
            return a1 == a2;
        }
        return (isGlobal(a1) && isParam(a2)) && ((GlobalVar) a1).getValue() instanceof ConstArray
                || (isGlobal(a2) && isParam(a1)) && ((GlobalVar) a2).getValue() instanceof ConstArray;
    }

    public static boolean callAlias(Value arr, CallInst callInst) {
        if (isParam(arr)) {
            return true;
        }

        // 如果arr是全局数组 同时函数中相关的全局变量中有这个arr，那么返回true
        if (isGlobal(arr) && relatedGVs.get(callInst.getCalledFunction()).contains(arr)) {
            return true;
        }

        for (Value arg : callInst.getOperands()) {
            if (arg instanceof GEPInst && alias(arr, getArrayValue(arg))) {
                return true;
            }
        }
        return false;
    }

    //在跑load store依赖关系时使用，存取所有function中用到的所有array的关系
    public static class PointerDefUses {
        private final Value pointer;
        // 所有load指令
        private final List<LoadInst> loads;
        // 所有def指令
        private final List<Instruction> defs;

        public PointerDefUses(Value pointer) {
            this.pointer = pointer;
            this.loads = new ArrayList<>();
            this.defs = new ArrayList<>();
        }

        public Value getPointer() {
            return this.pointer;
        }

        public List<LoadInst> getLoads() {
            return this.loads;
        }

        public List<Instruction> getDefs() {
            return this.defs;
        }
    }

    public static class RenameData {
        private final BasicBlock curBB;
        private final BasicBlock pred;
        private final Map<Value, Value> values;

        public RenameData(BasicBlock curBB, BasicBlock pred, Map<Value, Value> values) {
            this.curBB = curBB;
            this.pred = pred;
            this.values = values;
        }

        public BasicBlock getCurBB() {
            return curBB;
        }

        public BasicBlock getPredBB() {
            return pred;
        }

        public Map<Value, Value> getValues() {
            return values;
        }
    }

    public static void runLoadDependStore(Function function) {
        // pointer 对应的def块
        Map<Value, List<BasicBlock>> defBlocks = new HashMap<>();
        // 初始化
        function.getList().forEach(bbNode -> {
            BasicBlock bb = bbNode.getValue();
            for (INode<Instruction, BasicBlock> instNode : bb.getInstructions()) {
                Instruction inst = instNode.getValue();
                // 局部变量不需要分析
                if (inst instanceof LoadInst) {
                    if (((LoadInst) inst).getPointer() instanceof AllocaInst) {
                        if (((AllocaInst) ((LoadInst) inst).getPointer()).getAllocaType() instanceof IntegerType) {
                            continue;
                        }
                    }

                    Value pointer = getArrayValue(((LoadInst) inst).getPointer());
                    if (!pointerDUMap.containsKey(pointer)) {
                        PointerDefUses newPDF = new PointerDefUses(pointer);
                        pointerDUMap.put(pointer, newPDF);
                        // defBlock
                        defBlocks.put(pointer, new ArrayList<>());
                    }
                    pointerDUMap.get(pointer).getLoads().add((LoadInst) inst);
                }
            }
        });
        // 将store和call指令存进去
        pointerDUMap.values().forEach(pdu -> {
            Value pointer = pdu.getPointer();

            function.getList().forEach(bbNode -> {
                BasicBlock bb = bbNode.getValue();

                bb.getInstructions().forEach(instNode -> {
                    Instruction inst = instNode.getValue();
                    if (inst instanceof StoreInst) {
                        if (alias(pointer, getArrayValue(((StoreInst) inst).getPointer()))) {
                            pdu.getDefs().add(inst);
                            // defBlocks
                            defBlocks.get(pointer).add(bb);
                            aliasStore.add((StoreInst) inst);
                        }
                    } else if (inst instanceof CallInst) {
                        Function func = ((CallInst) inst).getCalledFunction();
                        if (func.hasSideEffect() && callAlias(pointer, (CallInst) inst)) {
                            pdu.getDefs().add(inst);
                            // defBlocks
                            defBlocks.get(pointer).add(bb);
                            aliasCall.add((CallInst) inst);
                        }
                    }
                });
            });
        });

        // MemPhi
        Queue<BasicBlock> w = new LinkedList<>();
        Map<MemPhi, Value> memPhi2Pointer = new HashMap<>();

        pointerDUMap.values().forEach(pdu -> {
            Value pointer = pdu.getPointer();
            w.addAll(defBlocks.get(pointer));
            Set<BasicBlock> dirty = new HashSet<>();

            while (!w.isEmpty()) {
                BasicBlock bb = w.poll();
                for (BasicBlock domF : df.get(bb)) {
                    if (!dirty.contains(domF)) {
                        dirty.add(domF);
                        // 插入一个空phi指令
                        MemPhi memPhi = new MemPhi(domF, pointer, domF.getPredecessors().size());
                        memPhi2Pointer.put(memPhi, pointer);
                        if (!defBlocks.get(pointer).contains(domF)) {
                            w.add(domF);
                        }
                    }
                }
            }
        });

        // pointer -> curValue
        Map<Value, Value> values = new HashMap<>();
        for (Value i : pointerDUMap.keySet()) {
            values.put(i, new NullValue());
        }
        Set<BasicBlock> dirty = new HashSet<>();

        // rename
        Stack<RenameData> renameStack = new Stack<>();
        renameStack.push(new RenameData(function.getList().getBegin().getValue(), null, values));
        while (!renameStack.isEmpty()) {
            RenameData data = renameStack.pop();
            Map<Value, Value> curValues = new HashMap<>(data.values);
            // MemPhi
            for (INode<Instruction, BasicBlock> instNode : data.getCurBB().getInstructions()) {
                if (instNode.getValue() instanceof MemPhi) {
                    MemPhi memPhi = (MemPhi) instNode.getValue();
                    memPhi.setOperand(data.getCurBB().getPredecessors().indexOf(data.getPredBB()),
                            data.getValues().get(memPhi2Pointer.get(memPhi)));
                } else {
                    break;
                }
            }

            if (!dirty.contains(data.getCurBB())) {
                dirty.add(data.getCurBB());

                for (INode<Instruction, BasicBlock> instNode : data.getCurBB().getInstructions()) {
                    Instruction inst = instNode.getValue();
                    if (inst instanceof MemPhi) {
                        Value pointer = memPhi2Pointer.get(inst);
                        curValues.replace(pointer, inst);
                    } else if (inst instanceof StoreInst || inst instanceof CallInst) {
                        Value pointer;
                        for (PointerDefUses pdf : pointerDUMap.values()) {
                            if (pdf.getDefs().contains(inst)) {
                                pointer = pdf.getPointer();
                                if (pointer != null) {
                                    curValues.replace(pointer, inst);
                                }
                            }
                        }
                    } else if (inst instanceof LoadInst) {
                        Value pointer = getArrayValue(((LoadInst) inst).getPointer());
                        if (pointer == null) {
                            continue;
                        }
                        inst.addOperand(curValues.get(pointer));
                    }
                }

                data.getCurBB().getSuccessors().forEach(bb -> renameStack.push(new RenameData(bb, data.getCurBB(), curValues)));
            }
        }
    }

    public static void runStoreDependLoad(Function function) {
        List<LoadInst> loads = new ArrayList<>();
        // load指令memPhi处理
        Queue<BasicBlock> w = new LinkedList<>();
        Map<MemPhi, Value> memPhiToLoad = new HashMap<>();
        pointerDUMap.values().forEach(pdu -> pdu.getLoads().forEach(loadInst -> {
            loads.add(loadInst);

            Set<BasicBlock> dirty = new HashSet<>();
            w.add(loadInst.getParent());

            while (!w.isEmpty()) {
                BasicBlock bb = w.poll();
                for (BasicBlock domF : df.get(bb)) {
                    if (!dirty.contains(domF)) {
                        dirty.add(domF);
                        MemPhi memPhi = new MemPhi(domF, new NullValue(), domF.getPredecessors().size());
                        memPhiToLoad.put(memPhi, loadInst);
                        w.add(domF);
                    }
                }
            }
        }));

        // construct loadDepInst
        Map<Value, Value> values = new HashMap<>();
        for (LoadInst i : loads) {
            values.put(i, new NullValue());
        }

        Set<BasicBlock> dirty = new HashSet<>();
        // 遍历
        Stack<RenameData> renameStack = new Stack<>();
        renameStack.push(new RenameData(function.getList().getBegin().getValue(), null, values));
        while (!renameStack.isEmpty()) {
            RenameData data = renameStack.pop();
            Map<Value, Value> curValue = new HashMap<>(data.getValues());

            // mem-phi update incoming values
            for (INode<Instruction, BasicBlock> instNode : data.getCurBB().getInstructions()) {
                if (instNode.getValue() instanceof MemPhi) {
                    MemPhi memPhi = (MemPhi) instNode.getValue();
                    Value loadInst = memPhiToLoad.get(memPhi);
                    if (loadInst != null) {
                        memPhi.setOperand(data.getCurBB().getPredecessors().indexOf(data.getPredBB()),
                                data.getValues().get(loadInst));
                    }
                } else {
                    break;
                }
            }

            if (!dirty.contains(data.getCurBB())) {
                dirty.add(data.getCurBB());

                for (INode<Instruction, BasicBlock> instNode = data.getCurBB().getInstructions().getBegin(); instNode != null; ) {
                    INode<Instruction, BasicBlock> tmp = instNode.getNext();
                    Instruction inst = instNode.getValue();
                    if (inst instanceof MemPhi) {
                        Value loadInst = memPhiToLoad.get(inst);
                        if (loadInst != null) {
                            curValue.replace(loadInst, inst);
                        }
                    } else if (inst instanceof LoadInst) {
                        curValue.replace(inst, inst);
                    } else if ((inst instanceof StoreInst && aliasStore.contains(inst)) ||
                            (inst instanceof CallInst && aliasCall.contains(inst))) {
                        for (Value memInst : curValue.values()) {
                            if (!(memInst instanceof NullValue)) {
                                new LoadDepInst(inst, (Instruction) memInst);
                            }
                        }
                    }
                    instNode = tmp;
                }

                data.getCurBB().getSuccessors().forEach(bb -> renameStack.push(new RenameData(bb, data.getCurBB(), curValue)));
            }
        }

        while (true) {
            boolean clear = true;
            for (INode<BasicBlock, Function> bbNode : function.getList()) {
                BasicBlock bb = bbNode.getValue();
                for (INode<Instruction, BasicBlock> instNode = bb.getInstructions().getBegin(); instNode != null; ) {
                    INode<Instruction, BasicBlock> tmp = instNode.getNext();
                    Instruction inst = instNode.getValue();

                    if (!(inst instanceof MemPhi)) {
                        break;
                    }
                    if (inst.getUsesList().isEmpty() || inst.getUsesList().get(0) == null) {
                        inst.getNode().removeFromList();
                        inst.removeUseFromOperands();
                        clear = false;
                    }

                    instNode = tmp;
                }
            }
            if (clear) {
                break;
            }
        }
    }

    // mem指令的def-use限制
    private static Map<BasicBlock, Set<BasicBlock>> df;
    private static Set<StoreInst> aliasStore;
    private static Set<CallInst> aliasCall;
    private static Map<Value, PointerDefUses> pointerDUMap = new HashMap<>();

    private static Set<Function> visitFunc = new HashSet<>();
    private static Map<GlobalVar, List<Function>> gvUserFunc = new HashMap<>();
    private static Map<Function, Set<GlobalVar>> relatedGVs = new HashMap<>();


    public static void run(Function function) {
        df = DomAnalysis.analyzeDom(function);
        aliasStore = new HashSet<>();
        aliasCall = new HashSet<>();
        pointerDUMap = new HashMap<>();

        gvUserFunc = new HashMap<>();
        visitFunc = new HashSet<>();
        relatedGVs = new HashMap<>();
        loadUserFuncs();
        findRelatedFunc();

        runLoadDependStore(function);
        runStoreDependLoad(function);
    }

    public static void clear(Function function) {
        function.getList().forEach(bbNode -> bbNode.getValue().getInstructions().forEach(instNode -> {
            Instruction inst = instNode.getValue();
            if (inst instanceof MemPhi ||
                    inst instanceof LoadDepInst) {
                inst.replaceUsedWith(null);
                inst.removeUseFromOperands();
            } else if (inst instanceof LoadInst) {
                if (inst.getOperands().size() == 2) {
                    inst.replaceOperands(1, null);
                    inst.getOperands().remove(null);
                }
            }
        }));

        function.getList().forEach(bbNode -> {
            BasicBlock bb = bbNode.getValue();
            INode<Instruction, BasicBlock> tmp = bb.getInstructions().getBegin();
            while (tmp != null) {
                INode<Instruction, BasicBlock> next = tmp.getNext();
                Instruction inst = tmp.getValue();
                if (inst instanceof MemPhi || inst instanceof LoadDepInst) {
                    tmp.removeFromList();
                }
                tmp = next;
            }
        });
    }


    private static void loadUserFuncs() {
        for (GlobalVar gv : IRModule.getInstance().getGlobalVars()) {
            List<Function> parents = new ArrayList<>();
            for (Use use : gv.getUsesList()) {
                Function func = ((Instruction) use.getUser()).getParent().getParent();
                if (!(func.getPredecessors().isEmpty() && !func.getName().equals("main"))) {
                    parents.add(((Instruction) use.getUser()).getParent().getParent());
                }
            }
            parents = parents.stream().distinct().collect(Collectors.toList());
            gvUserFunc.put(gv, parents);
        }
    }


    private static void findRelatedFunc() {
        IRModule.getInstance().getFunctions().forEach(functionNode -> {
            Function function = functionNode.getValue();
            relatedGVs.put(function, new HashSet<>());
            IRModule.getInstance().getGlobalVars().forEach(gv -> {
                visitFunc.clear();
                if (bfsFuncs(function, gv)) {
                    relatedGVs.get(function).add(gv);
                }
            });
        });
    }

    private static boolean bfsFuncs(Function start, GlobalVar gv) {
        if (visitFunc.contains(start))
            return false;
        visitFunc.add(start);
        if (gvUserFunc.get(gv).contains(start))
            return true;
        boolean result = false;
        for (Function callee : start.getSuccessors()) {
            result |= bfsFuncs(callee, gv);
        }
        return result;
    }

    public static class MemPhi extends MemInst {
        public MemPhi(BasicBlock bb, Value pointer, int size) {
            super(VoidType.voidType, Operator.MemPhi, bb);
            this.addOperand(pointer);
            for (int i = 0; i < size; i++) {
                this.addOperand(new NullValue());
            }
            this.getNode().insertAtBegin(bb.getInstructions());
        }

        public void setOperand(int index, Value value) {
            this.replaceOperands(index + 1, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("memPhi: ");
            sb.append(getOperands().get(0));
            for (int i = 1; i < getOperands().size(); i++) {
                sb.append(" [ ");
                sb.append(getOperands().get(i).getName());
                sb.append(", %");
                sb.append(getParent().getPredecessors().get(i - 1).getName());
                sb.append("] ");
            }
            return sb.toString();
        }
    }

    public static class LoadDepInst extends MemInst {
        public LoadDepInst(Instruction insertInst, Instruction dependInstruction) {
            super(VoidType.voidType, Operator.LoadDep, insertInst.getParent());
            this.addOperand(dependInstruction);
            this.getNode().insertBefore(insertInst.getNode());
        }

        @Override
        public String toString() {
            return "loadDep: " + this.getOperands().get(0).getName();
        }
    }
}
