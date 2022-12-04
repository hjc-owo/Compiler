package pass.ir;

import ir.IRModule;
import ir.analysis.AliasAnalysis;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVar;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.CallInst;
import pass.Pass;
import utils.INode;

import java.util.HashSet;

public class MarkConstArray implements Pass.IRPass {
    @Override
    public String getName() {
        return "markConstArray";
    }

    private final HashSet<GlobalVar> unChangedArray = new HashSet<>();

    @Override
    public void run(IRModule m) {
        for (GlobalVar i : m.getGlobalVars()) {
            if (!i.isConst() && ((PointerType) i.getType()).getTargetType() instanceof ArrayType) {
                unChangedArray.add(i);
            }
        }
        for (INode<Function, IRModule> i : m.getFunctions()) {
            Function function = i.getValue();
            if (function.isLibraryFunction()) {
                continue;
            }
            checkUsedValue(function);
        }
        for (GlobalVar i : unChangedArray) {
            i.setConst(true);
        }
    }

    private void checkUsedValue(Function func) {
        for (INode<BasicBlock, Function> i : func.getList()) {
            BasicBlock curBlock = i.getValue();
            for (INode<Instruction, BasicBlock> j : curBlock.getInstructions()) {

                Instruction instr = j.getValue();
                if (instr instanceof StoreInst) {
                    Value pointer = AliasAnalysis.getArrayValue(((StoreInst) instr).getPointer());
                    if (pointer instanceof GlobalVar && unChangedArray.contains((GlobalVar) pointer)) {
                        unChangedArray.remove(pointer);
                    }
                } else if (instr instanceof CallInst) {
                    Value callfunc = instr.getOperands().get(0);
                    if (((Function) callfunc).hasSideEffect()) {
                        for (Value arg : instr.getOperands()) {
                            if (arg instanceof GEPInst) {
                                Value pointer = AliasAnalysis.getArrayValue(((GEPInst) arg).getPointer());
                                if (pointer instanceof GlobalVar && unChangedArray.contains((GlobalVar) pointer)) {
                                    unChangedArray.remove(pointer);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}