package com.ochafik.lang.jeneral.annotations;

public @interface Instantiate {
	Class<?> template();
	String overrideClassName() default "";
	Param[] params() default {};
}
