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
package com.ochafik.lang.jeneral;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;

public class Templates {
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	//@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface TemplatesPrimitives {}

	public final class PrimitiveTypes {}
	
	@SuppressWarnings("unchecked")
	@TemplatesPrimitives
	public static final <T> Class<T> getClass(T value) { 
		return (Class<T>)(value == null ? null : value.getClass()); 
	}
	
	@TemplatesPrimitives
	public static final <T> boolean isPrimitive(T value) { 
		return false; 
	}
	
	@TemplatesPrimitives
	public static final <T> T newObject(Class<T> classe) throws Exception {
		return (T)classe.newInstance();
	}
	
	@SuppressWarnings("unchecked")
	@TemplatesPrimitives
	public static final <T> T[] newArray(Class<T> componentType, int size) {
		return (T[]) Array.newInstance(componentType, size);
	}
		
}
