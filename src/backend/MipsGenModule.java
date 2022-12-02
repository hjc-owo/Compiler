package backend;

import ir.IRModule;
import ir.types.*;
import ir.values.*;
import ir.values.instructions.*;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;
import ir.values.instructions.terminator.BrInst;
import ir.values.instructions.terminator.CallInst;
import ir.values.instructions.terminator.RetInst;
import utils.INode;
import utils.IOUtils;
import utils.Pair;

import java.util.*;

public class MipsGenModule {
    private static final MipsGenModule instance = new MipsGenModule();
    private IRModule irModule;

    private MipsGenModule() {
    }

    public static MipsGenModule getInstance() {
        return instance;
    }

    public void loadIR() {
        irModule = IRModule.getInstance();
    }

    public void genMips() {
        IOUtils.mips(".data\n");
        for (GlobalVar globalVar : irModule.getGlobalVars()) {
            if (globalVar.isString()) {
                ConstString constString = (ConstString) globalVar.getValue();
                IOUtils.mips("\n# " + globalVar + "\n\n");
                IOUtils.mips(globalVar.getStringName() + ": .asciiz " + constString.getName() + "\n");
            }
        }
        IOUtils.mips("\n.text\n");
        for (GlobalVar globalVar : irModule.getGlobalVars()) {
            if (!globalVar.isString()) {
                IOUtils.mips("\n# " + globalVar + "\n\n");
            }
            if (globalVar.isInt()) {
                getGp(globalVar.getUniqueName());
                ConstInt constInt = (ConstInt) globalVar.getValue();
                load("$t2", String.valueOf(constInt.getValue()));
                store("$t2", globalVar.getUniqueName());
            } else if (globalVar.isArray()) {
                ConstArray constArray = (ConstArray) globalVar.getValue();
                getGpArray(globalVar.getUniqueName(), 4 * constArray.getCapacity());
                if (constArray.isInit()) {
                    List<Value> values = constArray.get1DArray();
                    for (int i = 0; i < values.size(); i++) {
                        Value value = values.get(i);
                        if (value instanceof ConstInt) {
                            ConstInt constInt = (ConstInt) value;
                            IOUtils.mips("\n");
                            load("$t2", String.valueOf(constInt.getValue()));
                            load("$t0", globalVar.getUniqueName());
                            IOUtils.mips("addu $t0, $t0, " + (4 * i) + "\n");
                            store("$t2", "$t0", 0);
                        }
                    }
                }
            }
        }
        IOUtils.mips("\njal main\n");
        IOUtils.mips("li $v0, 10\n");
        IOUtils.mips("syscall\n\n");

        for (INode<Function, IRModule> funcEntry : irModule.getFunctions()) {
            Function function = funcEntry.getValue();
            if (function.isLibraryFunction()) {
                continue;
            }
            IOUtils.mips("\n" + function.getName() + ":\n");
            rec = function.getArguments().size();
            for (int i = 0; rec > 0; i++) {
                rec--;
                load("$t0", "$sp", 4 * rec);
                getSp(function.getArguments().get(i).getUniqueName());
                store("$t0", function.getArguments().get(i).getUniqueName());
            }
            rec = 0;
            for (INode<BasicBlock, Function> blockEntry : function.getList()) {
                BasicBlock basicBlock = blockEntry.getValue();
                IOUtils.mips("\n" + basicBlock.getLabelName() + ":\n");
                for (INode<Instruction, BasicBlock> instEntry : basicBlock.getInstructions()) {
                    Instruction ir = instEntry.getValue();
                    IOUtils.mips("\n# " + ir.toString() + "\n\n");
                    if (!(ir instanceof AllocaInst)) {
                        getSp(ir.getUniqueName());
                    }
                    translate(ir);
                }
            }
        }
    }


    private Map<String, Pair<String, Integer>> mem = new HashMap<>();
    int gpOff = 0, spOff = 0, rec = 0;

    private void getGp(String name) {
        if (mem.containsKey(name)) {
            return;
        }
        mem.put(name, new Pair<>("$gp", gpOff));
        gpOff += 4;
    }

    private void getGpArray(String name, int offset) {
        if (mem.containsKey(name)) {
            return;
        }
        getGp(name);
        IOUtils.mips("addu $t0, $gp, " + gpOff + "\n");
        store("$t0", name);
        gpOff += offset;
    }

    private void getSp(String name) {
        if (mem.containsKey(name)) {
            return;
        }
        spOff -= 4;
        mem.put(name, new Pair<>("$sp", spOff));
    }

    private void getSpArray(String name, int offset) {
        if (mem.containsKey(name)) {
            return;
        }
        getSp(name);
        spOff -= offset;
        IOUtils.mips("addu $t0, $sp, " + spOff + "\n");
        store("$t0", name);
    }

    private void translate(Instruction ir) {
        if (ir instanceof BinaryInst) parseBinary((BinaryInst) ir);
        else if (ir instanceof CallInst) parseCall((CallInst) ir);
        else if (ir instanceof RetInst) parseRet((RetInst) ir);
        else if (ir instanceof AllocaInst) parseAlloca((AllocaInst) ir);
        else if (ir instanceof LoadInst) parseLoad((LoadInst) ir);
        else if (ir instanceof StoreInst) parseStore((StoreInst) ir);
        else if (ir instanceof GEPInst) parseGEP((GEPInst) ir);
        else if (ir instanceof BrInst) parseBr((BrInst) ir);
        else if (ir instanceof ConvInst) parseConv((ConvInst) ir);
    }

    private void parseBinary(BinaryInst b) {
        if (b.isAdd()) calc(b, "addu");
        else if (b.isSub()) calc(b, "subu");
        else if (b.isMul()) calc(b, "mul");
        else if (b.isDiv()) calc(b, "div");
        else if (b.isMod()) calc(b, "rem");
        else if (b.isShl()) calc(b, "sll");
        else if (b.isShr()) calc(b, "srl");
        else if (b.isAnd()) calc(b, "and");
        else if (b.isOr()) calc(b, "or");
        else if (b.isLe()) calc(b, "sle");
        else if (b.isLt()) calc(b, "slt");
        else if (b.isGe()) calc(b, "sge");
        else if (b.isGt()) calc(b, "sgt");
        else if (b.isEq()) calc(b, "seq");
        else if (b.isNe()) calc(b, "sne");
        else if (b.isNot()) {
            load("$t0", b.getOperand(0).getUniqueName());
            IOUtils.mips("not $t1, $t0\n");
            store("$t1", b.getUniqueName());
        }
    }

    private void calc(BinaryInst b, String op) {
        load("$t0", b.getOperand(0).getUniqueName());
        load("$t1", b.getOperand(1).getUniqueName());
        IOUtils.mips(op + " $t2, $t0, $t1\n");
        store("$t2", b.getUniqueName());
    }

    private void parseCall(CallInst callInst) {
        Function function = callInst.getCalledFunction();
        if (function.isLibraryFunction()) {
            if (Objects.equals(function.getName(), "getint")) {
                IOUtils.mips("li $v0, 5\n");
                IOUtils.mips("syscall\n");
                store("$v0", callInst.getUniqueName());
            } else if (Objects.equals(function.getName(), "putint")) {
                load("$a0", callInst.getOperand(1).getUniqueName());
                IOUtils.mips("li $v0, 1\n");
                IOUtils.mips("syscall\n");
            } else if (Objects.equals(function.getName(), "putch")) {
                load("$a0", callInst.getOperand(1).getUniqueName());
                IOUtils.mips("li $v0, 11\n");
                IOUtils.mips("syscall\n");
            } else if (Objects.equals(function.getName(), "putstr")) {
                IOUtils.mips("li $v0, 4\n");
                IOUtils.mips("syscall\n");
            }
        } else {
            store("$ra", "$sp", spOff - 4);
            rec = 1;
            int argSize = callInst.getCalledFunction().getArguments().size();
            for (int i = 1; i <= argSize; i++) {
                rec++;
                load("$t0", callInst.getOperand(i).getUniqueName());
                store("$t0", "$sp", spOff - rec * 4);
            }
            IOUtils.mips("addu $sp, $sp, " + (spOff - rec * 4) + "\n");
            IOUtils.mips("jal " + function.getName() + "\n");
            IOUtils.mips("addu $sp, $sp, " + (-spOff + rec * 4) + "\n");
            load("$ra", "$sp", spOff - 4);
            if (!(((FunctionType) function.getType()).getReturnType() instanceof VoidType)) {
                store("$v0", callInst.getUniqueName());
            }
        }
    }

    private void parseRet(RetInst ret) {
        if (!ret.isVoid()) {
            load("$v0", ret.getOperand(0).getUniqueName());
        }
        IOUtils.mips("jr $ra\n");
    }

    private void parseAlloca(AllocaInst allocaInst) {
        if (allocaInst.getAllocaType() instanceof PointerType) {
            PointerType pointerType = (PointerType) allocaInst.getAllocaType();
            if (pointerType.getTargetType() instanceof IntegerType) {
                getSp(allocaInst.getUniqueName());
            } else if (pointerType.getTargetType() instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) pointerType.getTargetType();
                getSpArray(allocaInst.getUniqueName(), 4 * arrayType.getCapacity());
            }
        } else if (allocaInst.getAllocaType() instanceof IntegerType) {
            getSp(allocaInst.getUniqueName());
        } else if (allocaInst.getAllocaType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) allocaInst.getAllocaType();
            getSpArray(allocaInst.getUniqueName(), 4 * arrayType.getCapacity());
        }
    }

    private void parseLoad(LoadInst loadInst) {
        if (loadInst.getOperand(0) instanceof GEPInst) {
            load("$t0", loadInst.getOperand(0).getUniqueName());
            load("$t1", "$t0", 0);
            store("$t1", loadInst.getUniqueName());
        } else {
            load("$t0", loadInst.getOperand(0).getUniqueName());
            store("$t0", loadInst.getUniqueName());
        }
    }

    private void parseStore(StoreInst storeInst) {
        if (storeInst.getOperand(1) instanceof GEPInst) {
            load("$t0", storeInst.getOperand(0).getUniqueName());
            load("$t1", storeInst.getOperand(1).getUniqueName());
            store("$t0", "$t1", 0);
        } else {
            load("$t0", storeInst.getOperand(0).getUniqueName());
            store("$t0", storeInst.getOperand(1).getUniqueName());
        }
    }

    private void parseGEP(GEPInst gepInst) {
        PointerType pt = (PointerType) gepInst.getPointer().getType();
        if (pt.isString()) {
            IOUtils.mips("la $a0, " + gepInst.getPointer().getStringName() + "\n");
            return;
        }
        int offsetNum;
        List<Integer> dims;
        if (pt.getTargetType() instanceof ArrayType) {
            offsetNum = gepInst.getOperands().size() - 1;
            dims = ((ArrayType) pt.getTargetType()).getDimensions();
        } else {
            offsetNum = 1;
            dims = new ArrayList<>();
        }
        load("$t3", gepInst.getPointer().getUniqueName()); // arr
        store("$t3", gepInst.getUniqueName());
        int lastOff = 0;
        for (int i = 1; i <= offsetNum; i++) {
            int base = 4;
            if (pt.getTargetType() instanceof ArrayType) {
                for (int j = i - 1; j < dims.size(); j++) {
                    base *= dims.get(j);
                }
            }
            if (gepInst.getOperand(i).isNumber()) {
                int dimOff = gepInst.getOperand(i).getNumber() * base;
                lastOff += dimOff;
                if (i == offsetNum) {
                    if (lastOff == 0) {
                        store("$t3", gepInst.getUniqueName());
                    } else {
                        IOUtils.mips("addu $t2, $t3, " + lastOff + "\n");
                        store("$t2", gepInst.getUniqueName());
                    }
                }
            } else {
                if (lastOff != 0) {
                    IOUtils.mips("addu $t4, $t3, " + lastOff + "\n");
                    IOUtils.mips("move $t3, $t4\n");
                }
                IOUtils.mips("li $t5, " + base + "\n");
                load("$t6", gepInst.getOperand(i).getUniqueName()); // offset
                IOUtils.mips("mul $t6, $t6, $t5\n");
                IOUtils.mips("addu $t3, $t3, $t6\n");
                store("$t3", gepInst.getUniqueName());
            }
            IOUtils.mips("\n");
        }
    }

    private void parseBr(BrInst brInst) {
        if (brInst.isCondBr()) {
            load("$t0", brInst.getCond().getUniqueName());
            IOUtils.mips("beqz $t0, " + brInst.getFalseLabel().getLabelName() + "\n");
            IOUtils.mips("j " + brInst.getTrueLabel().getLabelName() + "\n");
        } else {
            IOUtils.mips("j " + brInst.getTrueLabel().getLabelName() + "\n");
        }
    }

    private void parseConv(ConvInst convInst) {
        if (convInst.getOperator() == Operator.Zext) {
            load("$t0", convInst.getOperand(0).getUniqueName());
            IOUtils.mips("sll $t0, $t0, 16\n");
            IOUtils.mips("srl $t0, $t0, 16\n");
            store("$t0", convInst.getUniqueName());
        } else if (convInst.getOperator() == Operator.Bitcast) {
            load("$t0", convInst.getOperand(0).getUniqueName());
            store("$t0", convInst.getUniqueName());
        }
    }

    private void load(String reg, String name) {
        if (isNumber(name)) {
            IOUtils.mips("li " + reg + ", " + name + "\n");
        } else {
            IOUtils.mips("lw " + reg + ", " + mem.get(name).getSecond() + "(" + mem.get(name).getFirst() + ")\n");
        }
    }

    private void load(String reg, String name, int offset) {
        IOUtils.mips("lw " + reg + ", " + offset + "(" + name + ")\n");
    }

    private void store(String reg, String name) {
        IOUtils.mips("sw " + reg + ", " + mem.get(name).getSecond() + "(" + mem.get(name).getFirst() + ")\n");
    }

    private void store(String reg, String name, int offset) {
        IOUtils.mips("sw " + reg + ", " + offset + "(" + name + ")\n");
    }

    private boolean isNumber(String str) {
        return str.matches("-?[0-9]+");
    }

}
