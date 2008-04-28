package com.ochafik.lang.jeneral.annotations;

public @interface Instantiate {
	Class<?> template();
	Class<?>[] params() default {};
}
