package com.ochafik.lang.jeneral.examples;

import java.io.IOException;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.runtime.Fields;
import com.ochafik.lang.jeneral.runtime.Methods;

@Template
public abstract class EvalExample<E> implements _EvalExample<E> {

	@ParamConstructor(returnNeutralValue = true)
	abstract E neutral_E();


	public static String capitalize(String s) {
		char[] chars = s.toCharArray();
		if (chars.length == 0)
			return "";
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars); 
	}

	public void writeExternal( ObjectOutput out ) throws IOException {
		// VERSION
		try {
			//Methods.invokeStatic(E(), "write" + E().getName(), out, _data.get(i));
			//Methods.invokeStatic(getClass(), "write" + capitalize(E().getName()) + "_", out, neutral_E());
			//E().getMethod("write" + capitalize(E().getName())).invoke(out, neutral_E());
			ObjectOutput.class.getMethod("write" + capitalize(E().getName()), E()).invoke(out, neutral_E());
			//E max = E().cast(Fields.getStatic(E(), "MAX_VALUE"));
			//Methods.invokeStatic(E(), "MAX_VALUE");
			E max = E().cast(E().getField("MAX_VALUE").get(null));

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
