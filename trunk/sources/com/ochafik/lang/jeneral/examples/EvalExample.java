package com.ochafik.lang.jeneral.examples;

import java.io.IOException;
import java.io.ObjectOutput;

import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.runtime.Methods;

@Template 
public abstract class EvalExample<E> implements _EvalExample<E> {
 
	@ParamConstructor(returnNeutralValue = true)
	abstract E neutral_E();

	public void writeExternal( ObjectOutput out ) throws IOException {
		// VERSION
		try {
			//Methods.invokeStatic(E(), "write" + E().getName(), out, _data.get(i));
			int e;
			Methods.invoke(out, "write" + ExampleUtils.capitalize(E().getName()), neutral_E());
			//E().getMethod("write" + capitalize(E().getName())).invoke(out, neutral_E());
			ObjectOutput.class.getMethod("write" + ExampleUtils.capitalize(E().getName()), E()).invoke(out, neutral_E());
			//E max = E().cast(Fields.getStatic(E(), "MAX_VALUE"));
			//Methods.invokeStatic(E(), "MAX_VALUE");
			E max = E().cast(E().getField("MAX_VALUE").get(null));

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
