package pass.ir;

import ir.IRModule;
import ir.analysis.AliasAnalysis;
import ir.types.ArrayType;
import ir.values.*;
import ir.values.instructions.ConvInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.CallInst;
import pass.Pass;
import utils.INode;

import java.util.*;

public class LocalArrayPromotion implements Pass.IRPass {
    @Override
    public String getName() {
        return "localArrayPromotion";
    }

    private IRModule m;
    private final BuildFactory f = BuildFactory.getInstance();

    @Override
    public void run(IRModule m) {
        this.m = m;
        // try to find all local array
        for (INode<Function, IRModule> i : m.getFunctions()) {
            Function func = i.getValue();
            if (func.isLibraryFunction()) continue;
            for (INode<BasicBlock, Function> j : func.getList()) {
                BasicBlock bb = j.getValue();
                for (INode<Instruction, BasicBlock> k : bb.getInstructions()) {
                    Instruction instr = k.getValue();
                    if (instr instanceof AllocaInst) {
                        if ((((AllocaInst) instr).getAllocaType() instanceof ArrayType)) {
                            // find it and judge if it can be promotion
                            curArray = (ArrayType) ((AllocaInst) instr).getAllocaType();
                            promotion((AllocaInst) instr);
                        }
                    }
                }
            }
        }
    }

    private final String nameBasic = "hjc_owo";
    private static int top = 0;

    private void promotion(AllocaInst allocaInst) {
        // first check if it can be promotion
        // all gep and store instr should be const
        // no load before store
        // and if that it can be initival like global var
        // 1 check stores
        saveValue = new HashMap<>();
        storeCount = 0;
        saveMemset = null;
        if (!checkStores(allocaInst)) {
            return;
        }
        // 2 check no load before store
        if (!checkLoad(allocaInst)) {
            return;
        }
        // promote
        String name = nameBasic + top++;
        GlobalVar gv = f.buildGlobalArray(name, curArray, false);
        for (Integer offset : saveValue.keySet()) {
            f.buildInitArray(gv, offset, saveValue.get(offset));
        }
        for (StoreInst i : KeepStores) {
            i.removeUseFromOperands();
            i.getNode().removeFromList();
        }
        if (saveMemset != null) {
            saveMemset.removeUseFromOperands();
            saveMemset.getNode().removeFromList();
        }
        allocaInst.replaceUsedWith(gv);
    }

    // pre for init array
    private ArrayType curArray = null;
    // recode where is what
    private Map<Integer, Const> saveValue = new HashMap<>();
    // to avoid store at same position
    private int storeCount = 0;
    private List<StoreInst> KeepStores = new ArrayList<>();

    private boolean checkStores(AllocaInst alloc) {
        KeepStores = new ArrayList<>();
        Function func = alloc.getParent().getParent();
        for (INode<BasicBlock, Function> i : func.getList()) {
            BasicBlock bb = i.getValue();
            for (INode<Instruction, BasicBlock> j : bb.getInstructions()) {
                Instruction instr = j.getValue();
                if (instr instanceof StoreInst) {
                    Value pointer = ((StoreInst) (instr)).getPointer();
                    //check if the pointer is an array
                    // and equals to alloc
                    if (pointer instanceof GEPInst) {
                        Value array = AliasAnalysis.getArrayValue(pointer);
                        if (Objects.equals(array, alloc)) {
                            // store value and store position must be const
                            if (!(((StoreInst) (instr)).getValue() instanceof Const)) {
                                return false;
                            }
                            ArrayList<Integer> indexs = new ArrayList<>();
                            storeCount++;
                            for (int k = 1; k < ((GEPInst) pointer).getOperands().size(); k++) {
                                Value index = ((GEPInst) pointer).getOperands().get(k);
                                if (!(index instanceof Const)) {
                                    return false;
                                }
                                indexs.add(((ConstInt) (index)).getValue());
                            }
                            int offset = curArray.index2Offset(indexs);
                            saveValue.put(offset, (Const) ((StoreInst) instr).getValue());
                            KeepStores.add((StoreInst) instr);
                        }
                    }
                }
            }
        }
        return true;
    }

    private CallInst saveMemset = null;

    private boolean checkLoad(AllocaInst alloc) {
        // do not use "for" to visit all block
        // use suc to ensure visit has right order
        // so use bfs
        List<BasicBlock> line = new ArrayList<>();
        Set<BasicBlock> visit = new HashSet<>();
        line.add(alloc.getParent());
        int store = 0;
        int i = 0;
        while (i < line.size()) {
            BasicBlock curBlock = line.get(i);
            if (!visit.contains(curBlock)) {
                visit.add(curBlock);
            } else {
                i++;
                continue;
            }
            // check b
            for (INode<Instruction, BasicBlock> j : curBlock.getInstructions()) {
                Instruction instr = j.getValue();
                // call is see as load
                if (instr instanceof StoreInst) {
                    Value pointer = ((StoreInst) instr).getPointer();
                    if (pointer instanceof GEPInst) {
                        Value array = AliasAnalysis.getArrayValue(pointer);
                        if (Objects.equals(array, alloc)) {
                            store++;
                        }
                    }
                }

                if (instr instanceof LoadInst) {
                    if (store == storeCount) return true;
                    Value pointer = ((LoadInst) instr).getPointer();
                    if (pointer instanceof GEPInst) {
                        Value array = AliasAnalysis.getArrayValue(pointer);
                        if (Objects.equals(array, alloc)) {
                            return false;
                        }
                    }
                }
                if (instr instanceof CallInst) {
                    Function func = ((CallInst) instr).getCalledFunction();
                    if (!func.getName().equals("memset")) {
                        if (store == storeCount) return true;
                        for (Value k : instr.getOperands()) {
                            if (k instanceof GEPInst) {
                                Value array = AliasAnalysis.getArrayValue(k);
                                if (Objects.equals(array, alloc)) {
                                    return false;
                                }
                            }
                        }
                    } else {
                        Value aim = instr.getOperands().get(1);
                        if (aim instanceof ConvInst)
                            aim = ((ConvInst) aim).getOperands().get(0);
                        Value array = AliasAnalysis.getArrayValue(aim);
                        if (alloc.equals(array)) saveMemset = (CallInst) instr;
                        // call instr only has one and need to remove
                    }
                }
            }

            for (BasicBlock j : curBlock.getSuccessors()) {
                if (!visit.contains(j)) {
                    line.add(j);
                }
            }
            i++;
        }
        return true;
    }

}
