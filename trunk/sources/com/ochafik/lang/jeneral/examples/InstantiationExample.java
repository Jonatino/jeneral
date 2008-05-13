package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import com.ochafik.lang.jeneral.annotations.Instantiate;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Value;

@Instantiate(template = ElementsBuilder.class, params = {
	@Param(value = @Value(Integer.class))
})
public class InstantiationExample  {
	@Instantiate(template = EvalExample.class, params = { @Param(@Value(Integer.class)) })
	public static void main(String[] args) {
		ElementsBuilder__int intBuilder = new ElementsBuilder__int("test");
		EvalExample__int intEval;
		try {
			intBuilder.buildElement(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
