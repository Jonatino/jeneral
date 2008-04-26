package com.ochafik.lang.jeneral.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE) // TODO implement bytecode template instantiation later
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Instantiate {
	boolean useParametersLongName() default false;
	Param[] params();
	
	@AnnotatedValue(Template.class)
	Class<?> type();
}
