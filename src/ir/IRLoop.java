package ir;

import ir.values.BasicBlock;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.PhiInst;

import java.util.*;

public class IRLoop {
    private IRLoop parentLoop = null;
    // 循环头
    private final BasicBlock head;

    private final List<IRLoop> subLoops = new ArrayList<>();
    // 循环内所有块
    private final List<BasicBlock> blocks = new ArrayList<>();
    // 循环内即将退出循环的block
    private final Set<BasicBlock> exitingBlocks = new HashSet<>();
    // 循环退出后第一个到达的block
    private final Set<BasicBlock> exitBlocks = new HashSet<>();
    // 跳转到循环头的块
    private final List<BasicBlock> latchBlocks = new ArrayList<>();
    // 必经节点(未做完)
    private final Set<BasicBlock> domNodes = new HashSet<>();

    // 仅在简单循环中计算
    private PhiInst indVar;             // 索引
    private Value indVarInit;       // 初值
    private Value indVarEnd;        // 边界
    private Instruction stepInst;   // 迭代instr
    private Instruction indVarCondInst; // 边界判断中携带IntVar的指令
    private Value step;                 // 步长
    private Integer iterationTime;          // 次数

    public IRLoop(BasicBlock head) {
        this.head = head;
        blocks.add(head);
    }

    public Integer getLoopDepth() {
        int depth = 0;
        IRLoop now = this;
        while (now != null) {
            depth++;
            now = now.parentLoop;
        }
        return depth;
    }

    public BasicBlock getHead() {
        return head;
    }

    public BasicBlock getHeadPreOutOfLoop() {
        for (BasicBlock bb : head.getPredecessors()) {
            if (!blocks.contains(bb)) {
                return bb;
            }
        }
        return null;
    }

    public IRLoop getParentLoop() {
        return parentLoop;
    }

    public void setParentLoop(IRLoop parentLoop) {
        this.parentLoop = parentLoop;
    }

    public boolean hasParent() {
        return parentLoop != null;
    }

    public List<IRLoop> getSubLoops() {
        return subLoops;
    }

    public void addSubLoop(IRLoop subLoop) {
        subLoops.add(subLoop);
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public void addBlock(BasicBlock bb) {
        blocks.add(bb);
    }

    public void reverseBlock1() {
        Collections.reverse(blocks);
        BasicBlock bb = blocks.get(blocks.size() - 1);
        blocks.add(0, bb);
        blocks.remove(blocks.size() - 1);
    }

    public Set<BasicBlock> getExitingBlocks() {
        return exitingBlocks;
    }

    public void addExitingBlock(BasicBlock bb) {
        exitingBlocks.add(bb);
    }

    public Set<BasicBlock> getExitBlocks() {
        return exitBlocks;
    }

    public void addExitBlock(BasicBlock bb) {
        exitBlocks.add(bb);
    }

    public List<BasicBlock> getLatchBlocks() {
        return latchBlocks;
    }

    public void addLatchBlock(BasicBlock bb) {
        latchBlocks.add(bb);
    }

    public Set<BasicBlock> getDomNodes() {
        return domNodes;
    }

    public Integer getIterationTime() {
        return iterationTime;
    }

    public void setIterationTime(int iterationTime) {
        this.iterationTime = iterationTime;
    }

    public void clearAllSimpLoopInfo() {
        indVar = null;
        indVarInit = null;
        indVarEnd = null;
        stepInst = null;
        indVarCondInst = null;
        step = null;
        iterationTime = null;
    }

    public void setIterationTime(Integer iterationTime) {
        this.iterationTime = iterationTime;
    }

    public PhiInst getIndVar() {
        return indVar;
    }

    public void setIndVar(PhiInst indVar) {
        this.indVar = indVar;
    }

    public Value getIndVarInit() {
        return indVarInit;
    }

    public void setIndVarInit(Value indVarInit) {
        this.indVarInit = indVarInit;
    }

    public Value getIndVarEnd() {
        return indVarEnd;
    }

    public void setIndVarEnd(Value indVarEnd) {
        this.indVarEnd = indVarEnd;
    }

    public Instruction getStepInst() {
        return stepInst;
    }

    public void setStepInst(Instruction stepInst) {
        this.stepInst = stepInst;
    }

    public Instruction getIndVarCondInst() {
        return indVarCondInst;
    }

    public void setIndVarCondInst(Instruction indVarCondInst) {
        this.indVarCondInst = indVarCondInst;
    }

    public Value getStep() {
        return step;
    }

    public void setStep(Value step) {
        this.step = step;
    }

    public boolean isSimpLoop() {
        return head.getPredecessors().size() == 2 && latchBlocks.size() == 1 &&
                exitBlocks.size() == 1 && exitingBlocks.size() == 1;
    }

    public Instruction getLatchCmp() {
        if (latchBlocks.size() != 1) {
            return null;
        }
        Instruction instr = latchBlocks.iterator().next().getInstructions().getEnd().getValue();
        if (!(instr.getOperands().get(0) instanceof Instruction)) {
            return null;
        }
        return (Instruction) instr.getOperands().get(0);
    }

    public Instruction getExitCmp() {
        if (exitingBlocks.size() != 1) {
            return null;
        }
        Instruction instr = exitingBlocks.iterator().next().getInstructions().getEnd().getValue();
        if (!(instr.getOperands().get(0) instanceof Instruction)) {
            return null;
        }
        return (Instruction) instr.getOperands().get(0);
    }

    public BasicBlock getSingleLatchBlock() {
        if (latchBlocks.size() != 1) {
            return null;
        }
        return latchBlocks.get(0);
    }

    public BasicBlock getSingleExitingBlock() {
        if (exitingBlocks.size() != 1) {
            return null;
        }
        return exitingBlocks.iterator().next();
    }

    public BasicBlock getSingleExitBlock() {
        if (exitBlocks.size() != 1) {
            return null;
        }
        return exitBlocks.iterator().next();
    }

    public BasicBlock getHeadPred() {
        BasicBlock headPred = null;
        int cnt = 0;
        for (BasicBlock pred : head.getPredecessors()) {
            if (pred.getLoopDepth() != head.getLoopDepth()) {
                headPred = pred;
                cnt++;
                if (cnt != 1) {
                    return null;
                }
            }
        }

        return headPred;
    }

    public void removeSubLoop(IRLoop subLoop) {
        subLoops.remove(subLoop);
        subLoop.setParentLoop(null);
    }
}
