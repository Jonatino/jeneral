package com.ochafik.lang.jeneral.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface Value {
	Class<?> value();
	int intValue();
	int longValue();
	int shortValue();
	int byteValue();
	int doubleValue();
	int floatValue();
	int charValue();
}
