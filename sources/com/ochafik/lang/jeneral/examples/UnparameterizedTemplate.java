package com.ochafik.lang.jeneral.examples;

import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;

@Template
public abstract class UnparameterizedTemplate implements _UnparameterizedTemplate 
{
	@Property
	int count;
	
	@Property
	String text;
	
	@Property(addToConstructors = true)
	protected int id;
}