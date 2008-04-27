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

import static com.ochafik.util.string.StringUtils.implode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ochafik.util.listenable.AdaptedCollection;
import com.ochafik.util.listenable.Adapter;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.ExecutableDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.util.SourcePosition;

public abstract class AbstractProcessor implements AnnotationProcessor {
	protected AnnotationProcessorEnvironment environment;

	PrintWriter pout;
	protected PrintWriter getLogger() {
		if (pout == null) {
			try {
				pout = new PrintWriter(new FileWriter(new File("/Users/ochafik/Prog/Java/templates_logs.txt")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pout;
	}
	protected void printNotice(SourcePosition position, String string) {
		environment.getMessager().printNotice(position, string);
	}
	protected void printError(Declaration decl, String string) {
		printError(decl.getPosition(), string);
	}
	protected void printError(SourcePosition position, String string) {
		environment.getMessager().printError(position, string);
	}
	protected void logError(Throwable t) {
		t.printStackTrace(getLogger());
		getLogger().flush();
	}
	protected void log(Object o) {
		getLogger().println(o);
		getLogger().flush();
	}
	public AbstractProcessor(AnnotationProcessorEnvironment env){
		this.environment = env;
	}

	protected AnnotationTypeDeclaration getAnnotationType(Class<? extends Annotation> cl) {
		return (AnnotationTypeDeclaration)environment.getTypeDeclaration(cl.getName());
	}
	
	protected AnnotationMirror findAnnotation(Declaration decl, AnnotationTypeDeclaration anno) {
		for (AnnotationMirror ann : decl.getAnnotationMirrors()) {
			if (ann.getAnnotationType().getDeclaration().equals(anno))
				return ann;
		}
		return null;
	}
	
	protected AnnotationValue getValue(AnnotationMirror ann, String fieldName) {
		for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> e : ann.getElementValues().entrySet()) {
			if (e.getKey().getSimpleName().equals(fieldName)) {
				return e.getValue();
			}
		}
		return null;
	}
	
	public static List<String> getArgumentNames(ExecutableDeclaration constructor, List<String> existingNames) {
		if (existingNames == null)
			existingNames = new ArrayList<String>();
		
		for (ParameterDeclaration d : constructor.getParameters())
			existingNames.add(d.getSimpleName());
		
		return existingNames;
	}
	public static String typedClass(String genericParamName) {
		return Class.class.getName() + "<" + genericParamName + ">";
	}
	protected static Collection<String> getFormalTypeNames(TypeDeclaration type) {
		return new AdaptedCollection<TypeParameterDeclaration, String>(type.getFormalTypeParameters(),
		new Adapter<TypeParameterDeclaration, String>() { public String adapt(TypeParameterDeclaration value) {
			return value.getSimpleName();
		}}, null);
	}
	
	public static <U, V> String adaptedImplosion(Collection<U> col, Adapter<U, V> adapter) {
		return implode(new AdaptedCollection<U, V>(col, adapter));
	}
	public static <E> String wrappedImplosion(Collection<E> col, String prefixIfResultNotEmpty, String suffixIfResultNotEmpty) {
		String s = implode(col);
		if (s.length() > 0)
			return prefixIfResultNotEmpty + s + suffixIfResultNotEmpty;
		return "";
	}
	public static boolean implementsInterface(ClassDeclaration decl, String interfaceQualifiedName) {
		for (InterfaceType interf : decl.getSuperinterfaces()) {
			if (interf.getDeclaration().getQualifiedName().equals(interfaceQualifiedName))
				return true;
		}
		return false;
	}
	public static String capitalize(String name) {
		char[] chars = name.toCharArray();
		if (chars.length > 0)
			chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}

	public static String decapitalize(String name) {
		char[] chars = name.toCharArray();
		if (chars.length > 0)
			chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}

	public static String chooseUniqueName(String name, Set<String> existingArgumentNames, boolean addChosenNameToExistingNames) {
		int i = 2;
		while (existingArgumentNames.contains(name)) 
			name = name + (i++);
		
		if (addChosenNameToExistingNames)
			existingArgumentNames.add(name);
		return name;
	}

	protected class LinesFormatter {
		PrintWriter out;
		Stack<String> indentStack = new Stack<String>();
		String currentIndent;
		
		public LinesFormatter(PrintWriter out, String initialIndent) {
			this.out = out;
			indentStack.push(initialIndent);
			currentIndent = initialIndent;
		}
		
		public void printfn(String fmt, Object... args) {
			println(String.format(fmt, args));
		}
		public void println() {
			out.println();
		}
		
		public <T> void println(T[] arr) {
			for (T t : arr)
				println(t);
		}
		public void println(Object o) {
			if (o == null)
				return;
			
			String ss = o.toString();
			for (String s : ss.split("\n")) {
				s = s.trim();
				if (s.startsWith("}")) {
					indentStack.pop();
					currentIndent = indentStack.empty() ? "" : indentStack.lastElement();
				}
				
				out.print(currentIndent);
				out.println(s);
				
				if (s.endsWith("{"))
					indentStack.push(currentIndent = currentIndent + "\t");
			}
		}
		
		public void close() {
			out.close();
		}
		public void format(String[] strings, Object... args) {
			for (String ff : strings)
				format(ff, args);
		}
		public void format(String f, Object... args) {
			if (f == null)
				return; 
			
			try {
				println(MessageFormat.format(f, args));
			} catch (Throwable t) {
				throw new RuntimeException("During formatting of "+f, t);
			}
		}
	}
	
	/*protected static List<String> collectGenericParamsArgumentsDeclaration(Collection<String> typeNames, boolean finalDecls, boolean argNameOnly, List<String> typeAndArgNameOut) {
		if (typeAndArgNameOut == null)
			typeAndArgNameOut = new ArrayList<String>();
		
		for (String type : typeNames) {
			String argName = chooseVariableName(type);
			if (argNameOnly)
				typeAndArgNameOut.add(argName);
			else
				typeAndArgNameOut.add((finalDecls ? "final " : "") + Class.class.getName() + "<" + type + "> " + argName);
		}
		return typeAndArgNameOut;
	}*/
	
	protected static String chooseVariableName(String typeName) {
		String[] words = typeName.toString().split(".");
		return chooseVariableNameFromSimpleName(words.length > 0 ? words[words.length - 1] : typeName);
	}
	protected static String chooseVariableNameFromSimpleName(String simpleName) {
		String name = decapitalize(simpleName);
		if (name.equals(simpleName))
			name = "_" + name;
		return name;
	}	
	protected static String chooseVariableName(Declaration decl) {
		return chooseVariableNameFromSimpleName(decl.getSimpleName());
	}
	
	protected static List<String> collectArgumentsDeclaration(Collection<ParameterDeclaration> argTypes, boolean finalDecls, boolean argNameOnly, List<String> typeAndArgNameOut) {
		if (typeAndArgNameOut == null)
			typeAndArgNameOut = new ArrayList<String>();
		
		for (ParameterDeclaration argType : argTypes) {
			if (argNameOnly) {
				String[] words = argType.toString().split("[^\\w]");
				String argName;
				if (words.length > 0) {
					argName = words[words.length - 1];
				} else {
					argName = chooseVariableName(argType);
				}
				typeAndArgNameOut.add(argName);
			} else
				typeAndArgNameOut.add((finalDecls && !argType.getModifiers().contains(Modifier.FINAL) ? "final " : "") + argType);
		}
		return typeAndArgNameOut;
	}
}