package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import com.ochafik.lang.jeneral.annotations.Instantiate;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Value;

@Instantiate(template = ElementsBuilder.class, params = {
	@Param(value = @Value(Integer.class))
})
public class InstantiationExample  {
	void d() {
		ElementsBuilder__Integer intBuilder = new ElementsBuilder__Integer("test");
		try {
			intBuilder.buildElement(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//JDTCompiler c = null;
		//c.set
		//c.getUnits(null, null);
	}

}
