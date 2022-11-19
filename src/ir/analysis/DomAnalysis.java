package ir.analysis;

import ir.values.BasicBlock;
import ir.values.Function;

import java.util.*;

public class DomAnalysis {

    //计算支配边界
    public static Map<BasicBlock, Set<BasicBlock>> analyzeDom(Function function) {
        // 提取出blocks
        Map<BasicBlock, Set<BasicBlock>> dom = new HashMap<>();
        Map<BasicBlock, BasicBlock> idom = new HashMap<>();
        Map<BasicBlock, List<BasicBlock>> idoms = new HashMap<>();
        Map<BasicBlock, Set<BasicBlock>> df = new HashMap<>();
        List<BasicBlock> allBlocks = new ArrayList<>();
        LinkedList<BasicBlock> passBlock = new LinkedList<>();
        passBlock.add(function.getList().getBegin().getValue());
        allBlocks.add(function.getList().getBegin().getValue());
        while (!passBlock.isEmpty()) {
            BasicBlock ref = passBlock.poll();
            for (BasicBlock bb : ref.getSuccessors()) {
                if (!allBlocks.contains(bb)) {
                    allBlocks.add(bb);
                    passBlock.add(bb);
                }
            }
        }
        // 初始化dom
        for (BasicBlock basicBlock : allBlocks) {
            dom.put(basicBlock, null);
            idoms.put(basicBlock, new ArrayList<>());
            df.put(basicBlock, new HashSet<>());
        }
        dom.replace(function.getList().getBegin().getValue(), new HashSet<>());
        dom.get(function.getList().getBegin().getValue()).add(function.getList().getBegin().getValue());
        // 计算dom
        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock basicBlock : allBlocks) {
                //得到前驱块的dom集合的交集
                Set<BasicBlock> temPred = null;
                for (BasicBlock bb : basicBlock.getPredecessors()) {
                    if (dom.get(bb) == null) {
                        continue;
                    }
                    if (temPred == null) {
                        temPred = new HashSet<>(dom.get(bb));
                    } else {
                        Set<BasicBlock> temp = new HashSet<>();
                        for (BasicBlock block : dom.get(bb)) {
                            if (temPred.contains(block)) {
                                temp.add(block);
                            }
                        }
                        temPred = temp;
                    }
                }
                if (temPred == null) {
                    temPred = new HashSet<>();
                }
                Set<BasicBlock> temBlocks = new HashSet<>(temPred);
                temBlocks.add(basicBlock);
                if (!temBlocks.equals(dom.get(basicBlock))) {
                    dom.replace(basicBlock, new HashSet<>(temBlocks));
                    changed = true;
                }
            }
        }
        function.setDom(dom);
        // 计算idom
        for (BasicBlock basicBlock : dom.keySet()) {
            Set<BasicBlock> tmpDomSet = dom.get(basicBlock);
            if (tmpDomSet.size() == 1) {
                idom.put(basicBlock, null);
            }
            for (BasicBlock mayIDom : tmpDomSet) {
                if (mayIDom.equals(basicBlock)) {
                    continue;
                }
                boolean isIDom = true;
                for (BasicBlock tmpDomBlock : tmpDomSet) {
                    if (!tmpDomBlock.equals(basicBlock) &&
                            !tmpDomBlock.equals(mayIDom) &&
                            dom.get(tmpDomBlock).contains(mayIDom)) {
                        isIDom = false;
                        break;
                    }
                }
                if (isIDom) {
                    idom.put(basicBlock, mayIDom);
                    idoms.get(mayIDom).add(basicBlock);
                    break;
                }
            }

        }
        function.setIdom(idom);
        function.setIdoms(idoms);
        computeDominanceLevel(function.getList().getBegin().getValue(), 0, function);

        // 计算DF
        for (BasicBlock basicBlock : allBlocks) {
            if (basicBlock.getPredecessors().size() > 1) {
                for (BasicBlock p : basicBlock.getPredecessors()) {
                    BasicBlock runner = p;
                    while (!runner.equals(idom.get(basicBlock))
                            && df.containsKey(runner)) {
                        df.get(runner).add(basicBlock);
                        runner = idom.get(runner);
                    }
                }
            }
        }
        return df;
    }

    private static void computeDominanceLevel(BasicBlock basicBlock, int domLevel, Function function) {
        basicBlock.setDomLevel(domLevel);
        for (BasicBlock succ : function.getIdoms().get(basicBlock)) {
            computeDominanceLevel(succ, domLevel + 1, function);
        }
    }

}
