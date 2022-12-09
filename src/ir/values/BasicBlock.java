package ir.values;

import ir.analysis.AliasAnalysis;
import ir.types.FunctionType;
import ir.types.LabelType;
import ir.types.VoidType;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.PhiInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.BrInst;
import ir.values.instructions.terminator.CallInst;
import ir.values.instructions.terminator.RetInst;
import utils.IList;
import utils.INode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BasicBlock extends Value {
    private IList<Instruction, BasicBlock> instructions; // 这个 BasicBlock 中的指令列表
    private INode<BasicBlock, Function> node; // 这个 BasicBlock 所属的 Function
    private List<BasicBlock> predecessors; // 这个 BasicBlock 的前驱 BasicBlock
    private List<BasicBlock> successors; // 这个 BasicBlock 的后继 BasicBlock
    private int domLevel;

    public BasicBlock(Function function) {
        super(String.valueOf(REG_NUMBER++), new LabelType());
        this.instructions = new IList<>(this);
        this.node = new INode<>(this);
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.node.insertAtEnd(function.getList());
    }

    public IList<Instruction, BasicBlock> getInstructions() {
        return instructions;
    }

    public void setInstructions(IList<Instruction, BasicBlock> instructions) {
        this.instructions = instructions;
    }

    public INode<BasicBlock, Function> getNode() {
        return node;
    }

    public void setNode(INode<BasicBlock, Function> node) {
        this.node = node;
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public void setPredecessors(List<BasicBlock> predecessors) {
        this.predecessors = predecessors;
    }

    public void addPredecessor(BasicBlock predecessor) {
        this.predecessors.add(predecessor);
    }

    public List<BasicBlock> getSuccessors() {
        return successors;
    }

    public void setSuccessors(List<BasicBlock> successors) {
        this.successors = successors;
    }

    public void addSuccessor(BasicBlock successor) {
        this.successors.add(successor);
    }

    public Function getParent() {
        return this.node.getParent().getValue();
    }

    public void setDomLevel(int domLevel) {
        this.domLevel = domLevel;
    }

    public int getDomLevel() {
        return domLevel;
    }

    public void refreshReg() {
        for (INode<Instruction, BasicBlock> inode : this.instructions) {
            Instruction inst = inode.getValue();
            if (!(inst instanceof AliasAnalysis.MemPhi || inst instanceof AliasAnalysis.LoadDepInst ||inst instanceof StoreInst || inst instanceof BrInst || inst instanceof RetInst ||
                    (inst instanceof CallInst && ((FunctionType) inst.getOperands().get(0).getType()).getReturnType() instanceof VoidType))) {
                inst.setName("%" + REG_NUMBER++);
            }
        }
    }

    public String getLabelName() {
        return "label_" + getId();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (INode<Instruction, BasicBlock> instruction : this.instructions) {
            s.append("\t").append(instruction.getValue().toString()).append("\n");
        }
        return s.toString();
    }

    public void removeSelf() {
        for (BasicBlock bb : this.getPredecessors()) {
            bb.getSuccessors().removeIf(basicBlock -> basicBlock.equals(this));
        }
        for (BasicBlock bb : this.getSuccessors()) {
            bb.getPredecessors().forEach(pred -> {
                if (pred.equals(this)) {
                    removeBasicBlockSucc(this, bb);
                }
            });
            bb.getPredecessors().removeIf(basicBlock -> basicBlock.equals(this));
        }

        for (INode<Instruction, BasicBlock> instNode : getInstructions()) {
            Instruction inst = instNode.getValue();
            inst.removeUseFromOperands();
        }
        this.getNode().removeFromList();
    }

    public void removeBasicBlockSucc(BasicBlock pred, BasicBlock succ) {
        HashSet<Integer> idx = new HashSet<>();
        idx.add(succ.getPredecessors().indexOf(pred));
        INode<Instruction, BasicBlock> instNode = succ.getInstructions().getBegin();
        while (instNode != null) {
            INode<Instruction, BasicBlock> ninstNode = instNode.getNext();
            Instruction inst = instNode.getValue();
            if (!(inst instanceof PhiInst)) {
                break;
            }

            if (inst.getOperands().size() == 1) {
                inst.replaceUsedWith(inst.getOperands().get(0));
                inst.removeUseFromOperands();
                instNode.removeFromList();
            } else {
                inst.removeNumberOperand(idx);
            }

            instNode = ninstNode;
        }
    }

    public void copyAllFrom(BasicBlock source, Map<Value, Value> replaceMap) {
        for (INode<Instruction, BasicBlock> instNode : source.getInstructions()) {
            Instruction instruction = instNode.getValue();
            Instruction insertInst = instruction.copySelf(replaceMap);
            replaceMap.put(instruction, insertInst);
        }
    }

    public int getLoopDepth() {
        return node.getParent().getValue().getLoopInfo().getLoopDepth(this);
    }
}
