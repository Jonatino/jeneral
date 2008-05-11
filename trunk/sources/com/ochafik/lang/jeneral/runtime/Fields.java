package com.ochafik.lang.jeneral.runtime;

import com.ochafik.lang.jeneral.annotations.TemplatesPrimitives;

@TemplatesPrimitives
public class Fields {

	@SuppressWarnings("unchecked")
	public static Object getStatic(Class<?> c, String name) throws ReflectionException {
		try {
			return c.getField(name).get(null);
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Object get(Object o, String name) throws ReflectionException {
		try {
			return o.getClass().getField(name).get(o);
		} catch (Exception e) {
			throw new ReflectionException(e);
		}
	}

}
