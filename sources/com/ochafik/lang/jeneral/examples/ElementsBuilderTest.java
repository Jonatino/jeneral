package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import javax.swing.JLabel;

public class ElementsBuilderTest {
	public static void main(String[] args) {
		// There is one newInstance method for each public constructor of the template class
		// Each newInstance method has the following arguments :
		//   - classes of generic template parameter 
		//   - constant template parameters, if any
		//   - arguments of the constructor that corresponds to this newInstance method (here String arg, which was added implicitely to the unique Test() constructor by the @Property(fromConstructor = true) annotation)
		try {
			TextElementsBuilder<JLabel> labelBuilder = TextElementsBuilder.template.newInstance(JLabel.class, "Default Label Text");
			System.out.println(labelBuilder.buildElement());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			ElementsBuilder<Integer> intBuilder = ElementsBuilder.template.newInstance(Integer.TYPE, "Default Label Text");
			System.out.println(intBuilder.buildElement(10));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			TextElementsBuilder<Integer> intBuilder = TextElementsBuilder.template.newInstance(Integer.class, "Default Label Text");
			System.out.println(intBuilder.buildElement());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}
