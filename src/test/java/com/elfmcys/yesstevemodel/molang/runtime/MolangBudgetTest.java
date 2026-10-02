package com.elfmcys.yesstevemodel.molang.runtime;

import com.elfmcys.yesstevemodel.molang.parser.MolangParser;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.molang.parser.ast.*;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import com.elfmcys.yesstevemodel.molang.runtime.binding.StandardBindings;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.TempVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationControllerContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MolangBudgetTest {
    private static CallExpression loop(int count, Expression body) {
        return new CallExpression(StandardBindings.LOOP_FUNC, new Function.ArgumentCollection(List.of(
                new FloatExpression(count), new ExecutionScopeExpression(List.of(body)))));
    }

    @Test
    void ordinaryArithmeticAndSupportedLoopRoundCountStillWork() throws Exception {
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        List<Expression> expressions = MolangParser.parseExpressions("(2 + 3) * 4;", ObjectBinding.EMPTY);
        assertEquals(20.0f, evaluator.evalAsFloat(expressions.getFirst()));
        assertDoesNotThrow(() -> evaluator.eval(loop(1024, FloatExpression.ONE)));
        assertDoesNotThrow(() -> evaluator.evalAll(Collections.nCopies(1000, FloatExpression.ONE), false));
    }

    @Test
    void nestedLoopsCannotMultiplyPastSharedBudgetAndNextEvaluationResetsIt() {
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        Expression nested = loop(1024, loop(1024, FloatExpression.ONE));
        assertThrows(EvaluationLimitException.class, () -> evaluator.eval(nested));
        assertEquals(1.0f, evaluator.evalAsFloat(FloatExpression.ONE));
    }

    @Test
    void scriptsAndArrowChildrenShareOneTotalBudget() {
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        assertThrows(EvaluationLimitException.class,
                () -> evaluator.evalAll(Collections.nCopies(100_001, FloatExpression.ONE), false));
        Expression child = new BinaryExpression(BinaryExpression.Op.ARROW, FloatExpression.ONE,
                loop(1024, FloatExpression.ONE));
        assertThrows(EvaluationLimitException.class, () -> evaluator.eval(loop(1024, child)));
    }

    @Test
    void primitiveAndObjectEvaluationRejectDeepTreesWithoutStackOverflow() {
        Expression tree = FloatExpression.ONE;
        for (int i = 0; i < 140; i++) tree = new UnaryExpression(UnaryExpression.Op.ARITHMETICAL_NEGATION, tree);
        Expression deep = tree;
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        assertThrows(EvaluationLimitException.class, () -> evaluator.eval(deep));
        assertThrows(EvaluationLimitException.class, () -> evaluator.evalAsFloat(deep));
        assertThrows(EvaluationLimitException.class, () -> evaluator.evalAsBoolean(deep));
        assertThrows(EvaluationLimitException.class, () -> evaluator.evalSafe(deep));
        assertEquals(1.0f, evaluator.evalAsFloat(FloatExpression.ONE));
    }

    @Test
    void parserRejectsExcessiveRecursionWithNormalParseException() {
        String input = "(".repeat(140) + "1" + ")".repeat(140);
        assertThrows(ParseException.class, () -> MolangParser.parseExpressions(input, ObjectBinding.EMPTY));
    }

    @Test
    void emptyNestedLoopsAndFunctionArgumentsAreChargedToTheSameBudget() {
        Expression empty = new CallExpression(StandardBindings.LOOP_FUNC, new Function.ArgumentCollection(List.of(
                new FloatExpression(1024), new ExecutionScopeExpression(List.of()))));
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        assertThrows(EvaluationLimitException.class, () -> evaluator.eval(loop(1024, empty)));
        Expression argument = new CallExpression((ctx, args) -> args.getValue(ctx, 0),
                new Function.ArgumentCollection(List.of(loop(1024, loop(1024, FloatExpression.ONE)))));
        assertThrows(EvaluationLimitException.class, () -> evaluator.eval(argument));
    }

    @Test
    void failedFunctionArgumentsRollBackTemporaryScope() {
        TempVariableStorage storage = new TempVariableStorage();
        storage.setElement(0, "outer");
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        Function.ArgumentCollection badArgs = new Function.ArgumentCollection(List.of(FloatExpression.ONE,
                loop(1024, loop(1024, FloatExpression.ONE))));
        assertThrows(EvaluationLimitException.class, () -> storage.pushScopeWithArgs(evaluator, badArgs));
        assertEquals("outer", storage.getElement(0));
        assertNull(storage.getElement(1));
        assertTrue(storage.pushScopeWithArgs(evaluator, new Function.ArgumentCollection(List.of(FloatExpression.ONE))));
        assertEquals(1.0f, storage.asList().getFirst());
        storage.popScope();
        assertEquals("outer", storage.getElement(0));
        assertNull(storage.getElement(1));
    }

    @Test
    void failedDeferredArgumentsDoNotLeaveAnExtraRenderLayer() throws Exception {
        AnimationControllerContext controller = new AnimationControllerContext();
        ExpressionEvaluator<Object> evaluator = ExpressionEvaluator.evaluator(null);
        Function.ArgumentCollection good = new Function.ArgumentCollection(List.of(FloatExpression.ONE));
        controller.captureArguments(evaluator, 0, good, 0);
        Function.ArgumentCollection bad = new Function.ArgumentCollection(List.of(FloatExpression.ONE,
                loop(1024, loop(1024, FloatExpression.ONE))));
        assertThrows(EvaluationLimitException.class, () -> controller.captureArguments(evaluator, 0, bad, 0));
        var count = AnimationControllerContext.class.getDeclaredField("captureCount");
        count.setAccessible(true);
        assertEquals(1, count.getInt(controller));
        controller.captureArguments(evaluator, 0, good, 0);
        assertEquals(2, count.getInt(controller));
    }
}
