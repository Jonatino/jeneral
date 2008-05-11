package com.ochafik.lang.jeneral.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ochafik.lang.jeneral.annotations.TemplatesPrimitives;


@TemplatesPrimitives
public class Methods {

	static Method getMethod(Class<?> c, Object... args) throws ReflectionException {
		int nArgs = args.length;
		for (Method m : c.getMethods()) {
			Class<?>[] params = m.getParameterTypes();
			if (params.length != nArgs)
				continue;
			
			boolean matches = true;
			for (int i = nArgs; i-- != 0;) {
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
			if (matches)
				return m;
		}
		throw new ReflectionException("Cannot find matching method in " + c.getName() + " for arguments (" + com.ochafik.util.string.StringUtils.implode(args, ", ") + ")");
	}
	@SuppressWarnings("unchecked")
	public static Object invokeStatic(Class<?> c, String name, Class[] argTypes, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			return c.getMethod(name, argTypes).invoke(null, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	public static Object invokeStatic(Class<?> c, String name, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			return getMethod(c, params).invoke(null, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (ReflectionException e) {
			throw e;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Object invoke(Object o, String name, Class[] argTypes, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			return o.getClass().getMethod(name, argTypes).invoke(o, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	public static Object invoke(Object o, String name, Object... params) throws ReflectionException, InvocationTargetException {
		try {
			return getMethod(o.getClass(), params).invoke(o, params);
		} catch (InvocationTargetException ex) {
			throw ex;
		} catch (ReflectionException e) {
			throw e;
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	
}
