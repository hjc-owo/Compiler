package pass.ir;

import ir.IRModule;
import ir.analysis.AliasAnalysis;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVar;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;
import pass.Pass;
import utils.INode;

public class InterProceduralAnalysis implements Pass.IRPass {
    @Override
    public String getName() {
        return "InterProceduralAnalysis";
    }

    @Override
    public void run(IRModule m) {
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function f = funcEntry.getValue();
            f.setHasSideEffect(f.isLibraryFunction());
            f.setUseGlobalVar(f.isLibraryFunction());
        }
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            Function f = funcEntry.getValue();
            for (INode<BasicBlock, Function> bbEntry : f.getList()) {
                for (INode<Instruction, BasicBlock> instrEntry : bbEntry.getValue().getInstructions()) {
                    Instruction instr = instrEntry.getValue();
                    if (instr instanceof LoadInst) {
                        Value addr = ((LoadInst) instr).getPointer();
                        if (addr instanceof AllocaInst &&
                                (((AllocaInst) addr).getAllocaType() instanceof IntegerType)) {
                            continue;
                        }
                        Value pt = AliasAnalysis.getArrayValue(addr);
                        if (AliasAnalysis.isGlobal(pt)) {
                            f.setUseGlobalVar(true);
                            if (((PointerType) pt.getType()).getTargetType() instanceof IntegerType) {
                                f.getLoadGVs().add((GlobalVar) pt);
                            }
                        }
                    } else if (instr instanceof StoreInst) {
                        Value addr = ((StoreInst) instr).getPointer();
                        if (addr instanceof AllocaInst &&
                                (((AllocaInst) addr).getAllocaType() instanceof IntegerType)) {
                            continue;
                        }
                        Value pt = AliasAnalysis.getArrayValue(addr);
                        if (AliasAnalysis.isGlobal(pt) || AliasAnalysis.isParam(pt)) {
                            f.setHasSideEffect(true);
                            if (((PointerType) pt.getType()).getTargetType() instanceof IntegerType) {
                                f.getStoreGVs().add((GlobalVar) pt);
                            }
                        }
                    }
                }
            }
        }
        // 递归标记
        for (INode<Function, IRModule> funcEntry : m.getFunctions()) {
            if (funcEntry.getValue().hasSideEffect()) {
                markSideEffect(funcEntry.getValue());
            }
            if (funcEntry.getValue().isUseGlobalVar()) {
                markUseGV(funcEntry.getValue());
            }
        }
    }

    private void markSideEffect(Function f) {
        for (Function pre : f.getPredecessors()) {
            pre.getStoreGVs().addAll(f.getStoreGVs());
            if (!pre.hasSideEffect()) {
                pre.setHasSideEffect(true);
                markSideEffect(pre);
            }
        }
    }

    private void markUseGV(Function f) {
        for (Function pre : f.getPredecessors()) {
            pre.getLoadGVs().addAll(f.getLoadGVs());
            if (!pre.isUseGlobalVar()) {
                pre.setUseGlobalVar(true);
                markUseGV(pre);
            }
        }
    }


}
