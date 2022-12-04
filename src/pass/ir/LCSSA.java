package pass.ir;

import ir.IRLoop;
import ir.IRModule;
import ir.analysis.DomAnalysis;
import ir.types.IntegerType;
import ir.values.*;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.PhiInst;
import pass.Pass;
import utils.INode;

import java.util.*;

public class LCSSA implements Pass.IRPass {
    @Override
    public String getName() {
        return "LCSSA";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> funcNode : m.getFunctions()) {
            if (!funcNode.getValue().isLibraryFunction()) {
                compute(funcNode.getValue());
            }
        }
    }

    private void compute(Function func) {
        DomAnalysis.analyzeDom(func);
        func.getLoopInfo().computeLoopInfo(func);

        for (IRLoop l : func.getLoopInfo().getTopLevelLoops()) {
            compute(l);
        }
    }

    private void compute(IRLoop loop) {
        for (IRLoop subL : loop.getSubLoops()) {
            compute(subL);
        }
        Set<Instruction> liveOut = getLiveOut(loop);
        if (liveOut.isEmpty()) {
            return;
        } else if (loop.getExitBlocks().isEmpty()) {
            return;
        }
        for (Instruction inst : liveOut) {
            generatePhi(loop, inst);
        }
    }

    private final Map<BasicBlock, Value> map = new HashMap<>();

    // 删除指令在循环外的使用
    private void generatePhi(IRLoop loop, Instruction inst) {
        map.clear();
        BasicBlock bb = inst.getParent();
        // 循环出口插入phi
        for (BasicBlock exitBlock : loop.getExitBlocks()) {
            Set<BasicBlock> domers = exitBlock.getParent().getDom().get(exitBlock);

            if (!map.containsKey(exitBlock) && domers.contains(bb)) {
                ArrayList<Value> values = new ArrayList<>();
                for (int i = 0; i < exitBlock.getPredecessors().size(); i++) {
                    values.add(inst);
                }
                PhiInst phiInst = BuildFactory.getInstance().buildPhi(exitBlock, inst.getType(), values);
                map.put(exitBlock, phiInst);
            }
        }

        List<Use> usesList = new ArrayList<>(inst.getUsesList());
        for (Use use : usesList) {
            BasicBlock userBlock = getUserBlock(use);
            Instruction userInst = (Instruction) use.getUser();

            if (loop.getBlocks().contains(userBlock) || userBlock == bb) {
                continue;
            }

            Value value = getValueInBlock(userBlock, loop);
            userInst.replaceOperands(inst, value);
        }
    }

    private Value getValueInBlock(BasicBlock userBlock, IRLoop loop) {
        if (userBlock == null) {
            return new NullValue();
        } else if (map.get(userBlock) != null) {
            return map.get(userBlock);
        }

        BasicBlock idom = userBlock.getParent().getIdom().get(userBlock);
        if (!loop.getBlocks().contains(idom)) {
            Value value = getValueInBlock(idom, loop);
            map.put(userBlock, value);
            return value;
        }
        List<Value> values = new ArrayList<>();
        for (int i = 0; i < userBlock.getPredecessors().size(); i++) {
            values.add(getValueInBlock(userBlock.getPredecessors().get(i), loop));
        }
        PhiInst phi = BuildFactory.getInstance().buildPhi(userBlock, IntegerType.i32, values);
        map.put(userBlock, phi);

        return phi;
    }

    public Set<Instruction> getLiveOut(IRLoop loop) {
        Set<Instruction> res = new HashSet<>();
        for (BasicBlock bb : loop.getBlocks()) {
            for (INode<Instruction, BasicBlock> instrEntry : bb.getInstructions()) {
                Instruction inst = instrEntry.getValue();
                for (Use use : inst.getUsesList()) {
                    BasicBlock userBlock = getUserBlock(use);
                    if (!loop.getBlocks().contains(userBlock) && bb != userBlock) {
                        res.add(inst);
                        break;
                    }
                }
            }
        }
        return res;
    }

    private BasicBlock getUserBlock(Use use) {
        Instruction userInst = (Instruction) use.getUser();
        BasicBlock userBlock = userInst.getParent();
        if (userInst instanceof PhiInst) {
            PhiInst phiInst = (PhiInst) userInst;
            for (int i = 0; i < phiInst.getOperands().size(); i++) {
                Value v = phiInst.getOperands().get(i);
                if (v.getUsesList().contains(use)) {
                    userBlock = phiInst.getParent().getPredecessors().get(i);
                }
            }
        }
        return userBlock;
    }
}
