package com.ochafik.lang.jeneral.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
//@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Template {
	Param[] params() default {};
	
	/// Reifiable templates have a "Factory" inner class which various newInstance methods allow for pure-generics instantiation of the template.
	boolean reifiable() default true;
	
	//boolean paramFactoryThrowsExceptions() default false;
}
