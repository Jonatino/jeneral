package com.ochafik.lang.jeneral.processors;

import java.util.List;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.eval.SymbolicEvaluator;
import spoon.reflect.eval.SymbolicInstance;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

public abstract class Inliner extends spoon.processing.AbstractProcessor<CtInvocation<?>> {
	
	public Inliner() {
		super();
	}
	
	EvaluationVisitor evaluationVisitor;
	public void setEvaluationVisitor(EvaluationVisitor evaluationVisitor) {
		this.evaluationVisitor = evaluationVisitor;
	}
	public EvaluationVisitor getEvaluationVisitor() {
		return evaluationVisitor;
	}
	
	SpoonHelper helper;
	public SpoonHelper getHelper() {
		if (helper == null) {
			helper = new SpoonHelper(this);
		}
		return helper;
	}
	String tag;
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}
	/*
	SymbolicEvaluator evaluator;
	public SymbolicEvaluator getEvaluator() {
		if (evaluator == null)
			evaluator = getFactory().Eval().createSymbolicEvaluator();
		return evaluator;
	}
	protected boolean mayEval(CtTypedElement<?> e) {
		if (e instanceof CtLiteral)
			return true;
		return mayEval(e.getType());
	}
	protected boolean mayEval(CtTypeReference<?> c) {
		if (c == null)
			return false;
		
		String n = c.getQualifiedName();
		return c.isPrimitive() || 
			n.equals(Class.class.getName()) || 
			n.equals(String.class.getName());
	}
	
	protected boolean mayEval(CtInvocation<?> invocation) {
		CtExecutableReference<?> exe = invocation.getExecutable();
		if (!exe.getModifiers().contains(ModifierKind.STATIC) && mayEval(exe.getType()))
			return false;
		
		for (CtExpression<?> argument : invocation.getArguments())
			if (!mayEval(argument))
				return false;
		
		return true;
	}
	protected Object eval(CtExpression<?> argument) throws Exception {
		if (argument instanceof CtLiteral) {
			return ((CtLiteral<?>)argument).getValue();
		} else {
			SymbolicInstance<?> evaluatedArgument = getEvaluator().evaluate(argument);
			CtTypeReference<?> concreteType = evaluatedArgument.getConcreteType();
			if (concreteType != null && (evaluatedArgument instanceof CtLiteral))
				return ((CtLiteral<?>)evaluatedArgument).getValue();
			
			throw new Exception("Dunno how to eval expr of type " + argument.getClass().getName() + " : " +argument);
		}
	}
	
	protected Object[] eval(List<CtExpression<?>> arguments) {
		Object[] evaluatedArguments = new Object[arguments.size()];
		for (int i = 0, len = evaluatedArguments.length; i < len; i++) {
			CtExpression<?> argument = arguments.get(i);
			if (!mayEval(argument))
				return null;
			
			try {
				evaluatedArguments[i] = eval(argument);
			} catch (Exception e) {
				return null;
			}
		}
		return evaluatedArguments;
	}
	*/
}
