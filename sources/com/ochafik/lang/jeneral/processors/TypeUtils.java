package com.ochafik.lang.jeneral.processors;

import java.util.HashMap;
import java.util.Map;

public class TypeUtils {
	static Map<Class<?>, Class<?>> wrappersToPrimitives = new HashMap<Class<?>, Class<?>>();
	static {
		wrappersToPrimitives.put(Integer.class, Integer.TYPE);
		wrappersToPrimitives.put(Long.class, Long.TYPE);
		wrappersToPrimitives.put(Short.class, Short.TYPE);
		wrappersToPrimitives.put(Byte.class, Byte.TYPE);
		wrappersToPrimitives.put(Double.class, Double.TYPE);
		wrappersToPrimitives.put(Float.class, Float.TYPE);
		wrappersToPrimitives.put(Character.class, Character.TYPE);
	}
	public static Class<?> unwrapPrimitiveClass(Class<?> value) {
		Class<?> c = wrappersToPrimitives.get(value);
		return c == null ? value : c;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrapPrimitive(Class<T> c) {
		if (!c.isPrimitive())
			return c;
		return (Class<T>)primitiveToWrapperClass.get(c);
	}

	static Map<String, Class<?>> classLiteralStringToClass = new HashMap<String, Class<?>>();
	public static Class<?> parseClassLiteral(String s) {
		if (s == null)
			return null;
		//s = s.replace("\\s+", "");
		
		Class<?> c = classLiteralStringToClass.get(s);
		if (c != null)
			return c;
		
		if (!s.endsWith(".class"))
			return null;
		
		try {
			return Class.forName(s.substring(0, s.length() - ".class".length()));
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	private static Map<Class<?>, Class<?>> primitiveToWrapperClass= new HashMap<Class<?>, Class<?>>();
	//static Map<String, Class<?>> primitiveToWrapperClass= new HashMap<String, Class<?>>();
	static Map<String, String> primitiveToClassAccess = new HashMap<String, String>();
	static {
		classLiteralStringToClass.put("java.lang.Integer.TYPE", java.lang.Integer.class);
		classLiteralStringToClass.put("java.lang.Long.TYPE", java.lang.Long.class);
		classLiteralStringToClass.put("java.lang.Short.TYPE", java.lang.Short.class);
		classLiteralStringToClass.put("java.lang.Byte.TYPE", java.lang.Byte.class);
		classLiteralStringToClass.put("java.lang.Character.TYPE", java.lang.Character.class);
		classLiteralStringToClass.put("java.lang.Float.TYPE", java.lang.Float.class);
		classLiteralStringToClass.put("java.lang.Double.TYPE", java.lang.Double.class);
		
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

}
