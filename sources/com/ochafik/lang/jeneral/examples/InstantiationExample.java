package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import javax.swing.JLabel;

import com.ochafik.lang.jeneral.annotations.SummonTemplate;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.SummonTemplates;
import com.ochafik.lang.jeneral.annotations.Value;

@SummonTemplates({
	@SummonTemplate(template = ElementsBuilder.class, params = { @Param(@Value(Integer.class)) }),
	@SummonTemplate(template = EvalExample.class, params = { @Param(@Value(Integer.class)) }),
	@SummonTemplate(template = EvalExample.class, params = { @Param(@Value(Float.class)) }),
	@SummonTemplate(template = EvalExample.class, params = { @Param(@Value(Double.class)) }),
	@SummonTemplate(template = EvalExample.class, params = { @Param(@Value(Long.class)) }),
	@SummonTemplate(template = TextElementsBuilder.class, params = { @Param(@Value(JLabel.class)) }), 
	@SummonTemplate(template = PArrayList.class, params = { @Param(@Value(Integer.class)) })
})
public class InstantiationExample {
	public static void main(String[] args) {
		ElementsBuilder__int intBuilder = new ElementsBuilder__int("test");
		EvalExample__int intEval;
		EvalExample__float floatEval;
		EvalExample__double doubleEval;
		EvalExample__long longEval;
		PArrayList__int intList;
		//TextElementsBuilder
		try {
			//intBuilder.buildElement(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
