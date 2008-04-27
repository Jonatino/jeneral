/*
   Copyright 2008 Olivier Chafik

   Licensed under the Apache License, Version 2.0 (the License);
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an AS IS BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This file comes from the Jeneral project (Java Reifiable Generics & Class Templates)

       http://jeneral.googlecode.com/.
*/
package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import javax.swing.JLabel;

import com.ochafik.lang.jeneral.Property;
import com.ochafik.lang.jeneral.annotations.DeclareConstructor;
import com.ochafik.lang.jeneral.annotations.Template;

// Declare that ElementsBuild is a template.
// It has to be abstract and to implement ElementsBuilder_Template, which is autogenerated on the fly in Eclipse, NetBeans or with the apt tool in Sun's JDK
@Template
public abstract class ElementsBuilder<U,V> implements ElementsBuilder_Template<U,V> {
	 
	// Generate getters and setters for the 'arg' property, and append it to all ElementBuilder's factory methods
	@Property(inConstructors = true)
	String arg;
	
	// Declare that T must have a constructor T(String) that throws no checked exception, and that it should be accessible as the method new_T :
	@DeclareConstructor
	public abstract U new_T(String arg) throws IOException;
	
	public U buildElement() throws IOException {
		return new_T(getArg());
	}
	
	public static void main(String[] args) {
		// There is one newInstance method for each public constructor of the template class
		// Each newInstance method has the following arguments :
		//   - classes of generic template parameter 
		//   - constant template parameters, if any (here Test has no constant template parameter)
		//   - arguments of the constructor that corresponds to this newInstance method (here String arg, which was added implicitely to the unique Test() constructor by the @Property(fromConstructor = true) annotation)
		try {
			ElementsBuilder<JLabel, Object> labelBuilder = ElementsBuilder_Template.Factory.newInstance(JLabel.class, Object.class, "Default Label Text");
			System.out.println(labelBuilder.buildElement());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			ElementsBuilder<Integer, Object> intBuilder = ElementsBuilder_Template.Factory.newInstance(Integer.class, Object.class, "Default Label Text");
			System.out.println(intBuilder.buildElement());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
