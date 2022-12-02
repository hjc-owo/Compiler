package pass.ir;

import ir.IRLoop;
import ir.IRModule;
import ir.values.BasicBlock;
import ir.values.ConstInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.PhiInst;
import ir.values.instructions.terminator.BrInst;
import pass.Pass;
import utils.INode;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class BranchOptimization implements Pass.IRPass{
    @Override
    public String getName() {
        return "BranchOptimization";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            if (!funcEntry.getValue().isLibraryFunction()) {
                compute(funcEntry.getValue());
            }
        }
    }

    public boolean compute(Function f) {
        boolean removePhi = false;
        boolean done = false;
        while (!done) {
            done = removeSinglePhi(f);
            if (!done) {
                removePhi = true;
            }
            done &= delOnlyUncondBr(f) && mergeUnCondJump(f) && mergeConstCondBr(f) && removeDeadBlock(f);
        }
        return removePhi;
    }

    private boolean removeDeadBlock(Function func) {
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
        HashSet<BasicBlock> bbToRemove = new HashSet<>();
        for (INode<BasicBlock, Function> bbEntry : func.getList()) {
            BasicBlock bb = bbEntry.getValue();
            if (!vis.contains(bb)) {
                bbToRemove.add(bb);
            }
        }
        boolean res = bbToRemove.isEmpty();
        for (BasicBlock bb : bbToRemove) {
            bb.removeSelf();
        }
        return res;
    }

    private boolean delOnlyUncondBr(Function f) {
        boolean done = true;
        INode<BasicBlock, Function> bbNode = f.getList().getBegin().getNext();
        while (bbNode != null) {
            INode<BasicBlock, Function> nbb = bbNode.getNext();
            BasicBlock bb = bbNode.getValue();
            if (bb.getPredecessors().isEmpty()) {
                bbNode = nbb;
                continue;
            }
            Instruction brInst = bb.getInstructions().getEnd().getValue();

            if (bb.getInstructions().getSize() == 1 && brInst instanceof BrInst && brInst.getOperands().size() == 1) {
                boolean flag = false;
                BasicBlock succ = (BasicBlock) (brInst.getOperands().get(0));
                if (succ.getInstructions().getBegin().getValue() instanceof PhiInst) {
                    for (BasicBlock pred : bb.getPredecessors()) {
                        if (succ.getPredecessors().contains(pred)) {
                            flag = true;
                            break;
                        }
                    }
                }

                if (flag) {
                    bbNode = nbb;
                    continue;
                }

                done = false;

                int bbIndex = succ.getPredecessors().indexOf(bb);
                succ.getPredecessors().remove(bb);

                for (BasicBlock pred : bb.getPredecessors()) {
                    Instruction predLastInst = (Instruction) (pred.getInstructions().getEnd().getValue());

                    if (predLastInst.getOperands().size() == 1) {
                        predLastInst.replaceOperands(0, succ);
                        pred.getSuccessors().set(0, succ);
                    } else if (predLastInst.getOperands().size() == 3) {
                        for (int i = 1; i <= 2; i++) {
                            if (predLastInst.getOperands().get(i) == bb) {
                                predLastInst.replaceOperands(i, succ);
                                pred.getSuccessors().set(i - 1, succ);
                            }
                        }
                    }
                    succ.getPredecessors().add(pred);
                }

                for (INode<Instruction, BasicBlock> instrEntry : succ.getInstructions()) {
                    Instruction inst = instrEntry.getValue();
                    if (!(inst instanceof PhiInst)) {
                        break;
                    }

                    Value v = inst.getOperands().get(bbIndex);
                    HashSet<Integer> set = new HashSet<>();
                    set.add(bbIndex);
                    inst.removeNumberOperand(set);
                    for (int i = 0; i < bb.getPredecessors().size(); i++) {
                        inst.addOperand(v);
                    }
                }

                brInst.removeUseFromOperands();
                bbNode.removeFromList();
            }

            bbNode = nbb;
        }

        return done;
    }

    private boolean mergeUnCondJump(Function f) {
        boolean done = true;

        for (INode<BasicBlock, Function> bbEntry : f.getList()) {
            if (bbEntry.getParent() == null) {
                continue;
            }
            BasicBlock bb = bbEntry.getValue();

            boolean notOk = true;
            while (notOk) {
                notOk = false;
                Instruction brInst = bb.getInstructions().getEnd().getValue();
                if (brInst instanceof BrInst && brInst.getOperands().size() == 1) {
                    BasicBlock succ = (BasicBlock) brInst.getOperands().get(0);
                    if (!(succ.getInstructions().getBegin().getValue() instanceof PhiInst)) {
                        notOk = mergeBasicBlock(bb, (BasicBlock) brInst.getOperands().get(0));
                        if (notOk) {
                            done = false;
                        }
                    }
                }
            }
        }

        return done;
    }

    public boolean removeSinglePhi(Function f) {
        boolean done = true;

        for (INode<BasicBlock, Function> bbEntry : f.getList()) {
            BasicBlock bb = bbEntry.getValue();
            INode<Instruction, BasicBlock> instrEntry = bb.getInstructions().getBegin();
            while (instrEntry != null) {
                INode<Instruction, BasicBlock> tmp = instrEntry.getNext();
                Instruction inst = instrEntry.getValue();
                if (!(inst instanceof PhiInst)) {
                    break;
                }
                if (bb.getPredecessors().size() == 1) {
                    inst.removeUseFromOperands();
                    inst.replaceUsedWith(inst.getOperands().get(0));
                    instrEntry.removeFromList();
                    done = false;
                }
                instrEntry = tmp;
            }
        }

        return done;
    }

    private boolean mergeConstCondBr(Function f) {
        f.computeSimpLoopInfo();
        ir.analysis.LoopInfo loopInfo = f.getLoopInfo();
        HashSet<BasicBlock> headPreds = new HashSet<>();
        for (IRLoop l : loopInfo.getLoops()) {
            if (l.getHeadPred() != null) {
                headPreds.add(l.getHeadPred());
            }
        }

        boolean done = true;

        for (INode<BasicBlock, Function> bbNode : f.getList()) {
            BasicBlock bb = bbNode.getValue();
            if (headPreds.contains(bb)) {
                continue;
            }
            Instruction brInst = bb.getInstructions().getEnd().getValue();
            if (brInst instanceof BrInst && brInst.getOperands().size() == 3) {
                if (brInst.getOperands().get(1) == brInst.getOperands().get(2)) {
                    BasicBlock targetBB = (BasicBlock) (brInst.getOperands().get(1));
                    HashSet<Integer> indexArr = new HashSet<>();
                    indexArr.add(0);
                    indexArr.add(1);
                    brInst.removeNumberOperand(indexArr);

                    bb.getSuccessors().remove(1);
                    for (int i = 0; i < targetBB.getPredecessors().size(); i++) {
                        if (targetBB.getPredecessors().get(i) == bb) {
                            targetBB.getPredecessors().remove(i);
                            break;
                        }
                    }

                    done = false;
                } else if (brInst.getOperands().get(0) instanceof ConstInt) {
                    int cond = ((ConstInt) brInst.getOperands().get(0)).getValue() > 0 ? 1 : 0;
                    BasicBlock unreachBB = (BasicBlock) (brInst.getOperands().get(1 + cond));
                    HashSet<Integer> set = new HashSet<>();
                    set.add(0);
                    set.add(cond + 1);
                    brInst.removeNumberOperand(set);
                    bb.getSuccessors().remove(unreachBB);
                    removePredBasicBlock(bb, unreachBB);
                    done = false;
                }
            }
        }

        return done;
    }

    private boolean mergeBasicBlock(BasicBlock pred, BasicBlock succ) {
        if (succ.getPredecessors().size() != 1) {
            return false;
        }
        INode<Instruction, BasicBlock> instrEntry = succ.getInstructions().getBegin();
        while (instrEntry != null) {
            INode<Instruction, BasicBlock> tmp = instrEntry.getNext();
            instrEntry.removeFromList();
            instrEntry.insertBefore(pred.getInstructions().getEnd());
            instrEntry = tmp;
        }
        pred.setSuccessors(succ.getSuccessors());
        pred.getInstructions().getEnd().removeFromList();
        for (BasicBlock bb : succ.getSuccessors()) {
            int index = bb.getPredecessors().indexOf(succ);
            bb.getPredecessors().set(index, pred);
        }
        succ.getNode().removeFromList();
        return true;
    }

    private void removePredBasicBlock(BasicBlock pred, BasicBlock succ) {
        HashSet<Integer> predIndexArr = new HashSet<>();
        predIndexArr.add(succ.getPredecessors().indexOf(pred));

        succ.getPredecessors().remove(pred);
        INode<Instruction, BasicBlock> instrEntry = succ.getInstructions().getBegin();
        while (instrEntry != null) {
            INode<Instruction, BasicBlock> tmp = instrEntry.getNext();
            Instruction inst = instrEntry.getValue();
            if (!(inst instanceof PhiInst)) {
                break;
            }

            if (inst.getOperands().size() == 1) {
                inst.replaceUsedWith(inst.getOperands().get(0));
                instrEntry.removeFromList();
                inst.removeUseFromOperands();
            } else {
                inst.removeNumberOperand(predIndexArr);
            }
            instrEntry = tmp;
        }
    }
}
