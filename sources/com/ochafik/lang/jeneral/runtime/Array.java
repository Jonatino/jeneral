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
package com.ochafik.lang.jeneral.runtime;

import spoon.reflect.code.CtInvocation;

import com.ochafik.lang.jeneral.annotations.Inlinable;
import com.ochafik.lang.jeneral.annotations.TemplatesPrimitives;
import com.ochafik.lang.jeneral.processors.Inliner;

@TemplatesPrimitives
public interface Array<T> {
	
	@Inlinable(inliner = GetterInliner.class)
	public T get(int i);
	
	@Inlinable(inliner = SetterInliner.class)
	public T set(int i, T value);
	
	@Inlinable(inliner = LengthInliner.class)
	public int length();
	
	@Inlinable(inliner = GetArrayInliner.class)
	public Object getArray();
	
	
	public class GetterInliner extends Inliner { public void process(CtInvocation<?> element) {
		element.replace(getFactory().Code().createCodeSnippetExpression(element.getTarget() + "[" + element.getArguments().get(0) + "]"));
	}}
	
	public class SetterInliner extends Inliner { public void process(CtInvocation<?> element) {
		element.replace(getFactory().Code().createCodeSnippetExpression(element.getTarget() + "[" + element.getArguments().get(0) + "] = " + element.getArguments().get(1)));
	}}
	
	public class LengthInliner extends Inliner { public void process(CtInvocation<?> element) {
		element.replace(getFactory().Code().createCodeSnippetExpression(element.getTarget() + ".length"));
	}}
	
	public class GetArrayInliner extends Inliner { public void process(CtInvocation<?> element) {
		element.replace(element.getTarget());
	}}
	
}
