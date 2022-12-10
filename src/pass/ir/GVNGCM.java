package pass.ir;

import ir.IRModule;
import ir.analysis.AliasAnalysis;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.VoidType;
import ir.values.*;
import ir.values.instructions.*;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.PhiInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.CallInst;
import pass.Pass;
import utils.INode;
import utils.PairTable;

import java.util.*;

public class GVNGCM implements Pass.IRPass {
    private final PairTable<Value, Value> valueTable = new PairTable<>();

    @Override
    public String getName() {
        return "gvngcm";
    }

    @Override
    public void run(IRModule m) {
        InterProceduralAnalysis ipa = new InterProceduralAnalysis();
        ipa.run(m);
        m.getFunctions().forEach(functionNode -> {
            Function function = functionNode.getValue();
            if (!function.isLibraryFunction()) {
                BranchOptimization bo = new BranchOptimization();
                do {
                    DeadCodeElimination dce = new DeadCodeElimination();
                    dce.dce(function);

                    AliasAnalysis.run(function);
                    runGVN(function);
                    // SimplifyInstruction有点bug 需要refresh useList链中不存在的user
                    refreshUseList(function);
                    AliasAnalysis.clear(function);

                    dce = new DeadCodeElimination();
                    dce.dce(function);

                    AliasAnalysis.run(function);
                    runGCM(function);
                    AliasAnalysis.clear(function);
                } while (bo.compute(function));
            }
        });
    }

    private void refreshUseList(Function function) {
        for (INode<BasicBlock, Function> bbNode : function.getList()) {
            for (INode<Instruction, BasicBlock> instNode : bbNode.getValue().getInstructions()) {
                Instruction instruction = instNode.getValue();
                instruction.getUsesList().removeIf(use -> ((Instruction) use.getUser()).getNode().getParent() == null);
            }
        }
    }

    // 逆后序遍历 basicBlock
    private void runGVN(Function function) {
        // 清空 value 表
        valueTable.clear();
        // get RPO
        List<BasicBlock> rpo = new ArrayList<>();
        Stack<BasicBlock> stack = new Stack<>();
        stack.push(function.getList().getBegin().getValue());
        while (!stack.isEmpty()) {
            BasicBlock tmp = stack.pop();
            rpo.add(tmp);
            for (BasicBlock i : tmp.getSuccessors()) {
                if (!rpo.contains(i) && !stack.contains(i)) {
                    stack.push(i);
                }
            }
        }

        // 遍历
        rpo.forEach(this::runGVNBasicBlock);
    }

    /*
    1、删除所有相同的 phi 指令
    2、遍历BB中的每一条指令
     */
    private void runGVNBasicBlock(BasicBlock basicBlock) {
        //1、删除相同 phi 指令
//        int predSize = basicBlock.getPredecessors().size();
//        for (INode<Instruction, BasicBlock> instNode : basicBlock.getInstructions()) {
//            Instruction inst = instNode.getValue();
//            if (inst instanceof PhiInst) {
//                INode<Instruction, BasicBlock> nextNode = instNode.getNext();
//                while (nextNode != null && (nextNode.getValue() instanceof PhiInst)) {
//                    Instruction next = nextNode.getValue();
//                    boolean same = true;
//                    for (int num = 0; num < predSize; num++) {
//                        if (!inst.getOperands().get(num).equals(next.getOperands().get(num))) {
//                            same = false;
//                            break;
//                        }
//                    }
//                    if (same) // 如果相同，则删除next指令
//                        replace(inst, next, true);
//                    nextNode = nextNode.getNext();
//                }
//            } else if (inst instanceof AliasAnalysis.MemPhi) {
//            } else {
//                break;
//            }
//        }

        INode<Instruction, BasicBlock> instNode = basicBlock.getInstructions().getBegin();
        while (!instNode.equals(basicBlock.getInstructions().getEnd())) {
            INode<Instruction, BasicBlock> nextNode = instNode.getNext();
            runGVNInstruction(instNode.getValue());
            instNode = nextNode;
        }
    }

    private void replace(Instruction instruction, Value simpleInst, boolean force) {
        if (instruction.equals(simpleInst)) {
            return;
        }
        if (simpleInst instanceof Instruction && (force || ((Instruction) simpleInst).getNode().getParent() == null)) {
            if (force) replaceWithValue(instruction, simpleInst);
            else replaceWithInst(instruction, (Instruction) simpleInst);
        } else {
            replaceWithValue(instruction, simpleInst);
        }
    }

    private void replaceWithInst(Instruction curInst, Instruction finalInst) {
        finalInst.getNode().insertBefore(curInst.getNode());
        curInst.removeUseFromOperands();
        curInst.replaceUsedWith(finalInst);
        curInst.getNode().removeFromList();
    }

    private void replaceWithValue(Instruction curInst, Value finalValue) {
        curInst.removeUseFromOperands();
        curInst.replaceUsedWith(finalValue);
        curInst.getNode().removeFromList();
    }

    public void checkForReplace(Instruction instruction, Value value) {
        if (!value.equals(instruction)) {
            replace(instruction, value, true);
        }
    }

    private Value findAdd(Value value) {
        if (valueTable.containsKey(value)) {
            return valueTable.get(value);
        }
        valueTable.put(value, value);
        if (value instanceof Instruction) {
            Value findValue = findValue((Instruction) value);
            if (findValue != null)
                valueTable.replace(value, findValue);
        }
        return valueTable.get(value);
    }

    private Value findValue(Instruction instruction) {
        if (instruction instanceof BinaryInst)
            return findValue((BinaryInst) instruction);
        if (instruction instanceof GEPInst)
            return findValue((GEPInst) instruction);
        if (instruction instanceof LoadInst)
            return findValue((LoadInst) instruction);
        if (instruction instanceof CallInst && ((CallInst) instruction).isPure())
            return findValue((CallInst) instruction);
        return null;
    }

    /*
        1、将左右操作数都向上findAdd
        2、遍历变量表，如果有key是binaryInst但是不是本条指令，则进行判断：
            如果两个指令的操作数均相同，则可认为是同一个操作，那么就返回查询到的binaryInst的value
     */
    private Value findValue(BinaryInst binaryInst) {
        Value left = findAdd(binaryInst.getOperands().get(0));
        Value right = findAdd(binaryInst.getOperands().get(1));
        int i = 0;
        while (i < valueTable.size()) {
            Value key = valueTable.getKey(i);
            if (key instanceof BinaryInst && !key.equals(binaryInst)) {
                Value left2 = findAdd(((BinaryInst) key).getOperands().get(0));
                Value right2 = findAdd(((BinaryInst) key).getOperands().get(1));
                boolean llrr = left.equals(left2) && right.equals(right2);
                boolean lrrl = left.equals(right2) && right.equals(left2);
                if (binaryInst.getOperator() == ((BinaryInst) key).getOperator()) {
                    if (binaryInst.getOperator() == Operator.Add ||
                            binaryInst.getOperator() == Operator.Mul ||
                            binaryInst.getOperator() == Operator.Ne ||
                            binaryInst.getOperator() == Operator.Eq) {
                        if (llrr || lrrl) {
                            return findAdd(key);
                        }
                    } else {
                        if (llrr) {
                            return findAdd(key);
                        }
                    }
                } else if (binaryInst.canReverse(binaryInst.getOperator(), ((BinaryInst) key).getOperator())) {
                    if (lrrl) {
                        return findAdd(key);
                    }
                }
            }
            i++;
        }
        return binaryInst;
    }

    /*
        1、同binaryInst的findValue
        2、遍历变量表，如果有key是gepInst但是不是本条指令，则进行判断：
            如果两个指令的操作数均相同，则可认为是同一个操作
     */
    private Value findValue(GEPInst gepInst) {
        int i = 0;
        while (i < valueTable.size()) {
            Value key = valueTable.getKey(i);
            if (key instanceof GEPInst && !key.equals(gepInst)) {
                Value thisPointer = findAdd(gepInst.getOperands().get(0));
                Value keyPointer = findAdd(((GEPInst) key).getOperands().get(0));
                if (thisPointer.equals(keyPointer) && gepInst.getOperands().size() == ((GEPInst) key).getOperands().size()) {
                    boolean isSame = true;
                    for (int j = 1; j < gepInst.getOperands().size(); j++) {
                        Value thisOperand = findAdd(gepInst.getOperands().get(j));
                        Value keyOperand = findAdd(((GEPInst) key).getOperands().get(j));
                        if (!thisOperand.equals(keyOperand)) {
                            isSame = false;
                            break;
                        }
                    }
                    if (isSame) {
                        return findAdd(key);
                    }
                }
            }
            i++;
        }
        return gepInst;
    }

    /*
        1、同其他findValue
        2、查询相同load指令
        3、查询相同的load指令之前的store指令
     */
    private Value findValue(LoadInst loadInst) {
        int i = 0;
        while (i < valueTable.size()) {
            Value key = valueTable.getKey(i);
            if (key instanceof LoadInst &&
                    !key.equals(loadInst)) {
                Value thisPointer = findAdd(loadInst.getOperands().get(0));
                Value keyPointer = findAdd(((LoadInst) key).getOperands().get(0));
                if (thisPointer.equals(keyPointer) && loadInst.getOperands().get(1) != null &&
                        loadInst.getOperands().get(1).equals(((LoadInst) key).getOperands().get(1))) {
                    return findAdd(key);
                }
            }
            /*
            store指令
            需要分析store和load指令的关系
             */
            if (key instanceof StoreInst) {
                Value thisPointer = findAdd(loadInst.getPointer());
                Value keyPointer = findAdd(((StoreInst) key).getPointer());
                if (thisPointer.equals(keyPointer) && loadInst.getOperands().get(1) != null &&
                        loadInst.getOperands().get(1).equals(key)) {
                    return ((StoreInst) key).getValue();
                }
            }
            i++;
        }
        return loadInst;
    }

    /*
        1、同其他findValue
        2、查询相同的call指令
     */
    private Value findValue(CallInst callInst) {
        int i = 0;
        while (i < valueTable.size()) {
            Value key = valueTable.getKey(i);
            if (key instanceof CallInst &&
                    !key.equals(callInst)) {
                Value thisFunc = findAdd(callInst.getOperands().get(0));
                Value keyFunc = findAdd(((CallInst) key).getOperands().get(0));
                if (thisFunc.equals(keyFunc)) {
                    boolean same = true;
                    for (int j = 1; j < callInst.getOperands().size(); j++) {
                        Value thisArg = findAdd(callInst.getOperands().get(j));
                        Value keyArg = findAdd(((CallInst) key).getOperands().get(j));
                        if (!thisArg.equals(keyArg)) {
                            same = false;
                            break;
                        }
                    }
                    if (same) {
                        return findAdd(key);
                    }
                }
            }
            i++;
        }
        return callInst;
    }

    /*
        1、如果没有 uses，且不是 store 和 call(void) 不处理
        2、简化指令 //这一步在常量折叠中做过了，再做一遍也可以
        3、binary指令：（不更改cmp指令）
            1、向上查找并替换或者添加该指令的键值对
        4、gep指令：
            1、向上查找并替换或者添加该指令的键值对
        5、load指令：
            1、找到最上层的pointer或者是array
            2、如果pointer是gep并且array是全局数组：
                1、如果是const类型的数组：
                    如果constArray没有东西（即初始化为0）那么直接替换成0
                    如果constArray里面有东西，将其中的值取出来替换掉
                2、如果不是const类型的数组：
                    向上查找并替换或者添加该指令的键值对
        6、phi指令：
            1、如果传进来的值都一样，那么就进行优化替换
        7、store指令：
            1、不能替换掉指针存入指针的情况
            2、将键值对加到变量表中
        8、call指令：
            如果call指令是pureCall那么：
                向上查找并替换或者添加该指令的键值对
     */
    private void runGVNInstruction(Instruction instruction) {
        if (instruction.getUsesList().size() == 0) {
            if (!(instruction instanceof StoreInst)) {
                if (!(instruction instanceof CallInst && instruction.getType() instanceof VoidType)) {
                    return;
                }
            }
        }
        Value simpleInst = SimplifyInstruction.simplify(instruction);
        if (!simpleInst.equals(instruction)) {
            replace(instruction, simpleInst, false);
            return;
        }

        if (simpleInst instanceof BinaryInst) {
            if (((BinaryInst) simpleInst).isCond()) {
                return;
            }
            checkForReplace((Instruction) simpleInst, findAdd(simpleInst));
        } else if (simpleInst instanceof GEPInst) {
            checkForReplace((Instruction) simpleInst, findAdd(simpleInst));
        } else if (simpleInst instanceof LoadInst) {
            Value pointer = ((LoadInst) simpleInst).getPointer();
            Value array = AliasAnalysis.getArrayValue(pointer);
            if (array instanceof GlobalVar) {
                if (((GlobalVar) array).getValue() instanceof ConstInt && ((GlobalVar) array).isConst()) {
                    replace((Instruction) simpleInst, ((GlobalVar) array).getValue(), true);
                    return;
                }
            }

            boolean isConstIndex = false;
            if (pointer instanceof GEPInst && array instanceof GlobalVar) {
                // const 类型的数组 直接替换值 需要检测index是不是都是常量 （有可能是变量）
                if (((GlobalVar) array).isConst()) {
                    isConstIndex = true;
                    ConstArray constArray = (ConstArray) ((GlobalVar) array).getValue();
                    List<Integer> index = new ArrayList<>();
                    Stack<GEPInst> gepStack = new Stack<>();
                    while (pointer instanceof GEPInst) {
                        gepStack.push((GEPInst) pointer);
                        pointer = ((GEPInst) pointer).getPointer();
                    }
                    while (!gepStack.isEmpty()) {
                        GEPInst curGep = gepStack.pop();
                        for (int i = 1; i < curGep.getOperands().size(); i++) {
                            if (!((curGep).getOperands().get(i) instanceof ConstInt)) {
                                isConstIndex = false;
                                break;
                            }
                            if (i == 1 && !index.isEmpty()) {
                                int tmp = index.get(index.size() - 1);
                                index.remove(index.size() - 1);
                                tmp += ((ConstInt) (curGep).getOperands().get(i)).getValue();
                                index.add(tmp);
                            } else {
                                index.add(((ConstInt) (curGep).getOperands().get(i)).getValue());
                            }
                        }
                        if (!isConstIndex) {
                            break;
                        }
                    }
                    if (isConstIndex) {
                        int offset = ((ArrayType) constArray.getType()).index2Offset(index);
                        if (constArray.get1DArray().size() <= offset) {
                            replace((Instruction) simpleInst, ConstInt.ZERO, true);
                        } else {
                            replace((Instruction) simpleInst, constArray.get1DArray().get(offset), true);
                        }
                    }
                }
            }

            if (!isConstIndex) {
                checkForReplace((Instruction) simpleInst, findAdd(simpleInst));
            }
        } else if (simpleInst instanceof PhiInst) {
            PhiInst phiInst = (PhiInst) simpleInst;
            // 针对所有分支进来的数都相同的情况
            HashSet<Value> checkSame = new HashSet<>();
            for (int i = 0; i < phiInst.getOperands().size(); i++) {
                checkSame.add(findAdd(phiInst.getOperands().get(i)));
            }
            boolean same = checkSame.size() == 1;
            if (same) {
                replace(phiInst, checkSame.iterator().next(), true);
            }
        } else if (simpleInst instanceof StoreInst) {
            Value value = ((StoreInst) simpleInst).getOperands().get(1);
            if (!(value.getType() instanceof PointerType)) {
                valueTable.put(simpleInst, simpleInst);
            }
        } else if (simpleInst instanceof CallInst && ((CallInst) simpleInst).isPure()) {
            checkForReplace((Instruction) simpleInst, findAdd(simpleInst));
        }
    }


    private Set<Instruction> dirtyInst;

    public void runGCM(Function function) {
        // 先做循环分析
        function.getLoopInfo().computeLoopInfo(function);

        // 保存所有指令
        List<Instruction> allInstructions = new ArrayList<>();
        function.getList().forEach(bbNode -> bbNode.getValue().getInstructions().forEach(instNode -> allInstructions.add(instNode.getValue())));

        dirtyInst = new HashSet<>();
        for (Instruction inst : allInstructions)
            scheduleEarly(inst, function);

        dirtyInst = new HashSet<>();
        for (Instruction inst : allInstructions)
            scheduleLate(inst, function);

    }

    // 将 instruction 在函数中尽可能前移
    public void scheduleEarly(Instruction instruction, Function function) {
        /*
        1、拿到一个指令，先判断能不能移
                如果能够移动，则插入第一个基本块的倒数第二个
        2、如果是binary或者gep或者load指令
                遍历所有operand，如果operand是指令，则先递归调用scheduleEarly，保证该指令在最前面
                如果operand所在基本块的支配级别大于该指令的支配级别，则将该指令插入到operand所在基本块的倒数第二个
        3、如果是pure的call指令
                遍历所有参数，如2所示
         */
        if (canSchedule(instruction) && !dirtyInst.contains(instruction)) {
            dirtyInst.add(instruction);
            moveAtSecToLast(instruction, function.getList().getBegin().getValue());
            if (instruction instanceof BinaryInst ||
                    instruction instanceof GEPInst ||
                    instruction instanceof LoadInst) {
                for (Value operand : instruction.getOperands()) {
                    if (operand instanceof Instruction) {
                        scheduleEarly((Instruction) operand, function);
                        if (((Instruction) operand).getParent().getDomLevel() >
                                instruction.getParent().getDomLevel()) {
                            moveAtSecToLast(instruction, ((Instruction) operand).getParent());
                        }
                    }
                }
            }
            if (instruction instanceof CallInst && ((CallInst) instruction).isPure()) {
                for (int i = 1; i < instruction.getOperands().size(); i++) {
                    if (instruction.getOperands().get(i) instanceof Instruction) {
                        Instruction operand = (Instruction) instruction.getOperands().get(i);
                        scheduleEarly(operand, function);
                        if (operand.getParent().getDomLevel() > instruction.getParent().getDomLevel()) {
                            moveAtSecToLast(instruction, operand.getParent());
                        }
                    }
                }
            }
        }
    }


    // 将 instruction 在函数中尽可能后移
    public void scheduleLate(Instruction instruction, Function function) {
        /*
        1、拿到一个指令，先判断能不能移
        2、遍历instruction的useList，得到user。
                如果user是指令类型，scheduleLate。
                若是phi指令，遍历所有incoming，找下界
                若是其他指令，则针对该指令的block找下界
        3、在 schedule early 和 schedule late 找到的上下界中找到循环深度最小的基本块
        4、插入到该基本块的倒数第二个
        5、如果bestbb是lcabb时，可能use inst的指令在inst前面，需要把inst往前放
         */
        if (canSchedule(instruction) && !dirtyInst.contains(instruction)) {
            dirtyInst.add(instruction);
            BasicBlock lcabb = null;
            for (Use use : instruction.getUsesList()) {
                Instruction useInst = (Instruction) use.getUser();
                scheduleLate(useInst, function);
                BasicBlock useBB = useInst.getParent();
                if (useInst instanceof PhiInst) {
                    for (int i = 0; i < useInst.getOperands().size(); i++) {
                        Value value = useInst.getOperands().get(i);
                        if (value.equals(instruction)) {
                            useBB = useInst.getParent().getPredecessors().get(i);
                            lcabb = lca(lcabb, useBB);
                        }
                    }
                } else if (useInst instanceof AliasAnalysis.MemPhi) {
                    for (int i = 1; i < useInst.getOperands().size(); i++) {
                        if (useInst.getOperands().get(i).equals(instruction)) {
                            useBB = useInst.getParent().getPredecessors().get(i - 1);
                            lcabb = lca(lcabb, useBB);
                        }
                    }
                } else {
                    lcabb = lca(lcabb, useBB);
                }
            }
            BasicBlock bestbb = lcabb;
            int bestLoopDepth = function.getLoopInfo().getLoopDepth(bestbb);
            while (!Objects.equals(lcabb, instruction.getParent())) {
                assert lcabb != null;
                lcabb = lcabb.getParent().getIdom().get(lcabb);
                int curLoopDepth = function.getLoopInfo().getLoopDepth(lcabb);
                if (curLoopDepth < bestLoopDepth) {
                    bestbb = lcabb;
                    bestLoopDepth = curLoopDepth;
                }
            }

            assert bestbb != null;
            moveAtSecToLast(instruction, bestbb);
            //bestbb 是 lcabb时，可能useInst在inst前面，需要把inst往前调
            for (INode<Instruction, BasicBlock> instNode : bestbb.getInstructions()) {
                Instruction inst = instNode.getValue();
                if (!(inst instanceof PhiInst || inst instanceof AliasAnalysis.MemPhi)) {
                    if (inst.getOperands().contains(instruction)) {
                        instruction.getNode().removeFromList();
                        instruction.getNode().insertBefore(instNode);
                        break;
                    }
                }
            }
        }

    }

    // binary load gep pure的call能够移动
    public boolean canSchedule(Instruction instruction) {
        return (instruction instanceof BinaryInst && !((BinaryInst) instruction).isCond()) ||
                instruction instanceof LoadInst ||
                instruction instanceof GEPInst ||
                (instruction instanceof CallInst && ((CallInst) instruction).isPure());
    }

    private void moveAtSecToLast(Instruction instruction, BasicBlock basicBlock) {
        INode<Instruction, BasicBlock> endNode = basicBlock.getInstructions().getEnd();
        instruction.getNode().removeFromList();
        instruction.getNode().insertBefore(endNode);
    }

    // lowest common ancestor
    public BasicBlock lca(BasicBlock a, BasicBlock b) {
        /*
        1、若a的支配级别小于b的支配级别，则减小b的支配级别（b赋值为b的idom）
        2、若b的支配级别小于a的支配级别，则减小a的支配级别（a赋值为a的idom）
        3、若a和b不相等，则求a，b的idom，直到相等位置。
         */
        if (a == null) {
            return b;
        }
        while (a.getDomLevel() < b.getDomLevel()) {
            b = b.getParent().getIdom().get(b);
        }
        while (b.getDomLevel() < a.getDomLevel()) {
            a = a.getParent().getIdom().get(a);
        }
        while (!(a.equals(b))) {
            a = a.getParent().getIdom().get(a);
            b = b.getParent().getIdom().get(b);
        }
        return a;
    }
}