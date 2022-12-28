package pass.ir;

import ir.IRModule;
import ir.analysis.AliasAnalysis;
import ir.values.*;
import ir.values.instructions.*;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.CallInst;
import pass.Pass;
import utils.INode;
import utils.PairTable;

import java.util.*;

/**
 * GVN: 尽可能地消除冗余的变量，同时会做常量合并、代数化简
 */
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
        INode<Instruction, BasicBlock> instNode = basicBlock.getInstructions().getBegin();
        while (!instNode.equals(basicBlock.getInstructions().getEnd())) {
            INode<Instruction, BasicBlock> nextNode = instNode.getNext();
            simplifyInstruction(instNode.getValue());
            instNode = nextNode;
        }
    }

    private void simplifyInstruction(Instruction instruction) {
        Value simpleInst = SimplifyInstruction.simplify(instruction);
        if (!simpleInst.equals(instruction)) {
            replace(instruction, simpleInst, false);
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
            if (key instanceof LoadInst && !key.equals(loadInst)) {
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
}