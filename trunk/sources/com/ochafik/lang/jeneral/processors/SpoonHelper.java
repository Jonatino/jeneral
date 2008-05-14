package com.ochafik.lang.jeneral.processors;
import java.util.List;

import spoon.processing.FactoryAccessor;
import spoon.reflect.CoreFactory;
import spoon.reflect.Factory;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.EvalFactory;
import spoon.reflect.factory.FieldFactory;
import spoon.reflect.factory.MethodFactory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public class SpoonHelper {
	final FactoryAccessor factoryAccessor;
	
	public SpoonHelper(FactoryAccessor factoryAccessor) {
		this.factoryAccessor = factoryAccessor;
	}

	CoreFactory Core() { return factoryAccessor.getFactory().Core(); }
	CodeFactory Code() { return factoryAccessor.getFactory().Code(); }
	FieldFactory Field() { return factoryAccessor.getFactory().Field(); }
	EvalFactory Eval() { return factoryAccessor.getFactory().Eval(); }
	TypeFactory Type() { return factoryAccessor.getFactory().Type(); }
	MethodFactory Method() { return factoryAccessor.getFactory().Method(); }

	public static <T extends CtElement> TypeFilter<T> typeFilter(Class<T> typeClass) {
		return new TypeFilter<T>(typeClass);
	}

	public void replace(CtElement element, List<CtStatement> replace) {
		CtElement parent = element.getParent();
		if (!(parent instanceof CtBlock)) {
			element.replace(newBlock(replace.toArray(new CtStatement[0])));
		} else {
			CtBlock<?> block = (CtBlock<?>)parent;
			int index = -1; 
			List<CtStatement> statements = block.getStatements();
			for (int i = 0, len = statements.size(); i < len; i++) {
				if (statements.get(i) == element) {
					index = i;
					break;
				}
			}
			if (index < 0)
				throw new RuntimeException("Statement " + element +" not found in its parent !");

			statements.remove(index);
			for (CtStatement r : replace)
				statements.add(index++, r);
		}
	}

	@SuppressWarnings("unchecked")
	public CtElement newArray(CtTypeReference paramArrayRefReplacement, List<CtExpression<?>> arguments) {
		CtNewArray newArray = Core().createNewArray();
		newArray.setType(paramArrayRefReplacement);
		newArray.setDimensionExpressions(arguments);
		return newArray;
	}

	public CtElement classAccess(Class<?> type, CtTypeReference<?> paramRefReplacement) {
		if (type.isPrimitive()) {
			//return newSnippet(primitiveToClassAccess.get(type.getName()));
			try {
				Class<?> wrapper = TypeUtils.wrapPrimitiveClass(type);
				CtFieldReference field = Field().createReference(wrapper.getField("TYPE"));
				CtFieldAccess<Object> fieldAccess = Core().createFieldAccess();
				//fieldAccess.set.setFinal(true);
				field.setFinal(true);
				field.setStatic(true);
				fieldAccess.setVariable(field);
				//Core().createt
				//fieldAccess.setTarget(Code().createClassAccess(Type().createReference(wrapper)));
				fieldAccess.setImplicit(false);
				fieldAccess.setType((CtTypeReference)Type().createReference(Class.class));
				
				//fieldAccess.set
				//return Code().createVariableAccess(field, true);
				return fieldAccess;
				//fieldAccess.setTarget(Type().createReference(getPrimitiveWrapper(type)));
				
				//Code().createThisAccess(type)
				//CtVariableAccess<Object> access = Code().createVariableAccess(field, true);
				//access.setImplicit(false);
				
				//return access;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else
			return Code().createClassAccess(paramRefReplacement);
	}

	@SuppressWarnings("unchecked")
	public CtStatement newFieldAssignment(CtField<?> field, String simpleName) {
		CtLocalVariableReference paramRef = Code().createLocalVariableReference(field.getType(), simpleName);
		return newAssignment(
				(CtFieldAccess)Code().createVariableAccess(Field().createReference(field), false), 
				Code().createVariableAccess(paramRef, false)
		);
	}

	@SuppressWarnings("unchecked")
	public CtStatement newAssignment(CtExpression dest, CtExpression source) {
		CtAssignment assignment = Core().createAssignment();
		assignment.setAssigned(dest);
		assignment.setAssignment(source);
		return assignment;
	}

	public String capitalize(String simpleName) {
		return simpleName.length() == 0 ? "" : Character.toUpperCase(simpleName.charAt(0)) + simpleName.substring(1);
	}


	<T> CtBlock<T> newBlock(CtStatement... statements) {
		CtBlock<T> block = Core().createBlock();
		for (CtStatement st : statements) {
			block.getStatements().add(st);
		}
		return block;
	}
	<T> CtReturn<T> newReturn(CtExpression<T> ex) {
		CtReturn<T> ctReturn = Core().createReturn();
		ctReturn.setReturnedExpression(ex);
		return ctReturn;
	}
	<T> CtParameter<T> newParam(CtTypeReference<T> type, String name) {
		CtParameter<T> param = Core().createParameter();
		param.setType(type);
		param.setSimpleName(name);

		return param;
	}

	public CtArrayAccess<Object, CtExpression<?>> newArrayAccess(CtExpression<?> target, CtExpression<Integer> index) {
		CtArrayAccess<Object, CtExpression<?>> arrayAccess = Core().createArrayAccess();
		arrayAccess.setTarget(target);
		arrayAccess.setIndexExpression(index);
		return arrayAccess;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getObjectWrapper(Class<T> classReplacement) {
		if (classReplacement.isPrimitive())
			return TypeUtils.wrapPrimitiveClass(classReplacement);
		return classReplacement;
	}

	CtTypeReference<?> integerType, intType;
	public CtTypeReference<?> getIntegerType() {
		if (integerType == null)
			integerType = factoryAccessor.getFactory().Type().createReference(Integer.class);

		return integerType;
	}
	public CtTypeReference<?> getIntType() {
		if (intType == null)
			intType = factoryAccessor.getFactory().Type().createReference(Integer.TYPE);

		return intType;
	}
	public boolean isInteger(CtExpression<?> target) {
		CtTypeReference<?> type = target.getType();
		return type.equals(getIntegerType()) || type.equals(getIntType());
	}

	public void replaceByStatementOrBlockStatements(CtElement e, CtStatement st) {
		if (st instanceof CtBlock) {
			replace(e, ((CtBlock<?>)st).getStatements());
		} else
			e.replace(st);
	}
	
	/*public void removeDeadBlocks(CtClass<?> ctClass) {
		/// Replace all typed elements T and Array<T> by actual param type and T[]
		for (CtIf ctIf : Query.getElements(ctClass, typeFilter(CtIf.class))) {
			CtExpression<Boolean> condition = ctIf.getCondition();
			if (condition instanceof CtLiteral) {
				replaceByStatementOrBlockStatements(ctIf, Boolean.TRUE.equals(((CtLiteral<?>)condition).getValue()) ?
					ctIf.getThenStatement() : ctIf.getElseStatement());
			}
		}
	}*/

	public static boolean acceptableEvalReturnValue(Class<?> expression) {
		//if (expression instanceof )
		return expression == Class.class || expression.isPrimitive() || expression == String.class;
	}
	public static boolean acceptableEvalReturnValue(CtTypedElement<?> e) {
		if (e instanceof CtLiteral)
			return true;
		return acceptableEvalReturnValue(e.getType());
	}
	public static boolean acceptableEvalReturnValue(CtTypeReference<?> c) {
		if (c == null)
			return false;
		
		String n = c.getQualifiedName();
		return c.isPrimitive() || 
			n.equals(Class.class.getName()) || 
			n.equals(String.class.getName());
	}
	
	public static boolean hasArgumentsWithAcceptableTypesForEval(CtInvocation<?> invocation) {
		CtExecutableReference<?> exe = invocation.getExecutable();
		if (!exe.getModifiers().contains(ModifierKind.STATIC) && acceptableEvalReturnValue(exe.getType()))
			return false;
		
		for (CtExpression<?> argument : invocation.getArguments())
			if (!acceptableEvalReturnValue(argument))
				return false;
		
		return true;
	}

	public Factory getFactory() {
		return factoryAccessor.getFactory();
	}
	
}

