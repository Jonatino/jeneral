package com.ochafik.lang.jeneral.examples;

import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Template;

@Template(
	additionalParameters = { 
		@Param(name = "initial_capacity", type = Integer.class), 
		@Param(name = "default_name", type = String.class) 
	}
)
public abstract class GenerationExample<T, U> implements _GenerationExample<T, U> {
	public GenerationExample(String arg) {}
}