package pass.ir;

import ir.IRModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import pass.Pass;
import utils.INode;

import java.util.*;

public class DeadCodeElimination implements Pass.IRPass {
    @Override
    public String getName() {
        return "Dead Code Elimination";
    }

    @Override
    public void run(IRModule m) {
        InterProceduralAnalysis preAnalysis = new InterProceduralAnalysis();
        preAnalysis.run(m);

        List<Function> funcToRemove = new ArrayList<>();
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function func = funcEntry.getValue();
            if (!func.getName().equals("main") && func.getPredecessors().isEmpty()) {
                funcToRemove.add(func);
            } else if (!func.isLibraryFunction()) {
                dce(func);
            }
        }
        for (Function func : funcToRemove) {
            func.getNode().removeFromList();
        }
    }

    public void dce(Function func) {
        Set<Instruction> instSet = new HashSet<>();
        for (INode<BasicBlock, Function> bbEntry : func.getList()) {
            for (INode<Instruction, BasicBlock> instrEntry : bbEntry.getValue().getInstructions()) {
                Instruction instr = instrEntry.getValue();
                if (dceNeed(instr)) {
                    findUsefulClosure(instr, instSet);
                }
            }
        }

        Set<Instruction> removeSet = new HashSet<>();
        for (INode<BasicBlock, Function> bbEntry : func.getList()) {
            for (INode<Instruction, BasicBlock> instrEntry : bbEntry.getValue().getInstructions()) {
                Instruction instr = instrEntry.getValue();
                if (!instSet.contains(instr)) {
                    removeSet.add(instr);
                }
            }
        }
        removeSet.forEach(instr -> {
            instr.removeUseFromOperands();
            instr.replaceUsedWith(null);
            instr.getNode().removeFromList();
        });
        removeDeadBlock(func);
    }

    private void removeDeadBlock(Function func) {
        Set<BasicBlock> vis = new HashSet<>();
        Stack<BasicBlock> st = new Stack<>();
        st.add(func.getList().getBegin().getValue());
        while (!st.isEmpty()) {
            BasicBlock now = st.pop();
            if (vis.contains(now)) {
                continue;
            }
            vis.add(now);
            now.getSuccessors().forEach(st::push);
        }
        Set<BasicBlock> bbToRemove = new HashSet<>();
        for (INode<BasicBlock, Function> bbEntry : func.getList()) {
            BasicBlock bb = bbEntry.getValue();
            if (!vis.contains(bb)) {
                bbToRemove.add(bb);
            }
        }
        for (BasicBlock bb : bbToRemove) {
            bb.removeSelf();
        }
    }

    private void findUsefulClosure(Instruction instr, Set<Instruction> instSet) {
        Stack<Instruction> stack = new Stack<>();
        stack.push(instr);
        Instruction now;
        while (!stack.isEmpty()) {
            now = stack.pop();
            if (instSet.contains(now)) {
                continue;
            }
            instSet.add(now);
            for (Value operand : now.getOperands()) {
                if (operand instanceof Instruction) {
                    stack.add((Instruction) operand);
                }
            }
        }
    }

    private boolean dceNeed(Instruction instr) {
        if (instr.getOperator() == Operator.Br ||
                instr.getOperator() == Operator.Ret ||
                instr.getOperator() == Operator.Store) {
            return true;
        } else if (instr.getOperator() == Operator.Call) {
            return ((Function) instr.getOperands().get(0)).hasSideEffect();
        } else {
            return false;
        }
    }
}
