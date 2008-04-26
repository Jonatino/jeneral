package com.ochafik.lang.jeneral;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.Arrays;

public class Templates {
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface TemplatesPrimitive {}
	
	@SuppressWarnings("unchecked")
	@TemplatesPrimitive
	public static final <T> Class<T> getClass(T value) { 
		return (Class<T>)(value == null ? null : value.getClass()); 
	}
	
	@TemplatesPrimitive
	public static final <T> boolean isPrimitive(T value) { 
		return false; 
	}
	
	@TemplatesPrimitive
	public static final <T> T newObject(Class<T> classe) throws Exception {
		return (T)classe.newInstance();
	}
	
	@SuppressWarnings("unchecked")
	@TemplatesPrimitive
	public static final <T> T[] newArray(Class<T> componentType, int size) {
		return (T[]) Array.newInstance(componentType, size);
	}
		
}
