package com.ochafik.lang.jeneral.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spoon.processing.AbstractProcessor;
import spoon.reflect.CoreFactory;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.EnumFactory;
import spoon.reflect.factory.EvalFactory;
import spoon.reflect.factory.FieldFactory;
import spoon.reflect.factory.MethodFactory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

public abstract class AbstractSpoonProcessor<E extends CtElement> extends AbstractProcessor<E> {
	CoreFactory Core() { return getFactory().Core(); }
	CodeFactory Code() { return getFactory().Code(); }
	FieldFactory Field() { return getFactory().Field(); }
	EvalFactory Eval() { return getFactory().Eval(); }
	TypeFactory Type() { return getFactory().Type(); }
	MethodFactory Method() { return getFactory().Method(); }

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
	

	@SuppressWarnings("unchecked")
	protected <T> Class<T> getPrimitiveWrapper(Class<T> c) {
		return (Class<T>)primitiveToWrapperClass.get(c);
	}

	private static Map<Class<?>, Class<?>> primitiveToWrapperClass= new HashMap<Class<?>, Class<?>>();
	//static Map<String, Class<?>> primitiveToWrapperClass= new HashMap<String, Class<?>>();
	static Map<String, String> primitiveToClassAccess = new HashMap<String, String>();
	static {
		primitiveToClassAccess.put("int", "java.lang.Integer.TYPE");
		primitiveToClassAccess.put("long", "java.lang.Long.TYPE");
		primitiveToClassAccess.put("short", "java.lang.Short.TYPE");
		primitiveToClassAccess.put("byte", "java.lang.Byte.TYPE");
		primitiveToClassAccess.put("char", "java.lang.Character.TYPE");
		primitiveToClassAccess.put("float", "java.lang.Float.TYPE");
		primitiveToClassAccess.put("double", "java.lang.Double.TYPE");
/*
		primitiveToWrapperClass.put("int", Integer.class);
		primitiveToWrapperClass.put("long", Long.class);
		primitiveToWrapperClass.put("short", Short.class);
		primitiveToWrapperClass.put("byte", Byte.class);
		primitiveToWrapperClass.put("char", Character.class);
		primitiveToWrapperClass.put("float", Float.class);
		primitiveToWrapperClass.put("double", Double.class);
		*/
		primitiveToWrapperClass.put(Integer.TYPE, Integer.class);
		primitiveToWrapperClass.put(Long.TYPE, Long.class);
		primitiveToWrapperClass.put(Short.TYPE, Short.class);
		primitiveToWrapperClass.put(Byte.TYPE, Byte.class);
		primitiveToWrapperClass.put(Character.TYPE, Character.class);
		primitiveToWrapperClass.put(Float.TYPE, Float.class);
		primitiveToWrapperClass.put(Double.TYPE, Double.class);
	}

	public CtElement classAccess(Class<?> type, CtTypeReference<?> paramRefReplacement) {
		if (type.isPrimitive()) {
			return newSnippet(primitiveToClassAccess.get(type.getName()));
			/*try {
				Class<?> wrapper = getPrimitiveWrapper(type);
				CtFieldReference field = Field().createReference(wrapper.getField("TYPE"));
				CtFieldAccess<Object> fieldAccess = Core().createFieldAccess();
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
			}*/
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

	protected CtCodeSnippetExpression<Object> newSnippet(String string) {
		CtCodeSnippetExpression<Object> snippet = Core().createCodeSnippetExpression();
		snippet.setValue(string);
		return snippet;
	}

	@SuppressWarnings("unchecked")
	public <T> Class<T> getObjectWrapper(Class<T> classReplacement) {
		if (classReplacement.isPrimitive())
			return getPrimitiveWrapper(classReplacement);
		return classReplacement;
	}

	CtTypeReference<?> integerType, intType;
	public CtTypeReference<?> getIntegerType() {
		if (integerType == null)
			integerType = getFactory().Type().createReference(Integer.class);

		return integerType;
	}
	public CtTypeReference<?> getIntType() {
		if (intType == null)
			intType = getFactory().Type().createReference(Integer.TYPE);

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

	
}
