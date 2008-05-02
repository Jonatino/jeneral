package com.ochafik.lang.jeneral.annotations;

public @interface Include {
	String script() default "";
	String engine() default "";
	Class<?> type() default Object.class;
	//String typeName() default "";
	boolean keepConstructors() default true;
	
	public enum Insertion {
		BEFORE, AFTER, BEGINNING, END
	}
	
	Insertion insertion() default Insertion.BEGINNING;
}
