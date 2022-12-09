package backend;

import config.Config;
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
import utils.Triple;

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
        for (GlobalVar gv : irModule.getGlobalVars()) {
            IOUtils.mips("\n# " + gv + "\n\n");
            if (gv.isString()) {
                ConstString constString = (ConstString) gv.getValue();
                IOUtils.mips(gv.getUniqueName() + ": .asciiz " + constString.getName() + "\n");
            } else if (gv.isInt()) {
                getGp(gv.getUniqueName(), gv);
                IOUtils.mips(gv.getUniqueName() + ": .word " + ((ConstInt) gv.getValue()).getValue() + "\n");
            } else if (gv.isArray()) {
                ConstArray constArray = (ConstArray) gv.getValue();
                getGp(gv.getUniqueName(), gv);
                PointerType pt = (PointerType) gv.getType();
                IOUtils.mips(gv.getUniqueName() + ": ");
                if (constArray.isInit()) {
                    IOUtils.mips(".word ");
                    // 数组初值
                    int capacity = ((ArrayType) pt.getTargetType()).getCapacity();
                    for (int i = 0; i < capacity; i++) {
                        IOUtils.mips(String.valueOf(((ConstInt) (constArray).get1DArray().get(i)).getValue()));
                        if (i != capacity - 1) {
                            IOUtils.mips(", ");
                        }
                    }
                    IOUtils.mips("\n");
                } else {
                    IOUtils.mips(".space " + ((ArrayType) pt.getTargetType()).getCapacity() * 4 + "\n");
                }
            }
        }
        for (INode<Function, IRModule> funcEntry : irModule.getFunctions()) {
            Function function = funcEntry.getValue();
            if (function.isLibraryFunction()) {
                if (Objects.equals(function.getName(), "getint"))
                    IOUtils.mips("\n.macro GETINT()\nli $v0, 5\nsyscall\n.end_macro\n");
                else if (Objects.equals(function.getName(), "putint"))
                    IOUtils.mips("\n.macro PUTINT()\nli $v0, 1\nsyscall\n.end_macro\n");
                else if (Objects.equals(function.getName(), "putch"))
                    IOUtils.mips("\n.macro PUTCH()\nli $v0, 11\nsyscall\n.end_macro\n");
                else if (Objects.equals(function.getName(), "putstr"))
                    IOUtils.mips("\n.macro PUTSTR()\nli $v0, 4\nsyscall\n.end_macro\n");
            }
        }
        IOUtils.mips("\n.text\n");
        IOUtils.mips("\njal main\n");
        IOUtils.mips("\nj return\n\n");

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
                getSp(function.getArguments().get(i).getUniqueName(), function.getArguments().get(i));
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
                        getSp(ir.getUniqueName(), ir);
                    }
                    translate(ir);
                }
                if (Config.RegAllocator) {
                    clearRegMap();
                }
            }
        }
        IOUtils.mips("\nreturn:\n");
    }


    private Map<String, Triple<String, Integer, Value>> mem = new HashMap<>();
    int spOff = 0, rec = 0;

    private void getGp(String name, Value value) {
        if (mem.containsKey(name)) {
            return;
        }
        mem.put(name, new Triple<>("$gp", 0, value));
    }

    private void getSp(String name, Value value) {
        if (mem.containsKey(name)) {
            return;
        }
        spOff -= 4;
        mem.put(name, new Triple<>("$sp", spOff, value));
    }

    private void getSpArray(String name, int offset, Value value) {
        if (mem.containsKey(name)) {
            return;
        }
        getSp(name, value);
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
        // else if (ir instanceof PhiInst) parsePhi((PhiInst) ir);
    }

    private void parseBinary(BinaryInst b) {
        if (b.isAdd()) calc(b, "addu", 0);
        else if (b.isSub()) calc(b, "subu", 1);
        else if (b.isMul()) {
            if (!Config.MulAndDivOptimization) {
                calc(b, "mul", 0);
                return;
            }
            Value left = b.getOperand(0), right = b.getOperand(1);
            boolean isLeftConst = left instanceof ConstInt, isRightConst = right instanceof ConstInt;
            int leftValue = isLeftConst ? ((ConstInt) left).getValue() : 0;
            int rightValue = isRightConst ? ((ConstInt) right).getValue() : 0;
            int leftAbs = leftValue >= 0 ? leftValue : -leftValue;
            int rightAbs = rightValue >= 0 ? rightValue : -rightValue;
            if (isLeftConst && leftValue == 0) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && rightValue == 0) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else if (isLeftConst && leftValue == 1) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && rightValue == 1) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else if (isLeftConst && leftValue == -1) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && rightValue == -1) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else if (isLeftConst && (leftAbs & (leftAbs - 1)) == 0) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && (rightAbs & (rightAbs - 1)) == 0) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else if (isLeftConst && ((leftAbs - 1) & (leftAbs - 2)) == 0) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && ((rightAbs - 1) & (rightAbs - 2)) == 0) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else if (isLeftConst && ((leftAbs + 1) & leftAbs) == 0) {
                optimizeMul(right, (ConstInt) left, b, false);
            } else if (isRightConst && ((rightAbs + 1) & rightAbs) == 0) {
                optimizeMul(left, (ConstInt) right, b, false);
            } else {
                calc(b, "mul", 0);
            }
        } else if (b.isDiv()) {
            if (!Config.MulAndDivOptimization) {
                calc(b, "div", 1);
                return;
            }
            if (b.getOperand(1) instanceof ConstInt) {
                optimizeDiv(b.getOperand(0), ((ConstInt) b.getOperand(1)), b, false);
            } else {
                calc(b, "div", 1);
            }
        } else if (b.isMod()) {
            if (!Config.MulAndDivOptimization) {
                calc(b, "rem", 1);
                return;
            }
            if (b.getOperand(1) instanceof ConstInt) {
                optimizeMod(b.getOperand(0), (ConstInt) b.getOperand(1), b);
            } else {
                calc(b, "rem", 1);
            }
        } else if (b.isShl()) calc(b, "sll", 1);
        else if (b.isShr()) calc(b, "srl", 1);
        else if (b.isAnd()) calc(b, "and", 0);
        else if (b.isOr()) calc(b, "or", 0);
        else if (b.isLe()) calc(b, "sle", 1);
        else if (b.isLt()) calc(b, "slt", 2);
        else if (b.isGe()) calc(b, "sge", 1);
        else if (b.isGt()) calc(b, "sgt", 2);
        else if (b.isEq()) calc(b, "seq", 0);
        else if (b.isNe()) calc(b, "sne", 0);
        else if (b.isNot()) {
            load("$t0", b.getOperand(0).getUniqueName());
            IOUtils.mips("not $t0, $t0\n");
            store("$t0", b.getUniqueName());
        }

    }

    private void optimizeMul(Value operand, ConstInt immValue, Instruction b, boolean isMod) {
        int imm = immValue.getValue();
        int abs = imm >= 0 ? imm : -imm;
        if (imm == 0) {
            load("$t0", "0");
        } else {
            if (!isMod) {
                load("$t0", operand.getUniqueName());
            }
            if (imm == 1) {
            } else if (imm == -1) {
                IOUtils.mips("negu $t0, $t0\n");
            } else if ((abs & (abs - 1)) == 0) {
                IOUtils.mips("sll $t0, $t0, " + getCTZ(abs) + "\n");
                if (imm <= 0) {
                    IOUtils.mips("negu $t0, $t0\n");
                }
            } else if (((abs - 1) & (abs - 2)) == 0) {
                IOUtils.mips("sll $t1, $t0, " + getCTZ(abs) + "\n");
                IOUtils.mips("addu $t0, $t0, $t1\n");
                if (imm <= 0) {
                    IOUtils.mips("negu $t0, $t0\n");
                }
            } else if (((abs + 1) & abs) == 0) {
                IOUtils.mips("sll $t1, $t0, " + (getCTZ(abs) + 1) + "\n");
                if (imm > 0) {
                    IOUtils.mips("subu $t0, $t1, $t0\n");
                } else {
                    IOUtils.mips("subu $t0, $t0, $t1\n");
                }
            } else {
                IOUtils.mips("mul $t0, $t0, " + imm + "\n");
            }
        }
        if (!isMod) {
            store("$t0", b.getUniqueName());
        }
    }

    private void optimizeDiv(Value operand, ConstInt immValue, BinaryInst b, boolean isMod) {
        int imm = immValue.getValue();
        if (imm == 1) {
            load("$t0", operand.getUniqueName());
        } else if (imm == -1) {
            load("$t0", operand.getUniqueName());
            IOUtils.mips("negu $t0, $t0\n");
        } else {
            int abs = imm >= 0 ? imm : -imm;
            load("$t0", operand.getUniqueName()); // n
            if ((abs & (abs - 1)) == 0) {
                // (n + ((n >> (31)) >>> (32 - l))) >> l
                IOUtils.mips("sra $t1, $t0, 31\n"); // n >> 31
                int l = getCTZ(abs);
                IOUtils.mips("srl $t1, $t1, " + (32 - l) + "\n"); // (n >> 31) >>> (32 - l)
                IOUtils.mips("addu $t0, $t0, $t1\n"); // n + ((n >> 31) >>> (32 - l))
                IOUtils.mips("sra $t0, $t0, " + l + "\n"); // (n + ((n >> 31) >>> (32 - l))) >> l
            } else {
                Triple<Long, Integer, Integer> multiplier = chooseMultiplier(abs, 31);
                long m = multiplier.getFirst();
                int sh = multiplier.getSecond();
                if (m < 2147483648L) {
                    load("$t1", String.valueOf(m));
                    IOUtils.mips("mult $t0, $t1\n");
                    IOUtils.mips("mfhi $t2\n");
                } else {
                    load("$t1", String.valueOf((m - (1L << 32))));
                    IOUtils.mips("mult $t0, $t1\n");
                    IOUtils.mips("mfhi $t2\n");
                    IOUtils.mips("addu $t2, $t2, $t0\n");
                }
                IOUtils.mips("sra $t2, $t2, " + sh + "\n");
                IOUtils.mips("srl $t1, $t0, 31\n");
                IOUtils.mips("addu $t0, $t2, $t1\n");
            }
            if (imm < 0) {
                IOUtils.mips("negu $t0, $t0\n");
            }
        }
        if (!isMod) {
            store("$t0", b.getUniqueName());
        }
    }

    private void optimizeMod(Value operand, ConstInt immValue, BinaryInst b) {
        int imm = immValue.getValue();
        if (imm == 1 || imm == -1) {
            load("$t0", "0");
        } else {
            optimizeDiv(operand, immValue, b, true);
            IOUtils.mips("\n");
            optimizeMul(b, immValue, b, true);
            IOUtils.mips("\n");
            load("$t1", operand.getUniqueName());
            IOUtils.mips("subu $t0, $t1, $t0\n");
        }
        store("$t0", b.getUniqueName());
    }

    public int getCTZ(int num) {
        int r = 0;
        num >>>= 1;
        while (num > 0) {
            r++;
            num >>>= 1;
        }
        return r; // 0 - 31
    }

    private Triple<Long, Integer, Integer> chooseMultiplier(int d, int prec) {
        long nc = (1L << prec) - ((1L << prec) % d) - 1;
        long p = 32;
        while ((1L << p) <= nc * (d - (1L << p) % d)) {
            p++;
        }
        long m = (((1L << p) + (long) d - (1L << p) % d) / (long) d);
        long n = ((m << 32) >>> 32);
        return new Triple<>(n, (int) (p - 32), 0);
    }

    private void calc(BinaryInst b, String op, int type) {
        if (type == 0 && b.getOperand(0) instanceof ConstInt) {
            load("$t0", b.getOperand(1).getUniqueName());
            IOUtils.mips(op + " $t0, $t0, " + ((ConstInt) b.getOperand(0)).getValue() + "\n");
            store("$t0", b.getUniqueName());
            return;
        }
        if (type <= 1 && b.getOperand(1) instanceof ConstInt) {
            load("$t0", b.getOperand(0).getUniqueName());
            IOUtils.mips(op + " $t0, $t0, " + ((ConstInt) b.getOperand(1)).getValue() + "\n");
            store("$t0", b.getUniqueName());
            return;
        }
        load("$t0", b.getOperand(0).getUniqueName());
        load("$t1", b.getOperand(1).getUniqueName());
        IOUtils.mips(op + " $t0, $t0, $t1\n");
        store("$t0", b.getUniqueName());
    }

    private void parseCall(CallInst callInst) {
        Function function = callInst.getCalledFunction();
        if (function.isLibraryFunction()) {
            if (Objects.equals(function.getName(), "getint")) {
                IOUtils.mips("GETINT()\n");
                store("$v0", callInst.getUniqueName());
            } else if (Objects.equals(function.getName(), "putint")) {
                load("$a0", callInst.getOperand(1).getUniqueName());
                IOUtils.mips("PUTINT()\n");
            } else if (Objects.equals(function.getName(), "putch")) {
                load("$a0", callInst.getOperand(1).getUniqueName());
                IOUtils.mips("PUTCH()\n");
            } else if (Objects.equals(function.getName(), "putstr")) {
                IOUtils.mips("PUTSTR()\n");
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
                getSp(allocaInst.getUniqueName(), allocaInst);
            } else if (pointerType.getTargetType() instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) pointerType.getTargetType();
                getSpArray(allocaInst.getUniqueName(), 4 * arrayType.getCapacity(), allocaInst);
            }
        } else if (allocaInst.getAllocaType() instanceof IntegerType) {
            getSp(allocaInst.getUniqueName(), allocaInst);
        } else if (allocaInst.getAllocaType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) allocaInst.getAllocaType();
            getSpArray(allocaInst.getUniqueName(), 4 * arrayType.getCapacity(), allocaInst);
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
            IOUtils.mips("la $a0, " + gepInst.getPointer().getGlobalName() + "\n");
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
        load("$t2", gepInst.getPointer().getUniqueName()); // arr
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
                    if (lastOff != 0) {
                        IOUtils.mips("addu $t2, $t2, " + lastOff + "\n");
                    }
                    store("$t2", gepInst.getUniqueName());
                }
            } else {
                if (lastOff != 0) {
                    IOUtils.mips("addu $t2, $t2, " + lastOff + "\n");
                }
                load("$t0", gepInst.getOperand(i).getUniqueName()); // offset
                optimizeMul(gepInst.getOperand(i), new ConstInt(base), gepInst, true);
                IOUtils.mips("addu $t2, $t2, $t0\n");
                store("$t2", gepInst.getUniqueName());
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
        } else if (mem.get(name).getThird() instanceof GlobalVar) {
            IOUtils.mips("la " + reg + ", " + name + "\n");
            if (((GlobalVar) mem.get(name).getThird()).isInt()) {
                IOUtils.mips("lw " + reg + ", 0(" + reg + ")\n");
            }
        } else {
            if (Config.RegAllocator) {
                String name1 = Reg.getInstance().loadFromRegMap(name);
                if (name1 == null) {
                    IOUtils.mips("lw " + reg + ", " + mem.get(name).getSecond() + "(" + mem.get(name).getFirst() + ")\n");
                } else {
                    IOUtils.mips("move " + reg + ", " + name1 + "\n");
                }
            } else {
                IOUtils.mips("lw " + reg + ", " + mem.get(name).getSecond() + "(" + mem.get(name).getFirst() + ")\n");
            }
        }
    }

    private void load(String reg, String name, int offset) {
        IOUtils.mips("lw " + reg + ", " + offset + "(" + name + ")\n");
    }

    private void store(String reg, String name) {
        if (mem.get(name).getThird() instanceof GlobalVar) {
            IOUtils.mips("la $t1, " + name + "\n");
            if (((GlobalVar) mem.get(name).getThird()).isInt()) {
                IOUtils.mips("sw " + reg + ", 0($t1)\n");
            }
        } else {
            if (Config.RegAllocator) {
                List<String> list = Reg.getInstance().storeToRegMap(name);
                if (list.size() == 1) {
                    String i = list.get(0);
                    IOUtils.mips("move " + i + ", " + reg + "\n");
                } else if (list.size() == 2) {
                    String i = list.get(0);
                    String name1 = list.get(1);
                    IOUtils.mips("sw " + i + ", " + mem.get(name1).getSecond() + "(" + mem.get(name1).getFirst() + ")\n");
                    IOUtils.mips("move " + i + ", " + reg + "\n");
                }
            } else {
                IOUtils.mips("sw " + reg + ", " + mem.get(name).getSecond() + "(" + mem.get(name).getFirst() + ")\n");
            }
        }
    }

    private void store(String reg, String name, int offset) {
        IOUtils.mips("sw " + reg + ", " + offset + "(" + name + ")\n");
    }

    private boolean isNumber(String str) {
        return str.matches("-?[0-9]+");
    }

    private void clearRegMap() {
        Map<String, Integer> map = Reg.getInstance().getRegMap();
        for (String s : map.keySet()) {
            int i = map.get(s);
            IOUtils.mips("sw " + Reg.getInstance().getReg(i) + ", " + mem.get(s).getSecond() + "(" + mem.get(s).getFirst() + ")\n");
        }
        Reg.getInstance().clear();
    }
}
