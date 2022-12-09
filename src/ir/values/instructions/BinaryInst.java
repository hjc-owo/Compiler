package ir.values.instructions;

import ir.types.IntegerType;
import ir.types.VoidType;
import ir.values.*;

public class BinaryInst extends Instruction {

    public BinaryInst(BasicBlock basicBlock, Operator op, Value left, Value right) {
        super(VoidType.voidType, op, basicBlock);
        if (op == Operator.Mod) {
            boolean isRight2Square = false;
            if (right instanceof ConstInt) {
                int m = ((ConstInt) right).getValue();
                if ((m > 0) && ((m & (m - 1)) == 0)) {
                    isRight2Square = true;
                }
            }
            if (!isRight2Square) {
                Value tmp = BuildFactory.getInstance().buildBinary(basicBlock, Operator.Div, left, right);
                tmp = BuildFactory.getInstance().buildBinary(basicBlock, Operator.Mul, right, tmp);
                this.setOperator(Operator.Sub);
                right = tmp;
            }
        }
        boolean isLeftI1 = left.getType() instanceof IntegerType && ((IntegerType) left.getType()).isI1();
        boolean isRightI1 = right.getType() instanceof IntegerType && ((IntegerType) right.getType()).isI1();
        boolean isLeftI32 = left.getType() instanceof IntegerType && ((IntegerType) left.getType()).isI32();
        boolean isRightI32 = right.getType() instanceof IntegerType && ((IntegerType) right.getType()).isI32();
        if (isLeftI1 && isRightI32) {
            addOperands(BuildFactory.getInstance().buildZext(left, basicBlock), right);
        } else if (isLeftI32 && isRightI1) {
            addOperands(left, BuildFactory.getInstance().buildZext(right, basicBlock));
        } else {
            addOperands(left, right);
        }
        this.setType(this.getOperands().get(0).getType());
        if (isCond()) {
            this.setType(IntegerType.i1);
        }
        this.setName("%" + REG_NUMBER++);
    }

    private void addOperands(Value left, Value right) {
        this.addOperand(left);
        this.addOperand(right);
    }

    public boolean isAdd() {
        return this.getOperator() == Operator.Add;
    }

    public boolean isSub() {
        return this.getOperator() == Operator.Sub;
    }

    public boolean isMul() {
        return this.getOperator() == Operator.Mul;
    }

    public boolean isDiv() {
        return this.getOperator() == Operator.Div;
    }

    public boolean isMod() {
        return this.getOperator() == Operator.Mod;
    }

    public boolean isShl() {
        return this.getOperator() == Operator.Shl;
    }

    public boolean isShr() {
        return this.getOperator() == Operator.Shr;
    }

    public boolean isAnd() {
        return this.getOperator() == Operator.And;
    }

    public boolean isOr() {
        return this.getOperator() == Operator.Or;
    }

    public boolean isLt() {
        return this.getOperator() == Operator.Lt;
    }

    public boolean isLe() {
        return this.getOperator() == Operator.Le;
    }

    public boolean isGe() {
        return this.getOperator() == Operator.Ge;
    }

    public boolean isGt() {
        return this.getOperator() == Operator.Gt;
    }

    public boolean isEq() {
        return this.getOperator() == Operator.Eq;
    }

    public boolean isNe() {
        return this.getOperator() == Operator.Ne;
    }

    public boolean isCond() {
        return this.isLt() || this.isLe() || this.isGe() || this.isGt() || this.isEq() || this.isNe();
    }

    public boolean isNot() {
        return this.getOperator() == Operator.Not;
    }

    public boolean canReverse(Operator operator1, Operator operator2) {
        Operator operator = reverse(operator1);
        if (operator == null) {
            return false;
        }
        return operator == operator2;
    }

    public Operator reverse(Operator operator) {
        Operator ans = null;
        switch (operator) {
            case Le:
                ans = Operator.Gt;
                break;
            case Lt:
                ans = Operator.Ge;
                break;
            case Ge:
                ans = Operator.Lt;
                break;
            case Gt:
                ans = Operator.Le;
                break;
            case Eq:
                ans = Operator.Eq;
                break;
            case Ne:
                ans = Operator.Ne;
                break;
        }
        return ans;
    }

    // 无法化简返回True，能够化简返回值
    public static Value simplifyConstant(Operator operator, Const left, Const right) {
        boolean isAllConstantInt = left instanceof ConstInt && right instanceof ConstInt;
        if (isAllConstantInt) {
            int l = ((ConstInt) left).getValue();
            int r = ((ConstInt) right).getValue();
            switch (operator) {
                case Add:
                    return new ConstInt(l + r);
                case Sub:
                    return new ConstInt(l - r);
                case Mul:
                    return new ConstInt(l * r);
                case Div:
                    return new ConstInt(l / r);
                case Mod:
                    return new ConstInt(l % r);
                case Le:
                    return new ConstInt(l <= r ? 1 : 0, true);
                case Lt:
                    return new ConstInt(l < r ? 1 : 0, true);
                case Ge:
                    return new ConstInt(l >= r ? 1 : 0, true);
                case Gt:
                    return new ConstInt(l > r ? 1 : 0, true);
                case Eq:
                    return new ConstInt(l == r ? 1 : 0, true);
                case Ne:
                    return new ConstInt(l != r ? 1 : 0, true);
                default:
                    return null;
            }
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        String s = getName() + " = ";
        switch (this.getOperator()) {
            case Add:
                s += "add i32 ";
                break;
            case Sub:
                s += "sub i32 ";
                break;
            case Mul:
                s += "mul i32 ";
                break;
            case Div:
                s += "sdiv i32 ";
                break;
            case Mod:
                s += "srem i32 ";
                break;
            case Shl:
                s += "shl i32 ";
                break;
            case Shr:
                s += "ashr i32 ";
                break;
            case And:
                s += "and " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Or:
                s += "or " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Lt:
                s += "icmp slt " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Le:
                s += "icmp sle " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Ge:
                s += "icmp sge " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Gt:
                s += "icmp sgt " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Eq:
                s += "icmp eq " + this.getOperands().get(0).getType().toString() + " ";
                break;
            case Ne:
                s += "icmp ne " + this.getOperands().get(0).getType().toString() + " ";
                break;
            default:
                break;
        }
        return s + this.getOperands().get(0).getName() + ", " + this.getOperands().get(1).getName();
    }
}
