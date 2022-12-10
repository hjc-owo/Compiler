package ir.values.instructions;

import ir.values.Const;
import ir.values.ConstInt;
import ir.values.GlobalVar;
import ir.values.Value;

public class SimplifyInstruction {
    // 调用入口
    public static Value simplify(Instruction instruction) {
        return simplify(instruction, 0);
    }

    private static Value simplify(Instruction instruction, int rec) {
        if (instruction instanceof BinaryInst) {
            return simplifyBinaryInst((BinaryInst) instruction, rec);
        } else if (instruction.isConvInst()) {
            return simplifyConvInst((ConvInst) instruction);
        } else {
            return instruction;
        }
    }

    private static Value simplifyGVAndFold(BinaryInst binaryInst) {
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // 1、全局 const 变量及 const 处理
        boolean change = false;
        if (left instanceof GlobalVar && ((GlobalVar) left).isConst()) {
            left = ((GlobalVar) left).getValue();
            change = true;
        }
        if (right instanceof GlobalVar && ((GlobalVar) right).isConst()) {
            right = ((GlobalVar) right).getValue();
            change = true;
        }
        if (change) {
            binaryInst.replaceOperands(0, left);
            binaryInst.replaceOperands(1, right);
        }
        return foldConstant(binaryInst.getOperator(), left, right);
    }

    public static Value simplifyBinaryInst(BinaryInst binaryInst, int rec) {
        switch (binaryInst.getOperator()) {
            case Add:
                return simplifyAddInst(binaryInst, rec);
            case Sub:
                return simplifySubInst(binaryInst, rec);
            case Mul:
                return simplifyMulInst(binaryInst, rec);
            case Div:
                return simplifyDivInst(binaryInst, rec);
            case Mod:
                return simplifyModInst(binaryInst);
            case Lt:
                return simplifyLtInst(binaryInst);
            case Le:
                return simplifyLeInst(binaryInst);
            case Gt:
                return simplifyGtInst(binaryInst);
            case Ge:
                return simplifyGeInst(binaryInst);
            case Eq:
                return simplifyEqInst(binaryInst);
            case Ne:
                return simplifyNeInst(binaryInst);
            default:
                /* exception handler */
                return binaryInst;
        }
    }

    public static Value simplifyConvInst(ConvInst convInst) {
        Value val = convInst.getOperands().get(0);
        // 全局 const 变量
        boolean change = false;
        if (val instanceof GlobalVar && ((GlobalVar) val).isConst()) {
            val = ((GlobalVar) val).getValue();
            change = true;
        }
        if (change) {
            convInst.replaceOperands(0, val);
        }
        switch (convInst.getOperator()) {
            case Zext:
                return simplifyZext(convInst);
            case Bitcast:
                return simplifyBitcast(convInst);
            default:
                /* exception handler*/
                return convInst;
        }
    }

    private static Value foldConstant(Operator operator, Value left, Value right) {
        if (left instanceof ConstInt && right instanceof ConstInt) {
            if (operator.ordinal() <= Operator.Ne.ordinal()) {
                return BinaryInst.simplifyConstant(operator, (Const) left, (Const) right);
            }
        }
        return null;
    }

    //add 二元指令的运算
    /*
    1、对全局变量fold
    2、 +0 优化
    3、 left + right == 0
    4、 (y - x) + x == y
    5、 结合律
     */
    private static Value simplifyAddInst(BinaryInst binaryInst, int rec) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null)
            return tmp;
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // 2、 +0 优化
        if (right instanceof ConstInt && ((ConstInt) right).getValue() == 0)
            return left;
        if (left instanceof ConstInt && ((ConstInt) left).getValue() == 0)
            return right;
        // 3、 left + right == 0
        if (left instanceof Instruction) {
            if (tryAddToZero(right, (Instruction) left)) {
                return ConstInt.ZERO;
            }
        }
        if (right instanceof Instruction) {
            if (tryAddToZero(left, (Instruction) right)) {
                return ConstInt.ZERO;
            }
        }
        // 以下化简 return 仍然为指令，因此不能递归
        if (rec != 0) {
            return binaryInst;
        }
        // (a - b) + c  |   (a + b) + c
        if (left instanceof BinaryInst &&
                (((BinaryInst) left).getOperator() == Operator.Sub || ((BinaryInst) left).getOperator() == Operator.Add)) {
            Value simp = tryCombineAddOrSubInst((BinaryInst) left, right, true, Operator.Add, rec);
            if (simp != null)
                return simp;
        }
        // a + (b - c)  |   a + (b + c)
        if (right instanceof BinaryInst &&
                (((BinaryInst) right).getOperator() == Operator.Sub || ((BinaryInst) right).getOperator() == Operator.Add)) {
            Value simp = tryCombineAddOrSubInst((BinaryInst) right, left, false, Operator.Add, rec);
            if (simp != null)
                return simp;
        }
        return binaryInst;
    }

    // add指令 尝试相加为0
    private static boolean tryAddToZero(Value value, Instruction inst) {
        if (inst.getOperator() == Operator.Sub) {
            Value left = inst.getOperands().get(0);
            Value right = inst.getOperands().get(1);
            if (left instanceof ConstInt && ((ConstInt) left).getValue() == 0)
                return right.equals(value);
        }
        return false;
    }

    // 结合律专用方法
    private static Value tryCombineAddOrSubInst(BinaryInst inst, Value value, boolean valueInRight, Operator curOperator, int rec) {
        Operator binOperator = inst.getOperator();
        Value l = inst.getOperands().get(0);
        Value r = inst.getOperands().get(1);
        //加法可以换位，统一 value 置左
        if (valueInRight && curOperator == Operator.Add) {
            valueInRight = false;
        }
        //减法在右 去括号
        if (!valueInRight && curOperator == Operator.Sub) {
            if (binOperator == Operator.Sub)
                binOperator = Operator.Add;
            else if (binOperator == Operator.Add)
                binOperator = Operator.Sub;
        }

        BinaryInst tmpBin = valueInRight ? new BinaryInst(null, curOperator, l, value) :  // l curOperator value
                new BinaryInst(null, curOperator, value, l); // value curOperator l
        Value simplify = simplify(tmpBin, rec + 1);
        if (!tmpBin.equals(simplify)) {
            tmpBin.removeUseFromOperands();
            BinaryInst ans = new BinaryInst(null, binOperator, simplify, r); // tmp binOperator r
            return simplify(ans);
        } else {
            tmpBin.removeUseFromOperands();
        }

        if (valueInRight && curOperator == Operator.Sub && binOperator == Operator.Sub) {
            curOperator = Operator.Add;
        }

        tmpBin = valueInRight ? new BinaryInst(null, curOperator, r, value) : // r curOperator value
                new BinaryInst(null, binOperator, value, r); // value binOperator r
        simplify = simplify(tmpBin, rec + 1);
        if (!tmpBin.equals(simplify)) {
            tmpBin.removeUseFromOperands();
            BinaryInst ans = valueInRight ? new BinaryInst(null, binOperator, l, simplify) : // l binOperator tmp
                    new BinaryInst(null, curOperator, simplify, l); // tmp curOperator l
            return simplify(ans);
        } else {
            tmpBin.removeUseFromOperands();
        }
        return null;
    }

    private static Value tryCombineMulOrDivInst(BinaryInst inst, Value value, boolean valueInRight, Operator curOperator, int rec) {
        Operator binOperator = inst.getOperator();
        Value l = inst.getOperands().get(0);
        Value r = inst.getOperands().get(1);
        //加法可以换位，统一 value 置左
        if (valueInRight && curOperator == Operator.Mul) {
            valueInRight = false;
        }
        //减法在右需去括号
        if (!valueInRight && curOperator == Operator.Div) {
            if (binOperator == Operator.Div)
                binOperator = Operator.Mul;
            else if (binOperator == Operator.Mul)
                binOperator = Operator.Div;
        }

        BinaryInst tmpBin = valueInRight ? new BinaryInst(null, curOperator, l, value) :  // l curOperator value
                new BinaryInst(null, curOperator, value, l); // value curOperator l
        Value simplify = simplify(tmpBin, rec + 1);
        if (!tmpBin.equals(simplify)) {
            tmpBin.removeUseFromOperands();
            BinaryInst ans = new BinaryInst(null, binOperator, simplify, r); // tmp binOperator r
            return simplify(ans, rec + 1);
        } else {
            tmpBin.removeUseFromOperands();
        }

        if (valueInRight && curOperator == Operator.Div && binOperator == Operator.Div) {
            curOperator = Operator.Mul;
        }

        tmpBin = valueInRight ? new BinaryInst(null, curOperator, r, value) : // r curOperator value
                new BinaryInst(null, binOperator, value, r); // value binOperator r
        simplify = simplify(tmpBin, rec + 1);
        if (!tmpBin.equals(simplify)) {
            tmpBin.removeUseFromOperands();
            BinaryInst ans = valueInRight ? new BinaryInst(null, binOperator, l, simplify) : // l binOperator tmp
                    new BinaryInst(null, curOperator, simplify, l); // tmp curOperator l
            return simplify(ans, rec + 1);
        } else {
            tmpBin.removeUseFromOperands();
        }
        return null;
    }

    /*
    1、对全局变量fold
    2、 +0 优化
    3、 left + right == 0
    4、 (y - x) + x == y
    5、 (x - y) + z -> (x + z) - y or (z - y) + x
    6、 x + (y - x) == y
    7、 x + (y - z) -> (x + y) - z or (x - z) + y
    8、 (x + y) + z -> x + (y + z)
     */

    /*
    1、对全局变量 fold
    2、 -0 优化
    3、 left == right
    4、
     */
    private static Value simplifySubInst(BinaryInst binaryInst, int rec) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // 2、 -0 优化
        if (right instanceof ConstInt && ((ConstInt) right).getValue() == 0) {
            return left;
        }
        // 3、 left == right
        if (left.equals(right)) {
            return ConstInt.ZERO;
        }
        if (rec != 0) {
            return binaryInst;
        }
        // (a + b) - c  |   (a - b) - c
        if (left instanceof BinaryInst &&
                (((BinaryInst) left).getOperator() == Operator.Add || ((BinaryInst) left).getOperator() == Operator.Sub)) {
            Value simp = tryCombineAddOrSubInst((BinaryInst) left, right, true, Operator.Sub, rec);
            if (simp != null)
                return simp;
        }
        // a - (b + c)  |   a - (b - c)
        if (right instanceof BinaryInst &&
                (((BinaryInst) right).getOperator() == Operator.Add || ((BinaryInst) right).getOperator() == Operator.Sub)) {
            Value simp = tryCombineAddOrSubInst((BinaryInst) right, left, false, Operator.Sub, rec);
            if (simp != null)
                return simp;
        }

        return binaryInst;
    }

    /*
    1、 全局变量fold
    2、 *0 *1
     */
    private static Value simplifyMulInst(BinaryInst binaryInst, int rec) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // 2、*0 *1
        if (left instanceof ConstInt) {
            int val = ((ConstInt) left).getValue();
            if (val == 0) {
                return ConstInt.ZERO;
            } else if (val == 1) {
                return right;
            }
        }
        if (right instanceof ConstInt) {
            int val = ((ConstInt) right).getValue();
            if (val == 0) {
                return ConstInt.ZERO;
            } else if (val == 1) {
                return left;
            }
        }
        // 以下递归
        if (rec != 0) {
            return binaryInst;
        }

        // (a * b) * c  |   (a / b) * c
        if (left instanceof BinaryInst &&
                (((BinaryInst) left).getOperator() == Operator.Mul)) {
            Value simp = tryCombineMulOrDivInst((BinaryInst) left, right, true, Operator.Mul, rec);
            if (simp != null)
                return simp;
        }
        // 分配律
        if (left instanceof BinaryInst &&
                (((BinaryInst) left).getOperator() == Operator.Add || ((BinaryInst) left).getOperator() == Operator.Sub)) {
            Value distribute = tryDistributeMulInst((BinaryInst) left, right, rec);
            if (distribute != null) {
                return distribute;
            }
        }
        if (right instanceof BinaryInst &&
                (((BinaryInst) right).getOperator() == Operator.Add || ((BinaryInst) right).getOperator() == Operator.Sub)) {
            Value distribute = tryDistributeMulInst((BinaryInst) right, left, rec);
            if (distribute != null) {
                return distribute;
            }
        }
        return binaryInst;
    }

    private static Value tryDistributeMulInst(BinaryInst inst, Value value, int rec) {
        Value l = inst.getOperands().get(0);
        Value r = inst.getOperands().get(1);
        Operator binOperator = inst.getOperator();
        BinaryInst leftInst = new BinaryInst(null, Operator.Mul, l, value);
        BinaryInst rightInst = new BinaryInst(null, Operator.Mul, r, value);
        Value simplifyLeft = simplify(leftInst, rec + 1);
        Value simplifyRight = simplify(rightInst, rec + 1);
        if (!leftInst.equals(simplifyLeft) && !rightInst.equals(simplifyRight)) {
            leftInst.removeUseFromOperands();
            rightInst.removeUseFromOperands();
            BinaryInst tmp = new BinaryInst(null, binOperator, simplifyLeft, simplifyRight);
            return simplify(tmp);
        } else {
            leftInst.removeUseFromOperands();
            rightInst.removeUseFromOperands();
        }
        return null;
    }

    /*
    1、 全局变量fold
    2、 /1
    3、 0/
     */
    private static Value simplifyDivInst(BinaryInst binaryInst, int rec) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // /1   |   0/
        if (right instanceof ConstInt && ((ConstInt) right).getValue() == 1) {
            return left;
        }
        if (left instanceof ConstInt && ((ConstInt) left).getValue() == 0) {
            return ConstInt.ZERO;
        }

        //以下不能递归
        return binaryInst;
    }


    private static Value simplifyModInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        Value left = binaryInst.getOperands().get(0);
        Value right = binaryInst.getOperands().get(1);
        // 2、 a % a == 0
        if (left.equals(right)) {
            return ConstInt.ZERO;
        }
        return binaryInst;
    }

    private static Value simplifyLtInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyLeInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyGtInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyGeInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyEqInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyNeInst(BinaryInst binaryInst) {
        Value tmp = simplifyGVAndFold(binaryInst);
        if (tmp != null) {
            return tmp;
        }
        return binaryInst;
    }

    private static Value simplifyZext(ConvInst convInst) {
        Value val = convInst.getOperands().get(0);
        if (val instanceof ConstInt) {
            return new ConstInt(((ConstInt) val).getValue());
        }
        return convInst;
    }

    private static Value simplifyBitcast(ConvInst convInst) {
        return convInst;
    }
}
