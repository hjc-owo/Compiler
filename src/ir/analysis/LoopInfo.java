package ir.analysis;

import ir.IRLoop;
import ir.values.BasicBlock;
import ir.values.Function;

import java.util.*;

public class LoopInfo {

    private final Map<BasicBlock, IRLoop> loopMap = new HashMap<>();
    private final List<IRLoop> loops = new ArrayList<>();
    private final List<IRLoop> topLevelLoops = new ArrayList<>();
    private final Function f;

    public LoopInfo(Function f) {
        this.f = f;
    }

    public IRLoop getLoop(BasicBlock bb) {
        return loopMap.get(bb);
    }

    public List<IRLoop> getLoops() {
        return loops;
    }

    public Integer getLoopDepth(BasicBlock bb) {
        if (loopMap.get(bb) == null) {
            return 0;
        } else {
            return loopMap.get(bb).getLoopDepth();
        }
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
}
