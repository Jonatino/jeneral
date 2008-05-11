/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import spoon.processing.AbstractProcessor;
import spoon.reflect.CoreFactory;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.FieldFactory;
import spoon.reflect.factory.MethodFactory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

import com.ochafik.lang.jeneral.Array;
import com.ochafik.lang.jeneral.TemplateContractViolationException;
import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.processors.InstantiationUtils.InstantiationResult;
import com.ochafik.util.string.StringUtils;

public class TemplateInstantiator extends AbstractProcessor<CtClass<?>> {
	List<CtParameter<?>> noParams = new ArrayList<CtParameter<?>>();
	Set<CtTypeReference<? extends Throwable>> noExceptions = new TreeSet<CtTypeReference<? extends Throwable>>();
	Set<InstantiationResult> results = new TreeSet<InstantiationResult>();
	Map<String, InstantiationParams> templateClassNameToParams = new HashMap<String, InstantiationParams>();
	
	public TemplateInstantiator(Set<InstantiationParams> params) {
		for (InstantiationParams param : params) {
			templateClassNameToParams.put(param.templateQualifiedName, param);
		}
	}

	public boolean removeTemplateInterface(CtClass<?> ctClass) {
		String tempQualName = InstantiationUtils.getTemplateInterfaceQualifiedName(ctClass.getQualifiedName());
		
		Set<CtTypeReference<?>> superInterfaces = ctClass.getSuperInterfaces();
		for (CtTypeReference<?> superInterface : superInterfaces) {
			if (superInterface.getQualifiedName().equals(tempQualName)) {
				superInterfaces.remove(superInterface);
				return true;
			}
		}
		
		return false;
	}
	
	public CtInterface<?> getTemplateInterface(CtClass<?> ctClass) {
		String tempQualName = InstantiationUtils.getTemplateInterfaceQualifiedName(ctClass.getQualifiedName());
		
		Set<CtTypeReference<?>> superInterfaces = ctClass.getSuperInterfaces();
		for (CtTypeReference<?> superInterface : superInterfaces) {
			if (superInterface.getQualifiedName().equals(tempQualName)) {
				return (CtInterface<?>)superInterface.getDeclaration();
			}
		}
		return null;
	}
	
	public <T extends Annotation> T getAndRemoveAnnotation(CtElement element, Class<T> annotationClass) {
		T a = element.getAnnotation(annotationClass);
		if (a == null)
			return null;
		
		for (CtAnnotation<? extends Annotation> annotation : element.getAnnotations()) {
			if (annotation.getAnnotationType().getQualifiedName().equals(annotationClass.getName())) {
				element.getAnnotations().remove(annotation);
				return a;
			}
		}
		throw new RuntimeException("Could not find annotation to remove from element " + element.getSignature() + " : " + annotationClass.getName());
	}
	
	public String decorateName(CtClass<?> ctClass, Map<String, Object> params) {
		StringBuilder b = new StringBuilder(ctClass.getSimpleName());
		for (CtTypeReference<?> ref : ctClass.getFormalTypeParameters()) {
			Object value = params.get(ref.getSimpleName());

			b.append("__");
			if (value != null) {
				if (value instanceof Class) {
					b.append(((Class<?>)value).getSimpleName());
				} else {
					b.append(value);
				}
			}	
		}
		return b.toString();
	}
	
	public void removeProvidedFormalParameters(CtClass<?> ctClass, Map<String, Object> params) {
		List<CtTypeReference<?>> formalTypeParameters = ctClass.getFormalTypeParameters();
		for (int i = formalTypeParameters.size(); i-- != 0;) {
			if (params.get(formalTypeParameters.get(i).getSimpleName()) != null)
				formalTypeParameters.remove(i);
		}
	}

	class ReplacedTypeInfo {
		public final Class<?> classReplacement;
		public final CtTypeReference<?> paramRefReplacement;
		public final CtTypeReference<?> paramObjectRefReplacement;
		public final CtTypeReference<?> paramArrayRefReplacement;
		public ReplacedTypeInfo(CtTypeReference<?> formalParamRef, Object formalParamValue) {
			if (!(formalParamValue instanceof Class))
				throw new RuntimeException("Expected class value for template parameter " + formalParamRef.getSimpleName()+". Found " + formalParamValue + " instead.");
			classReplacement = (Class<?>)formalParamValue;
			paramRefReplacement = type().createReference(classReplacement);
			if (classReplacement.isPrimitive()) {
				paramObjectRefReplacement = type().createReference(primitiveToWrapperClass.get(classReplacement.getName()));
			} else {
				paramObjectRefReplacement = paramRefReplacement;
			}
			paramArrayRefReplacement = type().createArrayReference(paramRefReplacement);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void process(CtClass<?> ctClass) {
		Template templateAnnotation = getAndRemoveAnnotation(ctClass, Template.class);
		if (templateAnnotation == null)
			return;
		
		String templateClassName = ctClass.getQualifiedName();
		InstantiationParams instantiationParams = templateClassNameToParams.get(templateClassName);
		
		List<CtTypeReference<?>> formalParameters = ctClass.getFormalTypeParameters();
		
		handleProperties(ctClass);
		
		List<CtMethod<?>> constructorContracts = getAndRemoveConstructorContracts(ctClass);
		
		if (!removeTemplateInterface(ctClass)) {
			System.out.println("Could not find template super interface declaration !");
		}
	
		/// Build info on replaced types
		Map<CtTypeReference<?>, ReplacedTypeInfo> replacedTypeInfos = new HashMap<CtTypeReference<?>, ReplacedTypeInfo>();
		for (CtTypeReference<?> formalParamRef : formalParameters) {
			Object formalParamValue = instantiationParams.templateParameters.get(formalParamRef.getSimpleName());
			if (formalParamValue == null) 
				continue;
			
			replacedTypeInfos.put(formalParamRef, new ReplacedTypeInfo(formalParamRef, formalParamValue));
		}
		
		/// Replace Class<T> methods
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			String exeSimpleName = invocation.getExecutable().getSimpleName();
			List<CtExpression<?>> args = invocation.getArguments();
			CtExpression<?> target = invocation.getTarget();
			if (target != null) {
				/// Replace Array.set(int, T), .get(int) and length()
				int nArgs = args.size();
				CtTypeReference<?> targetFormalParamType = getFormalParamClassType(target.getType());
				if (targetFormalParamType != null) {
					ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(targetFormalParamType);
					CtElement parent = invocation.getParent();
					if (replacedTypeInfo != null && (parent instanceof CtInvocation)) {
						CtInvocation parentInvocation = (CtInvocation)parent;
						List<CtExpression<?>> parentArgs = parentInvocation.getArguments();
						if (parentArgs.size() > 0) {
							CtExpression memberInvocationTarget = parentArgs.get(0);
							String parentExeSimpleName = parentInvocation.getExecutable().getSimpleName();
							if (parentExeSimpleName.equals("get") && exeSimpleName.equals("getField") && nArgs == 1 && parentArgs.size() == 1) {
								CtExpression fieldName = args.get(0);
								if (fieldName instanceof CtLiteral) {
									String fieldNameStr = ((CtLiteral)fieldName).getValue().toString();
									if ((memberInvocationTarget instanceof CtLiteral) && ((CtLiteral)memberInvocationTarget).getValue() == null)
										parentInvocation.replace(newSnippet(getObjectWrapper(replacedTypeInfo.classReplacement).getName() + "." + fieldNameStr));
									else
										parentInvocation.replace(newSnippet(memberInvocationTarget + "." + fieldNameStr));
									continue;
								}
							} /*else if (parentExeSimpleName.equals("get") && exeSimpleName.equals("getMethod") && nArgs == 1) {
								CtExpression methodName = args.get(0);
								if (fieldName instanceof CtLiteral) {
									String fieldNameStr = ((CtLiteral)fieldName).getValue().toString();
									if ((memberInvocationTarget instanceof CtLiteral) && ((CtLiteral)memberInvocationTarget).getValue() == null)
										parentInvocation.replace(newSnippet(getObjectWrapper(replacedTypeInfo.classReplacement).getName() + "." + fieldNameStr));
									else
										parentInvocation.replace(newSnippet(memberInvocationTarget + "." + fieldNameStr));
									continue;
								}
								//invocation.replace(newArrayAccess(target, (CtExpression<Integer>)args.get(0)));
								continue;
							}*/
						}
					}
				}
			}
		}
		
		
		/// Replace Array<T> methods
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			String exeSimpleName = invocation.getExecutable().getSimpleName();
			List<CtExpression<?>> args = invocation.getArguments();
			CtExpression<?> target = invocation.getTarget();
			if (target != null) {
				/// Replace Array.set(int, T), .get(int) and length()
				int nArgs = args.size();
				CtTypeReference<?> targetFormalParamType = getFormalParamArrayType(target.getType());
				if (targetFormalParamType != null) {
					if (exeSimpleName.equals("set") && nArgs == 2) {
						invocation.replace(newAssignment(newArrayAccess(target, (CtExpression<Integer>)args.get(0)), args.get(1)));
						continue;
					} else if (exeSimpleName.equals("get") && nArgs == 1) {
						invocation.replace(newArrayAccess(target, (CtExpression<Integer>)args.get(0)));
						continue;
					} else if (exeSimpleName.equals("length") && nArgs == 0) {
						invocation.replace(newSnippet("(" + target + ").length"));
						continue;
					} else if (exeSimpleName.equals("getClass") && nArgs == 0) {
						ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(targetFormalParamType);
						invocation.replace(newSnippet(replacedTypeInfo.classReplacement.getName() + "[].class"));
						continue;
					} else {
						throw new RuntimeException("Unhandled method in " + Array.class.getName() + ": " + exeSimpleName);
					}
				}
			}
		}
		
		/// Replace T.equals(T) by T == T (if T.isPrimitive()), and T.compareTo(T) by T < T, T >= T.... (where applicable)
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			String exeSimpleName = invocation.getExecutable().getSimpleName();
			List<CtExpression<?>> args = invocation.getArguments();
			CtExpression<?> target = invocation.getTarget();
			if (target == null)
				continue;
			
			CtTypeReference<?> targetFormalParamType = target.getType();
			ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(targetFormalParamType);
			if (replacedTypeInfo == null)
				continue;
			
			int nArgs = args.size();
			
			if (exeSimpleName.equals("getClass") && nArgs == 0) {
				invocation.replace(classAccess(replacedTypeInfo.classReplacement, replacedTypeInfo.paramRefReplacement));
				continue;
			}
			
			/// Replace compareTo and equals by operators on primitives
			if (replacedTypeInfo.classReplacement.isPrimitive()) {
				if (exeSimpleName.equals("equals") && nArgs == 1) {
					invocation.replace(code().createBinaryOperator(target, args.get(0), BinaryOperatorKind.EQ));
					continue;
				} else if (exeSimpleName.equals("compareTo") && nArgs == 1) {
					if (replaceCompareToInBinaryOperatorByDirectPrimitiveOperator(invocation))
						continue;
					
				} else if (exeSimpleName.equals("hashCode") && nArgs == 0) {
					if (isInteger(target))
						invocation.replace(target);
					else
						invocation.replace(newSnippet("(int)" + target));
					continue;
				} else {
					throw new RuntimeException("Unhandled method in " + Array.class.getName() + ": " + exeSimpleName);
				}
			}
		}
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			String exeSimpleName = invocation.getExecutable().getSimpleName();
			List<CtExpression<?>> args = invocation.getArguments();
		
			for (Map.Entry<CtTypeReference<?>, ReplacedTypeInfo> e : replacedTypeInfos.entrySet()) {
				ReplacedTypeInfo replacedTypeInfo = e.getValue();
				
				String formalParamName = e.getKey().getSimpleName();
				if (exeSimpleName.equals(formalParamName)) {
					if (args.size() == 0) {
						/// Replace T() by T.class
						invocation.replace(classAccess(replacedTypeInfo.classReplacement, replacedTypeInfo.paramRefReplacement));
						break;
					} else if (args.size() == 1) {
						/// Replace T(int) by new T[int]
						invocation.replace(newArray(replacedTypeInfo.paramArrayRefReplacement, invocation.getArguments()));
						break;
					}
				}
			}
		}
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			List<CtExpression<?>> args = invocation.getArguments();
		
			/// Replace T new_T(...)
			for (CtMethod<?> constructorContract : constructorContracts) {
				if (!matchesContract(invocation, constructorContract)) 
					continue;
				
				CtTypeReference<?> typeRef = constructorContract.getType();
				ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(typeRef);
				if (replacedTypeInfo.classReplacement.isPrimitive()) {
					if (args.size() == 0) {
						invocation.replace(newSnippet(
								replacedTypeInfo.classReplacement.isAssignableFrom(Integer.TYPE) ? 
								"0" : 
								"(" + replacedTypeInfo.classReplacement.getSimpleName() + ")0")); 
					} else if (args.size() == 1) {
						CtExpression singleArg = args.get(0);
						if (replacedTypeInfo.paramRefReplacement.isAssignableFrom(singleArg.getType()))
							invocation.replace(singleArg);
						else
							invocation.replace(newSnippet("(" + replacedTypeInfo.classReplacement.getSimpleName() + ")" + args.get(0)));
					} else {
						throw new TemplateContractViolationException("Primitive types have only two constructors : a default (zero-valued) one, and one with a single argument that has to be castable to the primitive type.");
					}
				} else {
					ParamConstructor paramConstructorAnnotation = constructorContract.getAnnotation(ParamConstructor.class);
					if (paramConstructorAnnotation.returnNeutralValue())
						invocation.replace(newSnippet("(" + replacedTypeInfo.classReplacement.getName() + ")null"));
					else 
						invocation.replace(newSnippet("new " + replacedTypeInfo.classReplacement.getName() + "(" + StringUtils.implode(invocation.getArguments(), ", ") + ")"));
				}
				break;
			}
		}
		
		/// Replace all typed elements T and Array<T> by actual param type and T[]
		for (CtTypedElement<?> typedElement : Query.getElements(ctClass, typeFilter(CtTypedElement.class))) {
			CtTypeReference<?> typedElementType = typedElement.getType();
			if (typedElementType == null)
				continue;
			
			CtTypeReference typeReplacement = getTorTArrayReplacement(typedElementType, false, replacedTypeInfos);
			if (typeReplacement != null) {
				typedElement.setType(typeReplacement);
				continue;
			}
			
			List<CtTypeReference<?>> typeArguments = typedElementType.getActualTypeArguments();
			List<CtTypeReference<?>> replacedTypeArgs = new ArrayList<CtTypeReference<?>>(typeArguments);
			boolean replacedSome = false;
			for (int i = 0, len = replacedTypeArgs.size(); i < len; i++) {
				typeReplacement = getTorTArrayReplacement(typeArguments.get(i), true, replacedTypeInfos);
				if (typeReplacement != null) {
					replacedTypeArgs.set(i, typeReplacement);
					replacedSome = true;
				}
			}
			if (replacedSome) {
				typedElementType.setActualTypeArguments(replacedTypeArgs);
			}
		}

		/// Change name of class to decorated instantiation name
		if (instantiationParams.overriddenSimpleName == null) {
			ctClass.setSimpleName(decorateName(ctClass, instantiationParams.templateParameters));
		} else {
			ctClass.setSimpleName(instantiationParams.overriddenSimpleName);
		}
		CtTypeReference<?> decoratedRef = ctClass.getReference();
		
		/// Change enclosing class of all template fields access to instantiated type
		for (CtFieldAccess<?> fieldAccess : Query.getElements(ctClass, typeFilter(CtFieldAccess.class))) {
			CtTypeReference<?> ref = fieldAccess.getVariable().getDeclaringType();
			if (ref != null && ref.getQualifiedName().equals(templateClassName)) {
				fieldAccess.getVariable().setDeclaringType(decoratedRef);
			}
		}
		
		/// Change enclosing class of all template method calls to instantiated type
		for (CtInvocation<?> invocation : Query.getElements(ctClass, typeFilter(CtInvocation.class))) {
			CtTypeReference<?> ref = invocation.getExecutable().getDeclaringType();
			if (ref != null && ref.getQualifiedName().equals(templateClassName))
				invocation.getExecutable().setDeclaringType(decoratedRef);
		}
		
		ctClass.getModifiers().remove(ModifierKind.ABSTRACT);
		removeProvidedFormalParameters(ctClass, instantiationParams.templateParameters);
		
		InstantiationResult result = new InstantiationResult(instantiationParams);
		result.qualifiedName = ctClass.getQualifiedName();
		result.sourceCode = ctClass.getPackage() + "\n" + ctClass;
		
		results.add(result);
	}
	
	private Class getObjectWrapper(Class<?> classReplacement) {
		if (classReplacement.isPrimitive())
			return primitiveToWrapperClass.get(classReplacement.getName());
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
	private boolean isInteger(CtExpression<?> target) {
		CtTypeReference<?> type = target.getType();
		return type.equals(getIntegerType()) || type.equals(getIntType());
	}

	private CtTypeReference<?> getTorTArrayReplacement(CtTypeReference<?> typeRef, boolean replacePrimitivesByWrappers, Map<CtTypeReference<?>, ReplacedTypeInfo> replacedTypeInfos) {
		CtTypeReference<?> formalType = getFormalParamArrayType(typeRef);
		if (formalType != null) {
			/// Replace Array<T>
			ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(formalType);
			if (replacedTypeInfo != null) {
				return replacedTypeInfo.paramArrayRefReplacement;
			}
		}
		
		/// Replace T
		ReplacedTypeInfo replacedTypeInfo = replacedTypeInfos.get(typeRef);
		if (replacedTypeInfo != null) {
			return replacePrimitivesByWrappers ? 
				replacedTypeInfo.paramObjectRefReplacement : 
				replacedTypeInfo.paramRefReplacement;
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	private CtElement newArray(CtTypeReference paramArrayRefReplacement, List<CtExpression<?>> arguments) {
		CtNewArray newArray = core().createNewArray();
		newArray.setType(paramArrayRefReplacement);
		newArray.setDimensionExpressions(arguments);
		return newArray;
	}

	static Map<String, Class<?>> primitiveToWrapperClass= new HashMap<String, Class<?>>();
	static Map<String, String> primitiveToClassAccess = new HashMap<String, String>();
	static {
		primitiveToClassAccess.put("int", "java.lang.Integer.TYPE");
		primitiveToClassAccess.put("long", "java.lang.Long.TYPE");
		primitiveToClassAccess.put("short", "java.lang.Short.TYPE");
		primitiveToClassAccess.put("byte", "java.lang.Byte.TYPE");
		primitiveToClassAccess.put("char", "java.lang.Character.TYPE");
		primitiveToClassAccess.put("float", "java.lang.Float.TYPE");
		primitiveToClassAccess.put("double", "java.lang.Double.TYPE");
		
		primitiveToWrapperClass.put("int", Integer.class);
		primitiveToWrapperClass.put("long", Long.class);
		primitiveToWrapperClass.put("short", Short.class);
		primitiveToWrapperClass.put("byte", Byte.class);
		primitiveToWrapperClass.put("char", Character.class);
		primitiveToWrapperClass.put("float", Float.class);
		primitiveToWrapperClass.put("double", Double.class);
	}
	
	private CtElement classAccess(Class<?> type, CtTypeReference<?> paramRefReplacement) {
		if (type.isPrimitive()) {
			return newSnippet(primitiveToClassAccess.get(type.getName()));
		} else
			return code().createClassAccess(paramRefReplacement);
	}

	@SuppressWarnings("unchecked")
	private boolean replaceCompareToInBinaryOperatorByDirectPrimitiveOperator(CtInvocation invocation) {
		CtElement parent = invocation.getParent();
		CtExpression target = invocation.getTarget();
		if (parent == null || target == null || !(parent instanceof CtBinaryOperator))
			return false;
		
		CtBinaryOperator parentOp = (CtBinaryOperator)parent;
		CtExpression left = parentOp.getLeftHandOperand(),
			right = parentOp.getRightHandOperand();
		
		List<CtExpression<?>> args = invocation.getArguments();
		if (args.size() != 1)
			return false;
		
		return replaceCompareToInBinaryOperatorByDirectPrimitiveOperator(invocation, target, args.get(0), (CtBinaryOperator)parent, left, right);
	}

	BinaryOperatorKind inverseKind(BinaryOperatorKind kind) {
		switch (kind) {
		case EQ:
			return BinaryOperatorKind.NE;
		case LE:
			return BinaryOperatorKind.GT;
		case LT:
			return BinaryOperatorKind.GE;
		case GE:
			return BinaryOperatorKind.LT;
		case GT:
			return BinaryOperatorKind.LE;
		case NE:
			return BinaryOperatorKind.EQ;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private boolean replaceCompareToInBinaryOperatorByDirectPrimitiveOperator(CtInvocation invocation, CtExpression target, CtExpression argument, CtBinaryOperator parent, CtExpression left, CtExpression right) {
		if (right instanceof CtLiteral) {
			CtLiteral rightLiteral = (CtLiteral)right;
			if (new Integer(0).equals(rightLiteral.getValue())) {
				switch (parent.getKind()) {
				case EQ:
				case LE:
				case LT:
				case GE:
				case GT:
				case NE:
					parent.replace(code().createBinaryOperator(target, argument, parent.getKind()));
					return true;
				}
				
			}
		}
		if (left instanceof CtLiteral) {
			CtLiteral leftLiteral = (CtLiteral)left;
			if (new Integer(0).equals(leftLiteral.getValue())) {
				switch (parent.getKind()) {
				case EQ:
				case LE:
				case LT:
				case GE:
				case GT:
				case NE:
					parent.replace(code().createBinaryOperator(argument, target, parent.getKind()));
					return true;
				}
				
			}
		}
		
		return false;
	}

	CoreFactory core() { return getFactory().Core(); }
	CodeFactory code() { return getFactory().Code(); }
	FieldFactory field() { return getFactory().Field(); }
	TypeFactory type() { return getFactory().Type(); }
	MethodFactory method() { return getFactory().Method(); }
	
	public static <T extends CtElement> TypeFilter<T> typeFilter(Class<T> typeClass) {
		return new TypeFilter<T>(typeClass);
	}

	private boolean matchesContract(CtInvocation<?> invocation, CtMethod<?> constructorContract) {
		if (!invocation.getExecutable().getSimpleName().equals(constructorContract.getSimpleName()))
			return false;
		
		List<CtExpression<?>> args = invocation.getArguments();
		int len = args.size();
		List<CtParameter<?>> params = constructorContract.getParameters();
		if (params.size() != len)
			return false;
		for (int i = 0; i < len; i++) {
			if (!args.get(i).getType().equals(params.get(i).getType()))
				return false;
		}
		return true;
	}

	private CtCodeSnippetExpression<Object> newSnippet(String string) {
		CtCodeSnippetExpression<Object> snippet = core().createCodeSnippetExpression();
		snippet.setValue(string);
		return snippet;
	}

	private CtTypeReference<?> getFormalParamArrayType(CtTypeReference<?> type) {
		if (type != null && type.getQualifiedName().equals(Array.class.getName())) {
			List<CtTypeReference<?>> typeArguments = type.getActualTypeArguments();
			if (typeArguments.size() == 1) {
				return typeArguments.get(0);
			}
		}
		return null;
	}
	private CtTypeReference<?> getFormalParamClassType(CtTypeReference<?> type) {
		if (type != null && type.getQualifiedName().equals(Class.class.getName())) {
			List<CtTypeReference<?>> typeArguments = type.getActualTypeArguments();
			if (typeArguments.size() == 1) {
				return typeArguments.get(0);
			}
		}
		return null;
	}

	private CtArrayAccess<Object, CtExpression<?>> newArrayAccess(CtExpression<?> target, CtExpression<Integer> index) {
		CtArrayAccess<Object, CtExpression<?>> arrayAccess = core().createArrayAccess();
		arrayAccess.setTarget(target);
		arrayAccess.setIndexExpression(index);
		return arrayAccess;
	}

	private List<CtMethod<?>> getAndRemoveConstructorContracts(CtClass<?> ctClass) {
		List<CtMethod<?>> constructorContracts = new ArrayList<CtMethod<?>>();
		for (CtMethod<?> method : ctClass.getMethods()) {
			ParamConstructor paramConstructorAnnotation = method.getAnnotation(ParamConstructor.class);
			if (paramConstructorAnnotation == null)
				continue;
			
			constructorContracts.add(method);
			if (!method.getModifiers().contains(ModifierKind.ABSTRACT)) {
				System.out.println("Param Constructor " + method.getSignature() + " should be abstract !"); 
			}
		}
		ctClass.getMethods().removeAll(constructorContracts);
		return constructorContracts;
	}

	private void handleProperties(CtClass<?> ctClass) {
		for (CtField<?> field : ctClass.getFields()) {
			Property propertyAnno = getAndRemoveAnnotation(field, Property.class);
			if (propertyAnno == null)
				continue;
			
			addGetterAndSetter(field, propertyAnno, ctClass);
			
			if (propertyAnno.construct()) {
				for (CtConstructor<?> ctor : ctClass.getConstructors()) {
					String argName = field.getSimpleName();
					ctor.getParameters().add(newParam(field.getType(), argName));
					List<CtStatement> statements =  ctor.getBody().getStatements();
					int i = 0, n = statements.size();
					int iInsertion = 0;
					for (; i < n; i++) {
						CtStatement st = statements.get(i);
						if (st instanceof CtInvocation) {
							CtInvocation<?> inv = (CtInvocation<?>)st;
							if (inv.getTarget() instanceof CtConstructor) {
								iInsertion = i + 1;
								break;
							}
						}
					}
					ctor.setImplicit(false);
					statements.add(iInsertion, newFieldAssignment(field, argName));
					
					//ct.getBody().getStatements().add(0
				}
			}
			// TODO add to constructors
		}
		
	}

	<T> CtBlock<T> newBlock(CtStatement... statements) {
		CtBlock<T> block = core().createBlock();
		for (CtStatement st : statements) {
			block.getStatements().add(st);
		}
		return block;
	}
	<T> CtReturn<T> newReturn(CtExpression<T> ex) {
		CtReturn<T> ctReturn = core().createReturn();
		ctReturn.setReturnedExpression(ex);
		return ctReturn;
	}
	<T> CtParameter<T> newParam(CtTypeReference<T> type, String name) {
		CtParameter<T> param = core().createParameter();
		param.setType(type);
		param.setSimpleName(name);
		
		return param;
	}
	
	@SuppressWarnings("unchecked")
	private void addGetterAndSetter(CtField<?> field, Property propertyAnno, CtClass<?> ctClass) {
		String capitalizedFieldName = capitalize(field.getSimpleName());
		String getterName = "get" + capitalizedFieldName,
			setterName = "set" + capitalizedFieldName;
		
		Set<ModifierKind> pub = Collections.singleton(ModifierKind.PUBLIC);
		
		CtFieldReference fieldRef = field().createReference(field);
		CtFieldAccess fieldAccess = (CtFieldAccess)code().createVariableAccess(fieldRef, false);
		
		if (ctClass.getMethod(getterName) == null) {
			CtMethod getter = method().create(ctClass, pub, field.getType(), getterName, noParams, noExceptions);
			getter.setBody(newBlock(newReturn(fieldAccess)));
		}
		
		if (ctClass.getMethod(setterName, field.getType()) == null) {
			CtParameter param = newParam(field.getType(), field.getSimpleName());
			
			CtMethod setter = method().create(ctClass, pub, field.getType(), setterName, 
					(List)Arrays.asList(param), noExceptions);
			
			setter.setBody(newBlock(newFieldAssignment(field, param.getSimpleName())));
		}
		
	}

	@SuppressWarnings("unchecked")
	private CtStatement newFieldAssignment(CtField<?> field, String simpleName) {
		CtLocalVariableReference paramRef = code().createLocalVariableReference(field.getType(), simpleName);
		return newAssignment(
			(CtFieldAccess)code().createVariableAccess(field().createReference(field), false), 
			code().createVariableAccess(paramRef, false)
		);
	}

	@SuppressWarnings("unchecked")
	private CtStatement newAssignment(CtExpression dest, CtExpression source) {
		CtAssignment assignment = core().createAssignment();
		assignment.setAssigned(dest);
		assignment.setAssignment(source);
		return assignment;
	}

	private String capitalize(String simpleName) {
		return simpleName.length() == 0 ? "" : Character.toUpperCase(simpleName.charAt(0)) + simpleName.substring(1);
	}

	public Set<InstantiationResult> getResults() {
		return results ;
	}
	
}