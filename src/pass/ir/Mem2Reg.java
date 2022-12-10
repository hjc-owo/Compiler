package pass.ir;

import ir.IRModule;
import ir.analysis.DomAnalysis;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.instructions.Instruction;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.PhiInst;
import ir.values.instructions.mem.StoreInst;
import pass.Pass;
import utils.INode;

import java.util.*;

public class Mem2Reg implements Pass.IRPass {

    @Override
    public String getName() {
        return "Mem2Reg";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> f : m.getFunctions()) {
            if (!f.getValue().isLibraryFunction()) {
                mem2reg(f.getValue());
            }
        }
    }

    private void mem2reg(Function function) {
        Map<AllocaInst, List<BasicBlock>> defMap = new HashMap<>();
        List<AllocaInst> defArraylist = new ArrayList<>();
        Map<BasicBlock, Set<BasicBlock>> DFMap = DomAnalysis.analyzeDom(function);
        Map<PhiInst, AllocaInst> phiAllocaInstHashMap = new HashMap<>();

        Stack<RenameBlock> renameBlockStack = new Stack<>();
        List<Value> values = new ArrayList<>();
        // initial the variables' defs
        for (INode<BasicBlock, Function> basicBlockINode : function.getList()) {
            BasicBlock basicBlock = basicBlockINode.getValue();
            for (INode<Instruction, BasicBlock> instructionINode : basicBlock.getInstructions()) {
                Instruction instruction = instructionINode.getValue();

                if (instruction.getOperator().equals(Operator.Alloca)) {
                    AllocaInst allocaInst = (AllocaInst) instruction;

                    if (allocaInst.getAllocaType() instanceof IntegerType) {
                        defMap.put(allocaInst, new ArrayList<>());
                        defArraylist.add(allocaInst);
                    }

                }
            }
        }

        // initial the variables' store
        for (INode<BasicBlock, Function> basicBlockINode : function.getList()) {
            BasicBlock basicBlock = basicBlockINode.getValue();
            for (INode<Instruction, BasicBlock> instructionINode : basicBlock.getInstructions()) {
                Instruction instruction = instructionINode.getValue();

                if (instruction.getOperator().equals(Operator.Store)) {
                    StoreInst storeInst = (StoreInst) instruction;
                    if (!(storeInst.getOperands().get(1) instanceof Instruction)) {
                        continue;
                    }
                    Instruction targetInst = (Instruction) storeInst.getOperands().get(1);
                    if (targetInst instanceof AllocaInst) {
                        if (defMap.containsKey((AllocaInst) targetInst)) {
                            defMap.get((AllocaInst) targetInst).add(basicBlock);
                        }
                    }
                }
            }
        }

        Map<AllocaInst, List<BasicBlock>> tmpDefMap = new HashMap<>(defMap);

        for (AllocaInst allocaInst : defMap.keySet()) {
            if (defMap.get(allocaInst).isEmpty()) {
                tmpDefMap.remove(allocaInst);
                defArraylist.remove(allocaInst);
            }
        }

        defMap = tmpDefMap;
        // get block to insert phi
        Map<BasicBlock, Boolean> visited = new HashMap<>();
        for (INode<BasicBlock, Function> basicBlockNode : function.getList()) {
            visited.put(basicBlockNode.getValue(), false);
        }
        for (AllocaInst allocaInst : defArraylist) {
            Queue<BasicBlock> workList = new LinkedList<>(defMap.get(allocaInst));
            Map<BasicBlock, Boolean> placed = new HashMap<>();

            // initial visited and placed
            for (INode<BasicBlock, Function> basicBlockNode : function.getList()) {
                visited.put(basicBlockNode.getValue(), false);
                placed.put(basicBlockNode.getValue(), false);
            }

            while (!workList.isEmpty()) {
                BasicBlock X = workList.remove();
                Set<BasicBlock> DominanceFrontier_X = DFMap.get(X);
                for (BasicBlock Y : DominanceFrontier_X) {
                    if (!placed.get(Y)) {
                        placed.replace(Y, true);
                        List<Value> tmpValues = new ArrayList<>();
                        if (((PointerType) allocaInst.getType()).getTargetType() instanceof IntegerType) {
                            tmpValues = new ArrayList<>(Collections.nCopies(Y.getPredecessors().size(), ConstInt.ZERO));
                        }
                        PhiInst phiInst = BuildFactory.getInstance().buildPhi(Y, ((PointerType) allocaInst.getType()).getTargetType(), tmpValues);
                        System.out.println(phiInst);
                        phiAllocaInstHashMap.put(phiInst, allocaInst);

                        if (!visited.get(Y)) {
                            visited.replace(Y, true);
                            workList.add(Y);
                        }
                    }
                }
            }
        }

        // variable renaming
        visited.replaceAll((key, value) -> false);

        // 对每一个values设置一个value值
        for (AllocaInst ignored : defArraylist) {
            values.add(ConstInt.ZERO);
        }
        RenameBlock entryBlock = new RenameBlock(function.getList().getBegin().getValue(), null, values);
        renameBlockStack.push(entryBlock);

        // loop begin
        while (!renameBlockStack.isEmpty()) {
            RenameBlock loopBlock = renameBlockStack.pop();

            BasicBlock curBasicBlock = loopBlock.basicBlock;
            BasicBlock predBlock = loopBlock.predecessor;
            List<Value> tmpValues = new ArrayList<>(loopBlock.values);

            // search phi
            for (INode<Instruction, BasicBlock> instructionNode : curBasicBlock.getInstructions()) {
                if (instructionNode.getValue().getOperator().equals(Operator.Phi)) {
                    PhiInst phiInst = (PhiInst) instructionNode.getValue();
                    if (phiAllocaInstHashMap.containsKey(phiInst)) {
                        phiInst.replaceOperands(curBasicBlock.getPredecessors().indexOf(predBlock),
                                tmpValues.get(defArraylist.indexOf(phiAllocaInstHashMap.get(phiInst))));
                    }
                }
            }

            // handle every inst
            if (visited.get(curBasicBlock)) {
                continue;
            }
            visited.replace(curBasicBlock, true);
            for (INode<Instruction, BasicBlock> instructionNode = curBasicBlock.getInstructions().getBegin(); instructionNode != null; ) {
                Instruction instruction = instructionNode.getValue();
                INode<Instruction, BasicBlock> next = instructionNode.getNext();

                // alloca will be removed
                switch (instruction.getOperator()) {
                    case Alloca:
                        if (defMap.containsKey((AllocaInst) instruction)) {
                            instructionNode.removeFromList();
                        }
                        break;
                    case Load: {
                        LoadInst loadInst = (LoadInst) instruction;
                        if (!(loadInst.getOperands().get(0) instanceof AllocaInst)) {
                            instructionNode = next;
                            continue;
                        }
                        AllocaInst allocaInst = (AllocaInst) loadInst.getOperands().get(0);
                        if (!(allocaInst.getAllocaType() instanceof IntegerType)) {
                            instructionNode = next;
                            continue;
                        }
                        loadInst.replaceUsedWith(tmpValues.get(defArraylist.indexOf(allocaInst)));
                        instructionNode.removeFromList();
                        instruction.removeUseFromOperands();
                    }
                    break;
                    case Store: {
                        StoreInst storeInst = (StoreInst) instruction;
                        if (!(storeInst.getOperands().get(1) instanceof AllocaInst)) {
                            instructionNode = next;
                            continue;
                        }
                        AllocaInst allocaInst = (AllocaInst) storeInst.getOperands().get(1);
                        if (!(allocaInst.getAllocaType() instanceof IntegerType)) {
                            instructionNode = next;
                            continue;
                        }
                        tmpValues.set(defArraylist.indexOf(allocaInst), storeInst.getOperands().get(0));
                        storeInst.replaceUsedWith(tmpValues.get(defArraylist.indexOf(allocaInst)));
                        instructionNode.removeFromList();
                        instruction.removeUseFromOperands();
                    }
                    break;
                    case Phi:
                        PhiInst phiInst = (PhiInst) instruction;
                        if (phiAllocaInstHashMap.containsKey(phiInst)) {
                            tmpValues.set(defArraylist.indexOf(phiAllocaInstHashMap.get(phiInst)), phiInst);
                        }
                        break;
                }
                instructionNode = next;
            }

            for (BasicBlock bb : curBasicBlock.getSuccessors()) {
                renameBlockStack.push(new RenameBlock(bb, curBasicBlock, tmpValues));
            }
        }
    }

    private static class RenameBlock {
        private final BasicBlock basicBlock;
        private final BasicBlock predecessor;
        private final List<Value> values;

        public RenameBlock(BasicBlock basicBlock, BasicBlock predecessor, List<Value> values) {
            this.basicBlock = basicBlock;
            this.predecessor = predecessor;
            this.values = new ArrayList<>(values);
        }

    }
}
