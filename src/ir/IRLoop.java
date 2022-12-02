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

    public boolean hasParent() {
        return parentLoop != null;
    }

    public IRLoop getParentLoop() {
        return parentLoop;
    }

    public void setParentLoop(IRLoop parentLoop) {
        this.parentLoop = parentLoop;
    }

    public void addSubLoop(IRLoop subLoop) {
        subLoops.add(subLoop);
    }

    public List<IRLoop> getSubLoops() {
        return subLoops;
    }

    public void reverseBlock1() {
        Collections.reverse(blocks);
        BasicBlock bb = blocks.get(blocks.size() - 1);
        blocks.add(0, bb);
        blocks.remove(blocks.size() - 1);
    }

    public void addBlock(BasicBlock bb) {
        blocks.add(bb);
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
}
