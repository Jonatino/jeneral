package com.ochafik.lang.jeneral.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.ModifierKind;

import com.ochafik.lang.jeneral.annotations.Inlinable;
import com.ochafik.lang.jeneral.annotations.TemplatesPrimitives;
import com.ochafik.lang.jeneral.processors.Inliner;
import com.ochafik.lang.jeneral.processors.SpoonHelper;
import com.ochafik.lang.jeneral.processors.TypeUtils;
import com.ochafik.util.string.StringUtils;


public class Methods {

	public static Object[] getArgsWithVarArgs(Method m, Object[] args) {
		Class<?>[] params = m.getParameterTypes();
		int nParams = params.length;
		boolean hasVarArgs = nParams > 0 && params[nParams - 1].isArray();
		if (!hasVarArgs)
			return args;
		
		Object[] ret = new Object[params.length];
		System.arraycopy(args, 0, ret, 0, nParams - 1);
		int nVarArgs = args.length - (nParams - 1);
		Object varArgs = Array.newInstance(params[nParams - 1].getComponentType(), nVarArgs);
		for (int i = 0; i < nVarArgs; i++) {
			Array.set(varArgs, i, args[nParams - 1 + i]);
		}
		ret[nParams - 1] = varArgs;
		
		return ret;
	}
	public static Method getMethodForArgs(Class<?> c, String name, Object... args) throws ReflectionException {
		int nArgs = args.length;
		for (Method m : c.getMethods()) {
			if (!m.getName().equals(name))
				continue;
			
			Class<?>[] params = m.getParameterTypes();
			int nParams = params.length;
			boolean hasVarArgs = nParams > 0 && params[nParams - 1].isArray();
			if ((!hasVarArgs && nArgs != nParams) || nArgs < nParams - 1)
				continue;
			
			boolean matches = true;
			for (int i = 0, len = (hasVarArgs ? nParams - 1 : nParams); i < len; i++) {
				Class<?> param = params[i];
				Object arg = args[i];
				if (arg == null) {
					if (param.isPrimitive()) {
						matches = false;
						break;
					} 
				} else {
					if (!param.isAssignableFrom(arg.getClass())) {
						matches = false;
						break;
					}
				}
			}
			if (hasVarArgs) {
				Class<?> varType = params[nParams - 1].getComponentType();
				for (int i = nParams - 1; i < nArgs; i++) {
					Object arg = args[i];
					if (arg == null) {
						if (varType.isPrimitive()) {
							matches = false;
							break;
						} 
					} else {
						if (!varType.isAssignableFrom(arg.getClass())) {
							matches = false;
							break;
						}
					}
				}
			}
			if (matches)
				return m;
		}
		throw new ReflectionException("Cannot find matching method in " + c.getName() + " for arguments " + name + "(" + com.ochafik.util.string.StringUtils.implode(Arrays.asList(args), ", ") + ")");
	}
	private static Method getMethodForArgTypes(Class<?> c, String name, Class<?>... args) throws ReflectionException {
		int nArgs = args.length;
		for (Method m : c.getMethods()) {
			if (!m.getName().equals(name))
				continue;
			
			Class<?>[] params = m.getParameterTypes();
			int nParams = params.length;
			boolean hasVarArgs = nParams > 0 && params[nParams - 1].isArray();
			if ((!hasVarArgs && nArgs != nParams) || nArgs < nParams - 1)
				continue;
			
			boolean matches = true;
			for (int i = 0, len = (hasVarArgs ? nParams - 1 : nParams); i < len; i++) {
				Class<?> param = params[i];
				Class<?> arg = args[i];
				if (!param.isAssignableFrom(arg)) {
					matches = false;
					break;
				}
			}
			if (hasVarArgs) {
				Class<?> varType = params[nParams - 1].getComponentType();
				for (int i = nParams - 1; i < nArgs; i++) {
					Class<?> arg = args[i];
					if (!varType.isAssignableFrom(arg)) {
						matches = false;
						break;
					}
				}
			}
			if (matches)
				return m;
		}
		throw new ReflectionException("Cannot find matching method in " + c.getName() + " for arguments (" + com.ochafik.util.string.StringUtils.implode(Arrays.asList(args), ", ") + ")");
	}
	
	public static class StaticInvokeInliner extends Inliner {
		public void process(CtInvocation<?> invocation) {
			List<CtExpression<?>> args = invocation.getArguments();
			String[] tags = getTag().split(",");
			CtExpression<?> targetExp = args.get(Integer.parseInt(tags[0]));
			CtExpression<?> methodName = args.get(Integer.parseInt(tags[1]));
			List<CtExpression<?>> argsCall = args.subList(Integer.parseInt(tags[2]), args.size());
			
			if (methodName instanceof CtLiteral) {
				String methodNameStr = ((CtLiteral<?>)methodName).getValue().toString();
				String target;
				if ("this".equals(targetExp.toString().trim()) || "getClass()".equals(targetExp.toString().trim()))
					target = "";
				else
					target = targetExp.toString() + ".";
				
				String s = target + methodNameStr + "(" + StringUtils.implode(argsCall) + ")";
				boolean isExpr = invocation.getParent() instanceof CtExpression;
				invocation.replace(isExpr ? getFactory().Code().createCodeSnippetExpression(s).compile() : getFactory().Code().createCodeSnippetStatement(s));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Inlinable(inliner = StaticInvokeInliner.class, tag = "0,1,3")
	public static Object invokeStatic(Class<?> c, String name, Class[] argTypes, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			Method m = getMethodForArgTypes(TypeUtils.wrapPrimitiveClass(c), name, argTypes);
			return m.invoke(null, getArgsWithVarArgs(m, params));
			//return c.getMethod(name, argTypes).invoke(null, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	@TemplatesPrimitives
	@Inlinable(inliner = StaticInvokeInliner.class, tag = "0,1,2")
	public static Object invokeStatic(Class<?> c, String name, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			Method m = getMethodForArgs(TypeUtils.wrapPrimitiveClass(c), name, params);
			return m.invoke(null, getArgsWithVarArgs(m, params));
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (ReflectionException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ReflectionException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@TemplatesPrimitives
	@Inlinable(inliner = StaticInvokeInliner.class, tag = "0,1,3")
	public static Object invoke(Object o, String name, Class[] argTypes, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			Method m = getMethodForArgTypes(o.getClass(), name, argTypes);
			return m.invoke(o, getArgsWithVarArgs(m, params));
			//return o.getClass().getMethod(name, argTypes).invoke(o, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	@TemplatesPrimitives
	@Inlinable(inliner = StaticInvokeInliner.class, tag = "0,1,2")
	public static Object invoke(Object o, String name, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			Method m = getMethodForArgs(o.getClass(), name, params);
			return m.invoke(o, getArgsWithVarArgs(m, params));
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (ReflectionException e) {
			throw e;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	
}
