package pass.ir;

import ir.IRLoop;
import ir.IRModule;
import ir.analysis.LoopInfo;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.PhiInst;
import pass.Pass;
import utils.INode;

import java.util.*;

public class RemoveUnUseLoop implements Pass.IRPass {
    @Override
    public String getName() {
        return "RemoveUnUseLoop";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> funcNode : m.getFunctions()) {
            if (!funcNode.getValue().isLibraryFunction()) {
                compute(funcNode.getValue());
            }
        }
    }

    private final Queue<IRLoop> loopQueue = new LinkedList<>();

    public static void addLoopToQueue(IRLoop loop, Queue<IRLoop> queue) {
        for (IRLoop subLoop : loop.getSubLoops()) {
            if (subLoop != null) {
                addLoopToQueue(subLoop, queue);
            }
        }
        queue.add(loop);
    }

    private LoopInfo curLoopInfo;

    public void compute(Function f) {
        f.computeLoopInfo();
        curLoopInfo = f.getLoopInfo();
        for (IRLoop topLoop : f.getLoopInfo().getTopLevelLoops()) {
            addLoopToQueue(topLoop, loopQueue);
        }
        while (!loopQueue.isEmpty()) {
            IRLoop loop = loopQueue.remove();
            removeUselessLoop(loop);
        }
    }

    private BasicBlock getExitBlock(Instruction latchBr, IRLoop loop) {
        if (loop.getBlocks().contains((BasicBlock) (latchBr.getOperands().get(1)))) {
            return (BasicBlock) (latchBr.getOperands().get(2));
        } else {
            return (BasicBlock) (latchBr.getOperands().get(1));
        }
    }

    private void removeUselessLoop(IRLoop loop) {
        BasicBlock preHeader = loop.getHeadPred();
        BasicBlock latchBlock = loop.getSingleLatchBlock();
        if (preHeader == null || latchBlock == null || loop.getExitBlocks().size() > 1) {
            return;
        }

        Instruction latchBr = latchBlock.getInstructions().getEnd().getValue();
        if (latchBr.getOperands().size() == 1) {
            return;
        }
        BasicBlock exit = getExitBlock(latchBr, loop);

        int headPredIndex = exit.getPredecessors().indexOf(preHeader);
        if (headPredIndex == -1) {
            return;
        }

        Set<Instruction> loopInsts = new HashSet<>();
        for (BasicBlock bb : loop.getBlocks()) {
            for (INode<Instruction, BasicBlock> instNode : bb.getInstructions()) {
                Instruction inst = instNode.getValue();
                if (inst.getOperator() == Operator.Store || inst.getOperator() == Operator.Ret
                        || (inst.getOperator() == Operator.Call &&
                        ((Function) inst.getOperands().get(0)).hasSideEffect())) {
                    return;
                }
                loopInsts.add(inst);
            }
        }

        ArrayList<Integer> predIndexList = new ArrayList<>();
        int index = 0;
        for (BasicBlock pred : exit.getPredecessors()) {
            if (loop.getExitingBlocks().contains(pred)) {
                predIndexList.add(index);
            }
            index++;
        }
        for (INode<Instruction, BasicBlock> instNode : exit.getInstructions()) {
            Instruction inst = instNode.getValue();
            if (!(inst instanceof PhiInst)) {
                break;
            }

            for (Value op : inst.getOperands()) {
                if (op instanceof Instruction && loopInsts.contains(op)) {
                    return;
                }
            }

            for (int i : predIndexList) {
                if (inst.getOperands().get(i) != inst.getOperands().get(headPredIndex)) {
                    return;
                }
            }
        }

        Instruction preBrInst = preHeader.getInstructions().getEnd().getValue();
        if (preBrInst.getOperands().size() == 1) {
            return;
        }

        // 消除

        // 把前一个块的br改成jump
        preHeader.getSuccessors().remove(loop.getHead());
        HashSet<Integer> set = new HashSet<>();
        set.add(0);
        set.add(preBrInst.getOperands().indexOf(loop.getHead()));
        preBrInst.removeNumberOperand(set);

        HashSet<BasicBlock> predSet = new HashSet<>();
        ArrayList<Integer> phiPredList = new ArrayList<>();

        for (index = 0; index < exit.getPredecessors().size(); index++) {
            BasicBlock pred = exit.getPredecessors().get(index);
            if (loop.getBlocks().contains(pred)) {
                predSet.add(pred);
                phiPredList.add(index);
            }
        }
        for (BasicBlock pred : predSet) {
            exit.getPredecessors().remove(pred);
        }

        set.clear();
        set.addAll(phiPredList);
        for (INode<Instruction, BasicBlock> instNode : exit.getInstructions()) {
            Instruction inst = instNode.getValue();
            if (!(inst instanceof PhiInst)) {
                break;
            }
            inst.removeNumberOperand(set);
        }

        // 删块
        for (BasicBlock bb : loop.getBlocks()) {
            INode<Instruction, BasicBlock> instNode = bb.getInstructions().getBegin();
            while (instNode != null) {
                INode<Instruction, BasicBlock> tmp = instNode.getNext();
                Instruction inst = instNode.getValue();
                inst.removeUseFromOperands();
                inst.replaceUsedWith(null);
                inst.getNode().removeFromList();
                instNode = tmp;
            }
            bb.getNode().removeFromList();
        }

        curLoopInfo.removeLoop(loop);
    }
}
