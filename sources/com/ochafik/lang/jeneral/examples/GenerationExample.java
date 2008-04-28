package com.ochafik.lang.jeneral.examples;

import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Template;

@Template(params = { @Param(name = "T"), @Param(name = "U") }, reifiable = true)
public abstract class GenerationExample<T, U> implements GenerationExample_<T, U> {
	public GenerationExample(String arg) {}
}