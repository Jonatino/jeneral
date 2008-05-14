package com.ochafik.lang.jeneral.annotations;

public @interface SummonTemplate {
	Class<?> template();
	String overrideClassName() default "";
	Param[] params() default {};
}
