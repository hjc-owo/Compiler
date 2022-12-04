package ir.analysis;

import ir.IRLoop;
import ir.values.*;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.PhiInst;

import java.util.*;

public class LoopInfo {

    private final Map<BasicBlock, IRLoop> loopMap = new HashMap<>();
    private final List<IRLoop> loops = new ArrayList<>();
    private final List<IRLoop> topLevelLoops = new ArrayList<>();
    private final Function f;

    public LoopInfo(Function f) {
        this.f = f;
    }

    public void run() {
        computeLoopInfo(f);
        computeOtherInfo(f);
    }

    public void computeLoopInfo(Function f) {
        DomAnalysis.analyzeDom(f);
        Map<BasicBlock, List<BasicBlock>> domInfo = f.getIdoms();
        Map<BasicBlock, Set<BasicBlock>> domers = f.getDom();
        loopMap.clear();
        loops.clear();
        topLevelLoops.clear();

        // 后序遍历IDom
        List<BasicBlock> postOrder = new ArrayList<>();
        Stack<BasicBlock> stack = new Stack<>();

        BasicBlock entry = f.getList().getBegin().getValue();
        stack.push(entry);
        BasicBlock now;
        // 构建Dom树后序遍历
        while (!stack.isEmpty()) {
            now = stack.pop();
            postOrder.add(now);
            for (BasicBlock child : domInfo.get(now)) {
                stack.push(child);
            }
        }
        Collections.reverse(postOrder); // 由于不是严格的树结构可以这样做后序遍历

        for (BasicBlock header : postOrder) {
            Stack<BasicBlock> backEdges = new Stack<>();
            for (BasicBlock backEdge : header.getPredecessors()) {
                if (domers.getOrDefault(backEdge, new HashSet<>()).contains(header)) {
                    backEdges.push(backEdge);
                }
            }

            if (!backEdges.isEmpty()) {
                IRLoop loop = new IRLoop(header);
                while (!backEdges.isEmpty()) {
                    BasicBlock edge = backEdges.pop();
                    IRLoop subLoop = getLoop(edge);
                    if (subLoop == null) {
                        loopMap.put(edge, loop);
                        if (edge.equals(loop.getHead())) {
                            continue;
                        }
                        for (BasicBlock edgePred : edge.getPredecessors()) {
                            backEdges.push(edgePred);
                        }
                    } else {
                        while (subLoop.hasParent()) {
                            subLoop = subLoop.getParentLoop();
                        }
                        if (subLoop == loop) {
                            continue;
                        }
                        subLoop.setParentLoop(loop);
                        for (BasicBlock subHeadPred : subLoop.getHead().getPredecessors()) {
                            if (!Objects.equals(loopMap.get(subHeadPred), subLoop)) {
                                backEdges.push(subHeadPred);
                            }
                        }
                    }
                }
            }
        }

        Set<BasicBlock> vis = new HashSet<>();
        populateLoopsDFS(entry, vis);

        computeAllLoops();
    }

    private void computeOtherInfo(Function f) {
        computeExitingAndExitBlocks();
        computeLatchBlocks();
        // computeDomNodes();
        computeIndVar();
    }

    private void computeExitingAndExitBlocks() {
        for (IRLoop l : loops) {
            for (BasicBlock bb : l.getBlocks()) {
                for (BasicBlock succ : bb.getSuccessors()) {
                    if (!l.getBlocks().contains(succ)) {
                        l.addExitingBlock(bb);
                        l.addExitBlock(succ);
                    }
                }
            }
        }
    }

    private void computeLatchBlocks() {
        for (IRLoop l : loops) {
            for (BasicBlock predbb : l.getHead().getPredecessors()) {
                if (l.getBlocks().contains(predbb)) {
                    l.addLatchBlock(predbb);
                }
            }
        }
    }

    private void computeAllLoops() {
        loops.clear();
        Stack<IRLoop> stack = new Stack<>();
        stack.addAll(topLevelLoops);
        loops.addAll(topLevelLoops);
        while (!stack.isEmpty()) {
            IRLoop l = stack.pop();
            if (!l.getSubLoops().isEmpty()) {
                stack.addAll(l.getSubLoops());
                loops.addAll(l.getSubLoops());
            }

        }
    }

    private void populateLoopsDFS(BasicBlock bb, Set<BasicBlock> vis) {
        if (vis.contains(bb)) {
            return;
        }
        vis.add(bb);
        for (BasicBlock succbb : bb.getSuccessors()) {
            populateLoopsDFS(succbb, vis);
        }
        IRLoop subLoop = getLoop(bb);
        if (subLoop != null && subLoop.getHead().equals(bb)) {
            if (subLoop.hasParent()) {
                subLoop.getParentLoop().addSubLoop(subLoop);
            } else {
                topLevelLoops.add(subLoop);
            }

            subLoop.reverseBlock1();
            Collections.reverse(subLoop.getSubLoops());

            subLoop = subLoop.getParentLoop();
        }

        while (subLoop != null) {
            subLoop.addBlock(bb);
            subLoop = subLoop.getParentLoop();
        }
    }

    public void computeIndVar() {
        for (IRLoop l : loops) {
            l.clearAllSimpLoopInfo();
            Instruction cmp = l.getLatchCmp();

            if (!l.isSimpLoop() || (cmp == null)) {
                continue;
            }

            for (int i = 0; i < 2; i++) {
                Value op = cmp.getOperands().get(i);
                if (!(op instanceof Instruction)) {
                    if (!(cmp.getOperands().get(1 - i) instanceof Instruction)) {
                        continue;
                    }
                    l.setIndVarCondInst((Instruction) cmp.getOperands().get(1 - i));
                    l.setIndVarEnd(cmp.getOperands().get(i));
                } else {
                    Instruction opInst = (Instruction) op;
                    if (!getLoopDepth(opInst.getParent()).equals(getLoopDepth(cmp.getParent()))) {
                        if (!(cmp.getOperands().get(1 - i) instanceof Instruction)) {
                            continue;
                        }
                        l.setIndVarCondInst((Instruction) cmp.getOperands().get(1 - i));
                        l.setIndVarEnd(cmp.getOperands().get(i));
                    } else {
                        if (!(cmp.getOperands().get(i) instanceof Instruction)) {
                            continue;
                        }
                        l.setIndVarCondInst((Instruction) cmp.getOperands().get(i));
                        l.setIndVarEnd(cmp.getOperands().get(1 - i));
                    }
                }
            }

            if (!(l.getIndVarCondInst() instanceof BinaryInst)) {
                return;
            }

            Instruction indVarCondInst = l.getIndVarCondInst();
            Value compareBias = null;
            for (Value op : indVarCondInst.getOperands()) {
                if (op instanceof PhiInst) {
                    l.setIndVar((PhiInst) op);
                } else {
                    compareBias = op;
                }
            }

            if (l.getIndVar() == null) {
                return;
            }

            int indVarDepth = getLoopDepth(l.getIndVar().getParent());
            for (Value v : l.getIndVar().getOperands()) {
                if (v instanceof Instruction) {
                    int incomingDepth = getLoopDepth(((Instruction) v).getParent());
                    if (indVarDepth != incomingDepth) {
                        l.setIndVarInit(v);
                    } else {
                        l.setStepInst((Instruction) v);
                    }
                } else {
                    l.setIndVarInit(v);
                }
            }

            Instruction stepInst = l.getStepInst();
            if (stepInst == null) {
                return;
            }

            for (Value op : stepInst.getOperands()) {
                if (op != l.getIndVar()) {
                    l.setStep(op);
                }
            }

            if (stepInst.getOperator() == Operator.Add &&
                    l.getStep() instanceof Const &&
                    l.getIndVarInit() instanceof Const &&
                    l.getIndVarEnd() instanceof Const &&
                    compareBias instanceof Const) {
                int init = ((ConstInt) l.getIndVarInit()).getValue();
                int end = ((ConstInt) l.getIndVarEnd()).getValue();
                int step = ((ConstInt) l.getStep()).getValue();
                int bias = ((ConstInt) compareBias).getValue();

                Integer time = null;
                if (cmp.getOperator() == Operator.Lt && step > 0) {
                    time = init < end ? ceilDiv(end - init, step) : 0;
                } else if (cmp.getOperator() == Operator.Gt && step < 0) {
                    time = init > end ? ceilDiv(init - end, -step) : 0;
                } else if (cmp.getOperator() == Operator.Le && step > 0) {
                    time = init <= end ? ceilDiv(end - init + 1, step) : 0;
                } else if (cmp.getOperator() == Operator.Ge && step < 0) {
                    time = init >= end ? ceilDiv(init - end + 1, -step) : 0;
                } else if (cmp.getOperator() == Operator.Ne) {
                    if (end - init == 0) {
                        time = 0;
                    } else if (step * (end - init) > 0 && (end - init) % step == 0) {
                        time = (end - init) / step;
                    }
                }

                if (time != null) {
                    time -= bias - step;
                }
                l.setIterationTime(time);
            }
        }
    }

    private int ceilDiv(int x, int y) {
        return (x - 1) / y + 1;
    }

    public List<IRLoop> getLoops() {
        return loops;
    }

    public List<IRLoop> getTopLevelLoops() {
        return topLevelLoops;
    }

    public IRLoop getLoop(BasicBlock bb) {
        return loopMap.get(bb);
    }

    public Integer getLoopDepth(BasicBlock bb) {
        if (loopMap.get(bb) == null) {
            return 0;
        } else {
            return loopMap.get(bb).getLoopDepth();
        }
    }

    public void removeLoop(IRLoop loop) {
        ArrayList<BasicBlock> loopBlocks = new ArrayList<>(loop.getBlocks());
        if (loop.getParentLoop() != null) {
            IRLoop parentLoop = loop.getParentLoop();
            for (BasicBlock bb : loopBlocks) {
                if (loopMap.get(bb) == loop) {
                    loopMap.put(bb, parentLoop);
                }
            }

            parentLoop.removeSubLoop(loop);

            while (!loop.getSubLoops().isEmpty()) {
                IRLoop subLoop = loop.getSubLoops().get(0);
                parentLoop.addSubLoop(subLoop);
                loop.removeSubLoop(subLoop);
            }
        } else {
            for (BasicBlock bb : loopBlocks) {
                if (loopMap.get(bb) == loop) {
                    removeBlockFromAllLoops(bb);
                }
            }

            topLevelLoops.remove(loop);
            while (!loop.getSubLoops().isEmpty()) {
                IRLoop subLoop = loop.getSubLoops().get(0);
                topLevelLoops.add(subLoop);
                loop.removeSubLoop(subLoop);
            }
        }
    }

    private void removeBlockFromAllLoops(BasicBlock bb) {
        IRLoop loop = loopMap.get(bb);
        while (loop != null) {
            loop.getBlocks().remove(bb);
            loop = loop.getParentLoop();
        }
        loopMap.remove(bb);
    }
}
