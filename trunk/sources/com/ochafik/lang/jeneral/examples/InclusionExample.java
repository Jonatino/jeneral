package com.ochafik.lang.jeneral.examples;
 
import com.ochafik.lang.jeneral.annotations.Include;
import com.ochafik.lang.jeneral.annotations.Template;

@Template
@Include(type = UsefulMethods.class)
public abstract class InclusionExample implements _InclusionExample {
	public InclusionExample() {
		aMethod();
	}
}
