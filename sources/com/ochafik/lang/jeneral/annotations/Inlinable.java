package com.ochafik.lang.jeneral.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.ochafik.lang.jeneral.processors.Inliner;

@Retention(RetentionPolicy.RUNTIME)
public @interface Inlinable {
	Class<? extends Inliner> inliner();
	String tag() default "";
}
