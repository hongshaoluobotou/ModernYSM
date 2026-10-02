package com.elfmcys.yesstevemodel.molang.runtime;

import com.elfmcys.yesstevemodel.molang.parser.ast.*;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ValueConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public final class ExpressionEvaluatorImpl<TEntity> implements ExpressionEvaluator<TEntity>, ExpressionVisitor<Object> {

    private static final Double DOUBLE_ZERO = 0.0d;
    private static final Float FLOAT_ZERO = 0.0f;

    private static final Evaluator[] BINARY_EVALUATORS = {
            (evaluator, a, b) -> {
                if (!ValueConversions.asBoolean(evaluator.visitBounded(a))) return Boolean.FALSE;
                return ValueConversions.asBoolean(evaluator.visitBounded(b)) ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                if (ValueConversions.asBoolean(evaluator.visitBounded(a))) return Boolean.TRUE;
                return ValueConversions.asBoolean(evaluator.visitBounded(b)) ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av < bv ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av <= bv ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av > bv ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av >= bv ? Boolean.TRUE : Boolean.FALSE;
            },
            (evaluator, a, b) -> {
                final Object aVal = evaluator.visitBounded(a);
                final Object bVal = evaluator.visitBounded(b);
                return ValueConversions.asFloat(aVal) + ValueConversions.asFloat(bVal);
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av - bv;
            },
            (evaluator, a, b) -> {
                float av = ValueConversions.asFloat(evaluator.visitBounded(a));
                float bv = ValueConversions.asFloat(evaluator.visitBounded(b));
                return av * bv;
            },
            // molang 里除零结果为 0
            (evaluator, a, b) -> {
                float dividend = ValueConversions.asFloat(evaluator.visitBounded(a));
                float divisor = ValueConversions.asFloat(evaluator.visitBounded(b));
                if (divisor == 0.0f) return FLOAT_ZERO;
                return dividend / divisor;
            },
            (evaluator, a, b) -> { // arrow
                Object val = evaluator.visitBounded(a);
                if (val == null) {
                    return null;
                }
                ExpressionEvaluatorImpl child = evaluator.createChild(val);
                Object res = child.visitBounded(b);
                evaluator.returnValue = child.returnValue;
                return res;
            },
            (evaluator, a, b) -> { // null coalesce
                Object val = evaluator.visitBounded(a);
                if (val == null) {
                    return evaluator.visitBounded(b);
                } else {
                    return val;
                }
            },
            (evaluator, a, b) -> { // assignation
                Object val = evaluator.visitBounded(b);
                if (a instanceof AssignableVariableExpression) {
                    AssignableVariable var = ((AssignableVariableExpression) a).target();
                    if (val instanceof Struct) {
                        val = ((Struct) val).copy();
                    }
                    var.assign(evaluator, val);
                } else if (a instanceof StructAccessExpression exp) {
                    if (val instanceof Struct) {
                        // 不允许结构体嵌套
                        return val;
                    }
                    Object value = evaluator.visitBounded(exp.left());
                    if (value instanceof Struct) {
                        ((Struct) value).putProperty(exp.path(), val);
                    } else if (exp.left() instanceof AssignableVariableExpression) {
                        AssignableVariable variable = ((AssignableVariableExpression) exp.left()).target();
                        Struct struct = new HashMapStruct();
                        struct.putProperty(exp.path(), val);
                        variable.assign(evaluator, struct);
                    }
                }
                // TODO: (else case) This isn't fail-fast, we can only assign to access expressions
                return val;
            },
            (evaluator, a, b) -> { // conditional
                Object condition = evaluator.visitBounded(a);
                if (ValueConversions.asBoolean(condition)) {
                    return evaluator.visitBounded(b);
                }
                return null;
            }, (evaluator, a, b) -> {
                Object left = evaluator.visitBounded(a);
                Object right = evaluator.visitBounded(b);
                if (left == right)
                    return true;
                if (left instanceof Number || right instanceof Number)
                    return ValueConversions.asFloat(right) == ValueConversions.asFloat(left);
                if (left == null || right == null)
                    return false;
                if (left instanceof StringExpression)
                    return left.equals(right);
                if (right instanceof StringExpression)
                    return right.equals(left);
                return false;
            }, //eq
            (evaluator, a, b) -> {
                Object left = evaluator.visitBounded(a);
                Object right = evaluator.visitBounded(b);
                if (left == right)
                    return false;
                if (left instanceof Number || right instanceof Number)
                    return ValueConversions.asFloat(right) != ValueConversions.asFloat(left);
                if (left == null || right == null)
                    return true;
                if (left instanceof StringExpression)
                    return !left.equals(right);
                if (right instanceof StringExpression)
                    return !right.equals(left);
                return false;
            }
    };

    private final TEntity entity;
    private final EvaluationBudget budget;

    private @Nullable Object returnValue;

    @Nullable
    private StatementExpression.Op op;

    private int cnt = 0;

    private int working = 0;

    public ExpressionEvaluatorImpl(@Nullable TEntity tentity) {
        this(tentity, new EvaluationBudget());
    }

    private ExpressionEvaluatorImpl(@Nullable TEntity tentity, EvaluationBudget budget) {
        this.entity = tentity;
        this.budget = budget;
    }

    @Override
    public TEntity entity() {
        return this.entity;
    }

    @Override
    @Nullable
    public Object eval(@NotNull Expression expression) {
        budget.begin();
        try {
            return this.visitBounded(expression);
        } finally {
            this.returnValue = null;
            this.op = null;
            budget.end();
        }
    }

    @Override
    public float evalAsFloat(@NotNull Expression expression) {
        budget.begin();
        try {
            return evalFloat(expression);
        } finally {
            this.returnValue = null;
            this.op = null;
            budget.end();
        }
    }

    @Override
    public boolean evalAsBoolean(@NotNull Expression expression) {
        budget.begin();
        try {
            return evalBool(expression);
        } finally {
            this.returnValue = null;
            this.op = null;
            budget.end();
        }
    }

    // 算术子树原生递归，跳过中间 Float 装箱；遇到不能在 primitive 域处理的节点回退到 visit
    private float evalFloat(@NotNull Expression expr) {
        budget.enter();
        try {
            return evalFloatUnchecked(expr);
        } finally {
            budget.leave();
        }
    }

    private float evalFloatUnchecked(@NotNull Expression expr) {
        if (expr instanceof FloatExpression fe) {
            return fe.value();
        }
        if (expr instanceof BinaryExpression be) {
            switch (be.op()) {
                case ADD: return evalFloat(be.left()) + evalFloat(be.right());
                case SUB: return evalFloat(be.left()) - evalFloat(be.right());
                case MUL: return evalFloat(be.left()) * evalFloat(be.right());
                case DIV: {
                    float d = evalFloat(be.right());
                    if (d == 0.0f) return 0.0f;
                    return evalFloat(be.left()) / d;
                }
                case LT:  return evalFloat(be.left()) <  evalFloat(be.right()) ? 1.0f : 0.0f;
                case LTE: return evalFloat(be.left()) <= evalFloat(be.right()) ? 1.0f : 0.0f;
                case GT:  return evalFloat(be.left()) >  evalFloat(be.right()) ? 1.0f : 0.0f;
                case GTE: return evalFloat(be.left()) >= evalFloat(be.right()) ? 1.0f : 0.0f;
                case AND: return (evalBool(be.left()) && evalBool(be.right())) ? 1.0f : 0.0f;
                case OR:  return (evalBool(be.left()) || evalBool(be.right())) ? 1.0f : 0.0f;
                default: break;
            }
        }
        if (expr instanceof UnaryExpression ue) {
            switch (ue.op()) {
                case ARITHMETICAL_NEGATION: return -evalFloat(ue.expression());
                case PLUS: return evalFloat(ue.expression());
                case LOGICAL_NEGATION: return evalBool(ue.expression()) ? 0.0f : 1.0f;
                case RETURN: return evalFloat(ue.expression());
                default: break;
            }
        }
        if (expr instanceof TernaryConditionalExpression te) {
            return evalBool(te.condition())
                    ? evalFloat(te.trueExpression())
                    : evalFloat(te.falseExpression());
        }
        return ValueConversions.asFloat(this.visitBounded(expr));
    }

    private boolean evalBool(@NotNull Expression expr) {
        budget.enter();
        try {
            return evalBoolUnchecked(expr);
        } finally {
            budget.leave();
        }
    }

    private boolean evalBoolUnchecked(@NotNull Expression expr) {
        if (expr instanceof FloatExpression fe) {
            return fe.value() != 0.0f;
        }
        if (expr instanceof BinaryExpression be) {
            switch (be.op()) {
                case AND: return evalBool(be.left()) && evalBool(be.right());
                case OR:  return evalBool(be.left()) || evalBool(be.right());
                case LT:  return evalFloat(be.left()) <  evalFloat(be.right());
                case LTE: return evalFloat(be.left()) <= evalFloat(be.right());
                case GT:  return evalFloat(be.left()) >  evalFloat(be.right());
                case GTE: return evalFloat(be.left()) >= evalFloat(be.right());
                case ADD: return (evalFloat(be.left()) + evalFloat(be.right())) != 0.0f;
                case SUB: return (evalFloat(be.left()) - evalFloat(be.right())) != 0.0f;
                case MUL: {
                    float l = evalFloat(be.left());
                    if (l == 0.0f) return false;
                    return evalFloat(be.right()) != 0.0f;
                }
                case DIV: {
                    float r = evalFloat(be.right());
                    if (r == 0.0f) return false;
                    return (evalFloat(be.left()) / r) != 0.0f;
                }
                default: break;
            }
        }
        if (expr instanceof UnaryExpression ue) {
            switch (ue.op()) {
                case LOGICAL_NEGATION: return !evalBool(ue.expression());
                case ARITHMETICAL_NEGATION: return evalBool(ue.expression());
                case PLUS: return evalBool(ue.expression());
                case RETURN: return evalBool(ue.expression());
                default: break;
            }
        }
        if (expr instanceof TernaryConditionalExpression te) {
            return evalBool(te.condition())
                    ? evalBool(te.trueExpression())
                    : evalBool(te.falseExpression());
        }
        return ValueConversions.asBoolean(this.visitBounded(expr));
    }

    @Override
    @Nullable
    public Object evalAll(@NotNull Iterable<Expression> iterable, boolean z) {
        budget.begin();
        if (z) {
            this.working++;
        }
        Object objValueOf = DOUBLE_ZERO;
        try {
            if (iterable instanceof List<Expression> list) {
                final int size = list.size();
                for (int i = 0; i < size; i++) {
                    objValueOf = this.visitBounded(list.get(i));
                    Object obj = popReturnValue();
                    if (obj != null) {
                        objValueOf = obj;
                        break;
                    }
                }
            } else {
                for (Expression expression : iterable) {
                    objValueOf = this.visitBounded(expression);
                    Object obj = popReturnValue();
                    if (obj != null) {
                        objValueOf = obj;
                        break;
                    }
                }
            }
            return objValueOf;
        } finally {
            this.returnValue = null;
            this.op = null;
            budget.end();
            if (z) {
                this.working--;
            }
        }
    }

    @NotNull
    public <TNewEntity> ExpressionEvaluatorImpl<TNewEntity> createChild(@Nullable TNewEntity tnewentity) {
        return new ExpressionEvaluatorImpl<>(tnewentity, this.budget);
    }

    @Nullable
    private Object popReturnValue() {
        Object obj = this.returnValue;
        if (this.working == 0) {
            this.returnValue = null;
        }
        return obj;
    }

    @Override
    @Nullable
    public Object visitCall(@NotNull CallExpression expression) {
        return expression.function().evaluate(this, expression.arguments());
    }

    @Override
    public Object visitFloat(@NotNull FloatExpression floatExpression) {
        return floatExpression.boxed();
    }

    @Override
    public Object visitExecutionScope(@NotNull ExecutionScopeExpression executionScope) {
        Object objMo2074xaffeef43 = null;
        final List<Expression> expressions = executionScope.expressions();
        final int size = expressions.size();
        for (int i = 0; i < size; i++) {
            objMo2074xaffeef43 = this.visitBounded(expressions.get(i));
            Object obj = popReturnValue();
            if (obj != null) {
                return obj;
            }
            if (this.cnt > 0 && this.op != null) {
                return null;
            }
        }
        return objMo2074xaffeef43;
    }

    private boolean buildExecutionScope(@NotNull ExecutionScopeExpression executionScope) {
        budget.step();
        this.cnt++;
        try {
            final List<Expression> expressions = executionScope.expressions();
            final int size = expressions.size();
            for (int i = 0; i < size; i++) {
                this.visitBounded(expressions.get(i));
                if (popReturnValue() != null) {
                    return true;
                }
                StatementExpression.Op op = this.op;
                this.op = null;
                if (op == StatementExpression.Op.CONTINUE) {
                    break;
                }
                if (op == StatementExpression.Op.BREAK) {
                    return true;
                }
            }
            return false;
        } finally {
            this.cnt--;
        }
    }

    public void loopFunciton(@NotNull ExecutionScopeExpression executionScope, int n) {
        for (int i = 0; i < n && !buildExecutionScope(executionScope); i++) {
        }
    }

    public void forEachFunction(@NotNull ExecutionScopeExpression executionScope, AssignableVariable variableAccess, Iterable<?> iterable) {
        Iterator<?> it = iterable.iterator();
        while (it.hasNext()) {
            variableAccess.assign(this, it.next());
            if (buildExecutionScope(executionScope)) {
                return;
            }
        }
    }

    @Override
    public Object visitIdentifier(@NotNull IdentifierExpression identifierExpression) {
        throw new RuntimeException("Unknown identifier type");
    }

    @Override
    public Object visitVariable(@NotNull VariableExpression expression) {
        return expression.target().evaluate(this);
    }

    @Override
    public Object visitAssignableVariable(@NotNull AssignableVariableExpression expression) {
        return expression.target().evaluate(this);
    }

    @Override
    public Object visitStruct(@NotNull StructAccessExpression expression) {
        Object value = this.visitBounded(expression.left());
        if (value instanceof Struct) {
            return ((Struct) value).getProperty(expression.path());
        } else {
            return null;
        }
    }

    @Override
    public Object visitBinary(@NotNull BinaryExpression expression) {
        return BINARY_EVALUATORS[expression.op().index()].eval(
                this,
                expression.left(),
                expression.right()
        );
    }

    @Override
    public Object visitBinaryOperation(BinaryOperationExpression expression) {
        Object objMo2074xaffeef43 = this.visitBounded(expression.getLeft());
        Object objMo2074xaffeef432 = this.visitBounded(expression.getRight());
        if (objMo2074xaffeef432 instanceof Number) {
            int iIntValue = ((Number) objMo2074xaffeef432).intValue();
            if (iIntValue < 0) {
                iIntValue = 0;
            }
            if (objMo2074xaffeef43 instanceof List list) {
                if (list.size() > iIntValue) {
                    return list.get(iIntValue);
                }
                return null;
            }
            return null;
        }
        return null;
    }

    @Override
    public Object visitUnary(@NotNull UnaryExpression expression) {
        Object value = this.visitBounded(expression.expression());
        switch (expression.op()) {
            case LOGICAL_NEGATION:
                return ValueConversions.asBoolean(value) ? Boolean.FALSE : Boolean.TRUE;
            case ARITHMETICAL_NEGATION:
                return -ValueConversions.asFloat(value);
            case RETURN: {
                this.returnValue = value;
                return DOUBLE_ZERO;
            }
            default:
                throw new IllegalStateException("Unknown operation");
        }
    }

    @Override
    public Object visitStatement(@NotNull StatementExpression expression) {
        switch (expression.op()) {
            case BREAK: {
                this.op = StatementExpression.Op.BREAK;
                break;
            }
            case CONTINUE: {
                this.op = StatementExpression.Op.CONTINUE;
                break;
            }
        }
        return null;
    }

    @Override
    public Object visitString(@NotNull StringExpression expression) {
        return expression;
    }

    @Override
    public Object visitTernaryConditional(@NotNull TernaryConditionalExpression expression) {
        Object obj = this.visitBounded(expression.condition());
        obj = ValueConversions.asBoolean(obj)
                ? this.visitBounded(expression.trueExpression())
                : this.visitBounded(expression.falseExpression());
        return obj;
    }

    @Override
    public Object visit(@NotNull Expression expression) {
        throw new UnsupportedOperationException("Unsupported expression type: " + expression);
    }

    private Object visitBounded(Expression expression) {
        budget.enter();
        try {
            return expression.visit(this);
        } finally {
            budget.leave();
        }
    }

    // 每次顶层求值共享节点预算，函数实参、嵌套 loop/for_each 和 arrow 子求值器不能重置它。
    private static final class EvaluationBudget {
        private static final int MAX_STEPS = 100_000;
        private static final int MAX_DEPTH = 128;
        private int active;
        private int remaining;
        private int depth;

        void begin() {
            if (active++ == 0) {
                remaining = MAX_STEPS;
                depth = 0;
            }
        }

        void end() {
            active--;
        }

        void step() {
            if (--remaining < 0) {
                throw new EvaluationLimitException("Molang execution step budget exceeded");
            }
        }

        void enter() {
            step();
            if (depth >= MAX_DEPTH) {
                throw new EvaluationLimitException("Molang evaluation nesting limit exceeded");
            }
            depth++;
        }

        void leave() {
            depth--;
        }
    }

    private interface Evaluator<TEntity> {
        Object eval(ExpressionEvaluatorImpl<TEntity> evaluator, Expression a, Expression b);
    }
}