package ir.values;

import ir.types.ArrayType;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.Type;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.ConstArray;
import ir.values.instructions.ConvInst;
import ir.values.instructions.Operator;
import ir.values.instructions.mem.*;
import ir.values.instructions.terminator.BrInst;
import ir.values.instructions.terminator.CallInst;
import ir.values.instructions.terminator.RetInst;

import java.util.List;

public class BuildFactory {

    private static final BuildFactory buildFactory = new BuildFactory();

    public BuildFactory() {
    }

    public static BuildFactory getInstance() {
        return buildFactory;
    }

    /**
     * Functions
     **/
    public Function buildFunction(String name, Type ret, List<Type> parametersTypes) {
        return new Function(name, getFunctionType(ret, parametersTypes), false);
    }

    public Function buildLibraryFunction(String name, Type ret, List<Type> parametersTypes) {
        return new Function(name, getFunctionType(ret, parametersTypes), true);
    }

    public FunctionType getFunctionType(Type retType, List<Type> parametersTypes) {
        return new FunctionType(retType, parametersTypes);
    }

    public List<Value> getFunctionArguments(Function function) {
        return function.getArguments();
    }

    /**
     * BasicBlock
     */
    public BasicBlock buildBasicBlock(Function function) {
        return new BasicBlock(function);
    }

    public void checkBlockEnd(BasicBlock basicBlock) {
        Type retType = ((FunctionType) basicBlock.getNode().getParent().getValue().getType()).getReturnType();
        if (!basicBlock.getInstructions().isEmpty()) {
            Value lastInst = basicBlock.getInstructions().getEnd().getValue();
            if (lastInst instanceof RetInst || lastInst instanceof BrInst) {
                return;
            }
        }
        if (retType instanceof IntegerType) {
            buildRet(basicBlock, ConstInt.ZERO);
        } else {
            buildRet(basicBlock);
        }
    }

    /**
     * BinaryInst
     **/
    public BinaryInst buildBinary(BasicBlock basicBlock, Operator op, Value left, Value right) {
        BinaryInst tmp = new BinaryInst(basicBlock, op, left, right);
        if (op == Operator.And || op == Operator.Or) {
            tmp = buildBinary(basicBlock, Operator.Ne, tmp, ConstInt.ZERO);
        }
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public BinaryInst buildNot(BasicBlock basicBlock, Value value) {
        return buildBinary(basicBlock, Operator.Eq, value, ConstInt.ZERO);
    }

    /**
     * Var
     */
    public GlobalVar buildGlobalVar(String name, Type type, boolean isConst, Value value) {
        return new GlobalVar(name, type, isConst, value);
    }

    public AllocaInst buildVar(BasicBlock basicBlock, Value value, boolean isConst, Type allocaType) {
        AllocaInst tmp = new AllocaInst(basicBlock, isConst, allocaType);
        tmp.addInstToBlock(basicBlock);
        if (value != null) {
            buildStore(basicBlock, tmp, value);
        }
        return tmp;
    }

    public ConstInt getConstInt(int value) {
        return new ConstInt(value);
    }

    public ConstString getConstString(String value) {
        return new ConstString(value);
    }

    /**
     * Array
     */
    public GlobalVar buildGlobalArray(String name, Type type, boolean isConst) {
        Value tmp = new ConstArray(type, ((ArrayType) type).getElementType(), ((ArrayType) type).getCapacity());
        return new GlobalVar(name, type, isConst, tmp);
    }

    public AllocaInst buildArray(BasicBlock basicBlock, boolean isConst, Type arrayType) {
        AllocaInst tmp = new AllocaInst(basicBlock, isConst, arrayType);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public void buildInitArray(Value array, int index, Value value) {
        ((ConstArray) ((GlobalVar) array).getValue()).storeValue(index, value);
    }

    public ArrayType getArrayType(Type elementType, int length) {
        return new ArrayType(elementType, length);
    }

    /**
     * ConvInst
     */
    public Value buildZext(Value value, BasicBlock basicBlock) {
        if (value instanceof ConstInt) {
            return new ConstInt(((ConstInt) value).getValue());
        }
        ConvInst tmp = new ConvInst(basicBlock, Operator.Zext, value);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public ConvInst buildBitcast(Value value, BasicBlock basicBlock) {
        ConvInst tmp = new ConvInst(basicBlock, Operator.Bitcast, value);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public BinaryInst buildConvToI1(Value val, BasicBlock basicBlock) {
        BinaryInst tmp = new BinaryInst(basicBlock, Operator.Ne, val, getConstInt(0));
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    /**
     * MemInst
     */
    public LoadInst buildLoad(BasicBlock basicBlock, Value pointer) {
        LoadInst tmp = new LoadInst(basicBlock, pointer);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public StoreInst buildStore(BasicBlock basicBlock, Value ptr, Value value) {
        StoreInst tmp = new StoreInst(basicBlock, ptr, value);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public GEPInst buildGEP(BasicBlock basicBlock, Value pointer, List<Value> indices) {
        GEPInst tmp = new GEPInst(basicBlock, pointer, indices);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public GEPInst buildGEP(BasicBlock basicBlock, Value pointer, int offset) {
        GEPInst tmp = new GEPInst(basicBlock, pointer, offset);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public PhiInst buildPhi(BasicBlock basicBlock, Type type, List<Value> in) {
        PhiInst tmp = new PhiInst(basicBlock, type, in);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    /**
     * TerminatorInst
     */
    public BrInst buildBr(BasicBlock basicBlock, BasicBlock trueBlock) {
        BrInst tmp = new BrInst(basicBlock, trueBlock);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public BrInst buildBr(BasicBlock basicBlock, Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
        BrInst tmp = new BrInst(basicBlock, cond, trueBlock, falseBlock);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public CallInst buildCall(BasicBlock basicBlock, Function function, List<Value> args) {
        CallInst tmp = new CallInst(basicBlock, function, args);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public RetInst buildRet(BasicBlock basicBlock) {
        RetInst tmp = new RetInst(basicBlock);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

    public RetInst buildRet(BasicBlock basicBlock, Value ret) {
        RetInst tmp = new RetInst(basicBlock, ret);
        tmp.addInstToBlock(basicBlock);
        return tmp;
    }

}
