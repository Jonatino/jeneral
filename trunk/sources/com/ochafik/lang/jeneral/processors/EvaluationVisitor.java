/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spoon.processing.FactoryAccessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.eval.SymbolicEvaluator;
import spoon.reflect.eval.SymbolicInstance;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.Query;

import com.ochafik.lang.jeneral.runtime.Methods;

public class EvaluationVisitor extends CtScanner {
	SpoonHelper helper;
	public EvaluationVisitor(FactoryAccessor factoryAccessor) {
		helper = new SpoonHelper(factoryAccessor);
	}
	SymbolicEvaluator evaluator;
	public SymbolicEvaluator getEvaluator() {
		if (evaluator == null)
			evaluator = helper.Eval().createSymbolicEvaluator();
		
		return evaluator;
	}
	
	@Override
	protected void exit(CtElement e) {
		/*super.exit(e);
		if (elementStack.isEmpty())
			for (Map.Entry<CtElement, CtElement> replacement : replacements.entrySet()) {
				replacement.getKey().replace(replacement.getValue());
			}*/
	}
	
	Map<CtElement, CtElement> replacements = new HashMap<CtElement, CtElement>();
	
	//CtTypeReference<?> stringType = Type().createReference(String.class);
	
	//CtElement replacement;
	
	@Override
	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		super.visitCtBinaryOperator(operator);
		
		CtExpression<?> left = operator.getLeftHandOperand(), right = operator.getRightHandOperand();
		
		boolean literals = (left instanceof CtLiteral) && (right instanceof CtLiteral);
		
		if (literals) {
			// Replacements that require both operands to be literals 
			if (operator.getKind() == BinaryOperatorKind.PLUS) {
				Object lv = ((CtLiteral<?>)left).getValue(), rv = ((CtLiteral<?>)right).getValue();
				if (lv != null && rv != null) {
					/// Concatenate strings
					if ((lv instanceof String) || (rv instanceof String))
						operator.replace(helper.Code().createLiteral(String.valueOf(lv) + String.valueOf(rv)));
					
					/// Add numbers
					//if ((lv instanceof Number) && (rv instanceof Number))
				}
			} else if (operator.getKind() == BinaryOperatorKind.LT) {
				
			}
		} else if (operator.getKind() == BinaryOperatorKind.INSTANCEOF) {
			// no need for literals here
		}
	}
	
	/// Make sure there is no concurrency exception when scanning some element after one of its children replaced itself.
	@Override
	public void scan(Collection<? extends CtElement> elements) {
		if ((elements != null))
			for (CtElement e : new ArrayList<CtElement>(elements))
				scan(e);
	}

	InlinerCache inlinerCache;
	InlinerCache getInlinerCache() {
		if (inlinerCache == null)
			inlinerCache = new InlinerCache(helper.getFactory(), this);
		return inlinerCache;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		super.visitCtInvocation(invocation);
		
		try {
			Inliner inliner = getInlinerCache().getInliner(invocation.getExecutable().getActualMethod());
			if (inliner != null) {
				inliner.process(invocation);
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try {
			if (!SpoonHelper.hasArgumentsWithAcceptableTypesForEval(invocation))
				return;
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		CtExecutableReference<T> exe = invocation.getExecutable();
		//System.out.println("Invocation = " + invocation);
		
		List<CtExpression<?>> arguments = invocation.getArguments();
		Class[] argsTypes = new Class[arguments.size()];
		Object[] evaluatedArguments = new Object[arguments.size()];
		for (int i = 0, len = evaluatedArguments.length; i < len; i++) {
			CtExpression<?> argument = arguments.get(i);
			CtTypeReference argTypeRef = argument.getType();
			
			try {
				evaluatedArguments[i] = eval(argument);
				if (argTypeRef != null)
					argsTypes[i] = argTypeRef.getActualClass();
				else if (evaluatedArguments[i] != null)
					argsTypes[i] = evaluatedArguments[i].getClass();
				else
					argsTypes[i] = Object.class;
				
			} catch (Exception e) {
				return;
			}
		}
		
		try {
			Class<?> class1;
			Object instance = null;
			
			String targetStr = invocation.getTarget() == null ? null : invocation.getTarget().toString().replace("\\s+", "");
			Class<?> classLiteral = TypeUtils.parseClassLiteral(targetStr);
			if (classLiteral != null) {
				class1 = Class.class; 
				instance = TypeUtils.wrapPrimitiveClass(classLiteral);
			} else {
				class1 = Class.forName(exe.getDeclaringType().getQualifiedName());
			}
			
			if (class1 != null) {
				class1 = TypeUtils.wrapPrimitiveClass(class1);
				//Method m = class1.getMethod(exe.getSimpleName(), argsTypes);
				//invocation.replace(Code().createLiteral(m.invoke(null, evaluatedArguments)));
				//invocation.replace(Code().createLiteral(Methods.invokeStatic(class1, exe.getSimpleName(), argsTypes, evaluatedArguments)));
				Method m = Methods.getMethodForArgs(class1, exe.getSimpleName(), evaluatedArguments);
				if ((m.getModifiers() & Modifier.STATIC) != 0) {
					invocation.replace(helper.Code().createLiteral(Methods.invokeStatic(class1, exe.getSimpleName(), evaluatedArguments)));
				} else if (instance != null)
					invocation.replace(helper.Code().createLiteral(Methods.invoke(instance, exe.getSimpleName(), evaluatedArguments)));
					
			}
				
		} catch (Exception e) {
			if (!exe.getSimpleName().contains("init>"))
				e.printStackTrace();
		}
	}
	public Object eval(CtExpression<?> argument) throws Exception {
		if (argument instanceof CtLiteral) {
			return ((CtLiteral<?>)argument).getValue();
		} else {
			SymbolicInstance<?> evaluatedArgument = getEvaluator().evaluate(argument);
			CtTypeReference<?> concreteType = evaluatedArgument.getConcreteType();
			if (concreteType != null && (evaluatedArgument instanceof CtLiteral))
				return ((CtLiteral<?>)evaluatedArgument).getValue();
			
			throw new Exception();
		}
	}
	@Override
	public void visitCtIf(CtIf ifElement) {
		super.visitCtIf(ifElement);
		
		CtExpression<Boolean> condition = ifElement.getCondition();
		if (condition instanceof CtLiteral) {
			helper.replaceByStatementOrBlockStatements(ifElement, Boolean.TRUE.equals(((CtLiteral<?>)condition).getValue()) ?
					ifElement.getThenStatement() : ifElement.getElseStatement());
		}
	}
}
