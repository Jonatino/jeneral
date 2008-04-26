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

import static com.ochafik.lang.SyntaxUtils.*;
import static com.ochafik.util.string.StringUtils.implode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ochafik.lang.jeneral.annotations.DeclareConstructor;
import com.ochafik.lang.jeneral.annotations.Instantiate;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.annotations.TemplatesHelper;
import com.ochafik.util.listenable.AdaptedCollection;
import com.ochafik.util.listenable.Adapter;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorListener;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;

/*
cd /Users/ochafik/Prog/Java && rm templates_logs.txt >/dev/null ; apt -factory com.ochafik.lang.templates.TemplateProcessorFactory -d classes/ -s sources/.apt_generated/ -cp sources:classes sources/com/ochafik/lang/templates/*.java && open templates_logs.txt
 */
public class TemplateProcessor extends AbstractProcessor {
	private static final String 
		GENERATED_INTERFACE_SUFFIX = "_Template",
		GENERATED_REIFICATOR_NAME = "Factory",
		NEW_ARRAY_METHOD_FORMAT = "%s";
		
	private Class<?>[] deps = array(
		ReificationUtils.class,
		TemplatesHelper.class
	);

	public TemplateProcessor(AnnotationProcessorEnvironment env){
		super(env);
		//log("Template Processor created");
	}

	static String computeInstanceName(String baseName, TypeDeclaration... types) {
		StringBuilder b = new StringBuilder(baseName);
		for (TypeDeclaration type : types) {
			b.append("__");
			b.append(type.getSimpleName());
		}
		return b.toString();
	}
	public void process() {
		AnnotationTypeDeclaration templateAnno = getAnnotationType(Template.class);
		for (Declaration dec : environment.getDeclarationsAnnotatedWith(templateAnno)) {
			if (!(dec instanceof ClassDeclaration)) {
				printError(dec, "Only classes may be annotated with " + Template.class.getName());
				continue;
			}
			ClassDeclaration decl = (ClassDeclaration)dec;
			try {
				processTemplateClass(decl);
			} catch (Throwable t) {
				logError(t);
			}
		}
		
		AnnotationTypeDeclaration instantiationAnno = getAnnotationType(Instantiate.class);
		for (Declaration dec : environment.getDeclarationsAnnotatedWith(instantiationAnno)) {
			try {
				processInstantiation(dec);
			} catch (Throwable t) {
				logError(t);
			}
		}
	}
	
	public static <E> String wrapImplosion(Collection<E> col, String prefixIfResultNotEmpty, String suffixIfResultNotEmpty) {
		String s = implode(col);
		if (s.length() > 0)
			return prefixIfResultNotEmpty + s + suffixIfResultNotEmpty;
		return "";
	}
	
	public static <U, V> String adaptedImplosion(Collection<U> col, Adapter<U, V> adapter) {
		return implode(new AdaptedCollection<U, V>(col, adapter));
	}

	private void processTemplateClass(ClassDeclaration decl) throws IOException {
		AnnotationMirror templateAnnotationMirror = null;
		for (AnnotationMirror annoMirror : decl.getAnnotationMirrors()) {
			if (annoMirror.getAnnotationType().toString().equals(Template.class.getName())) {
				templateAnnotationMirror = annoMirror;
				break;
			}
		}
		if (templateAnnotationMirror == null) 
			return;
		
		if (decl.getDeclaringType() != null) {
			printError(templateAnnotationMirror.getPosition(), "Cannot define nested classes as templates.");
			return;
		}
		
		if (!decl.getModifiers().contains(Modifier.ABSTRACT)) {
			printError(decl, "Template "+decl.getQualifiedName()+" must be declared as abstract.");
		}
		
		Filer filer = environment.getFiler();
		String interfaceQualifiedName = decl.getQualifiedName() + GENERATED_INTERFACE_SUFFIX;
		String interfaceName = decl.getSimpleName() + GENERATED_INTERFACE_SUFFIX;
		
		LinesFormatter f = new LinesFormatter(filer.createSourceFile(interfaceQualifiedName), "");
		//String interface
		
		String packageName = decl.getPackage().getQualifiedName();
		String genericParamsDefinition = wrapImplosion(decl.getFormalTypeParameters(), "<", ">");
		
		List<String> genericParamNames = new ArrayList<String>(getFormalTypeNames(decl));
		String genericParamsUsage = wrapImplosion(genericParamNames, "<", ">");
		
		f.format(array(
				"//",
				"// This file was autogenerated by {5} from {0}",
				"//",
				packageName.length() == 0 ? null : "package {1};",
				"",
				//"@{2}",
				"interface {3}{4} '{'"
			), 
			decl.getQualifiedName(), 
			packageName,
			TemplatesHelper.class.getName(),
			interfaceName, 
			genericParamsDefinition, 
			getClass().getName()
		);
		
		Map<String, List<MethodDeclaration>> paramConstructorContractsByParam = new HashMap<String, List<MethodDeclaration>>();
		for (MethodDeclaration constructorContract : decl.getMethods()) {
			DeclareConstructor declAnn = constructorContract.getAnnotation(DeclareConstructor.class);
			if (declAnn == null)
				continue;
			
			if (!constructorContract.getModifiers().contains(Modifier.ABSTRACT)) {
				printError(constructorContract, "Template parameter constructor declarations must be abstract methods !");
				continue;
			}
			TypeMirror m = constructorContract.getReturnType();
			String paramType = m.toString();
			if (!genericParamNames.contains(paramType)) {
				printError(constructorContract, "Template parameter constructor declarations must have a return type that matches one of the template parameters !");
				continue;
			}
			if (!constructorContract.getFormalTypeParameters().isEmpty()) {
				printError(constructorContract, "Template parameter constructor declarations cannot be generics");
				continue;
			}
			List<MethodDeclaration> constructorContracts = paramConstructorContractsByParam.get(paramType);
			if (constructorContracts == null) {
				constructorContracts = new ArrayList<MethodDeclaration>();
				paramConstructorContractsByParam.put(paramType, constructorContracts);
			}
			
			boolean hasDuplicates = false;
			
			String paramConstructorArgs = getParameterTypesString(constructorContract);
			for (MethodDeclaration existingContract : constructorContracts) {
				if (getParameterTypesString(existingContract).equals(paramConstructorArgs)) {
					printError(constructorContract, "Duplicate constructor contract for parameter " + paramType);
					printError(existingContract, "Duplicate constructor contract for parameter " + paramType);
					hasDuplicates = true;
				}
			}
			if (!hasDuplicates)
				constructorContracts.add(constructorContract);
		}
		
		List<FieldDeclaration> properties = new ArrayList<FieldDeclaration>();
		List<FieldDeclaration> propertiesToAddToConstructors = new ArrayList<FieldDeclaration>();
		//Map<Property, Pair<TypeMirror, String>> properties = new HashMap<Property, Pair<TypeMirror,String>>();
		for (FieldDeclaration field : decl.getFields()) {
			Property prop = field.getAnnotation(Property.class);
			if (prop == null)
				continue;
			
			properties.add(field);
			if (prop.inConstructors())
				propertiesToAddToConstructors.add(field);
		}
		for (FieldDeclaration field : properties) {
			String capitalizedName = AbstractProcessor.capitalize(field.getSimpleName());
			
			f.println(field.getDocComment());
			f.format("{0} get{2}();",
					field.getType(),
					field.getSimpleName(),
					capitalizedName);
			f.println(field.getDocComment());
			f.format("void set{2}({0} {1});",
					field.getType(),
					field.getSimpleName(),
					capitalizedName);
		}
		
		for (String type : genericParamNames) {
			String lengthName = "arraySize";
			if (lengthName.equals(type))
				lengthName = "_" + lengthName;
			
			f.format(array(
					"/** Returns the class of the generic parameter {1} */",
					"{0}<{1}> {1}();", 
					"",
					"/** Creates a new array of elements of class {1}.<br/>",
					"    Equivalent to a call to java.lang.reflect.Array.newInstance({2}.{1}(), " + lengthName + ") */",
					"{3}<{1}> " + String.format(NEW_ARRAY_METHOD_FORMAT, type) + "(int length);",
					""
				),
				Class.class.getName(),
				type, 
				interfaceName,
				Array.class.getName()
			);
			
			
		}
		
		f.println("/// Factory class for " + decl.getSimpleName() + genericParamsUsage);
		f.println("@SuppressWarnings(\"unchecked\")");
		f.println("public final class " + GENERATED_REIFICATOR_NAME + " {");
		
		for (ConstructorDeclaration constructor : decl.getConstructors()) {
			if (!constructor.getModifiers().contains(Modifier.PUBLIC))
				continue;
			
			String throwsClause = wrapImplosion(constructor.getThrownTypes(), "throws ", "");
			
			List<String> factoryArgsDeclaration = new ArrayList<String>();
			collectGenericParamsArgumentsDeclaration(genericParamNames, true, false, factoryArgsDeclaration);
			collectArgumentsDeclaration(constructor.getParameters(), false, false, factoryArgsDeclaration);
			for (FieldDeclaration propertyField : propertiesToAddToConstructors)
				factoryArgsDeclaration.add(propertyField.getType() + " " + propertyField.toString());
			
			String fullType = decl.getQualifiedName() + genericParamsUsage;
			f.println(constructor.getDocComment());
			f.printfn(
				"public static final %s %s newInstance(%s) %s {",
				genericParamsDefinition,
				fullType,
				implode(factoryArgsDeclaration), 
				throwsClause); 
					 
			List<String> paramClassesArgNames = collectGenericParamsArgumentsDeclaration(genericParamNames, false, true, null);
			List<String> constructorArgNames = collectArgumentsDeclaration(constructor.getParameters(), false, true, null);
			
			List<String> argCall = new ArrayList<String>();
			collectGenericParamsArgumentsDeclaration(genericParamNames, false, true, argCall);
			collectArgumentsDeclaration(constructor.getParameters(), false, true, argCall);
			
			String instanceName = "instance";
			while (genericParamNames.contains(instanceName)) instanceName = "_" + instanceName;
			f.printfn(
				"%s %s = new %s(%s) {",
				fullType,
				instanceName,
				decl.getSimpleName(),
				implode(constructorArgNames, ", "));
			
			for (FieldDeclaration field : properties) {
				String capitalizedName = AbstractProcessor.capitalize(field.getSimpleName());
				
				f.println(field.getDocComment());
				f.format("public {0} get{2}() '{' return {1}; '}'",
						field.getType(),
						field.getSimpleName(),
						capitalizedName);
				f.println();
				f.println(field.getDocComment());
				f.format("public void set{2}({0} {1}) '{' this.{1} = {1}; '}'",
						field.getType(),
						field.getSimpleName(),
						capitalizedName);
				f.println();
			}
			
			for (Map.Entry<String, List<MethodDeclaration>> e : paramConstructorContractsByParam.entrySet()) {
				String paramName = e.getKey();
				for (MethodDeclaration constructorContract : e.getValue()) {
					String contractThrowsClause = wrapImplosion(constructorContract.getThrownTypes(), "throws ", "");
						
					String paramConstructorArgsDef = implode(constructorContract.getParameters());
					f.println(constructorContract.getDocComment());
					f.println("@SuppressWarnings(\"unchecked\")");
					f.println("public " + paramName + " " + constructorContract.getSimpleName() + "(" + paramConstructorArgsDef + ") " + contractThrowsClause + " {");
					
					List<String> paramConstructorArgsTypes = new ArrayList<String>();
					List<String> paramConstructorArgsNames = new ArrayList<String>();
					for (ParameterDeclaration d : constructorContract.getParameters()) {
						paramConstructorArgsTypes.add(d.getType() + ".class");
						paramConstructorArgsNames.add(d.getSimpleName());
					}

					String exName = "ex", innerEx = "innerEx", innerExClass = "innerExClass";
					while (paramConstructorArgsDef.contains(exName)) exName = "_" + exName;
					while (paramConstructorArgsDef.contains(innerEx)) innerEx = "_" + innerEx;
					while (paramConstructorArgsDef.contains(innerExClass)) innerExClass = "_" + innerExClass;
					
					f.println(array(
						"try {",
						"return (" + paramName +")" + paramName + "().getConstructor(" + implode(paramConstructorArgsTypes) + ").newInstance(" + implode(paramConstructorArgsNames)+");",
						"} catch (" + SecurityException.class.getName() + " " + exName + ") {",
						"	throw new " + ViolatedTemplateConstraintException.class.getName() + "(\"Cannot access to this constructor of template parameter class \" + " + paramName + "().getClass().getName(), " + exName + ");",
						"} catch (" + IllegalAccessException.class.getName() + " " + exName + ") {",
						"	throw new " + ViolatedTemplateConstraintException.class.getName() + "(\"Cannot invoke the constructor of template parameter class \" + " + paramName + "().getClass().getName(), " + exName + ");",
						"} catch (" + NoSuchMethodException.class.getName() + " " + exName + ") {",
						"	throw new " + ViolatedTemplateConstraintException.class.getName() + "(\"Template parameter class \" + " + paramName + "().getClass().getName() + \" does not have the expected constructor\", " + exName + ");",
						"} catch (" + IllegalArgumentException.class.getName() + " " + exName + ") {",
						"	throw new " + RuntimeException.class.getName() + "(\"Internal Jeneral exception\", " + exName + ");",
						"} catch (" + InstantiationException.class.getName() + " " + exName + ") {",
						"	throw new " + ViolatedTemplateConstraintException.class.getName() + "(\"Template parameter class \" + " + paramName + "().getClass().getName() + \" is abstract and cannot be instantiated\", " + exName + ");",
						"} catch (" + InvocationTargetException.class.getName() + " " + exName + ") {",
						"	" + Throwable.class.getName() + " " + innerEx + " = " + exName + ".getCause();",
						"	" + Class.class.getName() + "<? extends " + Throwable.class.getName() + "> " + innerExClass + " = " + innerEx + ".getClass();"
					));
					f.println("if (" + RuntimeException.class.getName() + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + RuntimeException.class.getName() + ")" + innerEx + ";");
					for (ReferenceType expectedException : constructorContract.getThrownTypes())
						f.println("if (" + expectedException + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + expectedException + ")" + innerEx + ";");
					
					f.println(array(
						"	throw new " + ViolatedTemplateConstraintException.class.getName() + "(\"Template parameter constructor throwed a undeclared checked exception of type \" + " + innerEx + ".getClass().getName(), " + innerEx + ");",
						"}"
					));
					
					f.println("}");
				}
			}
			
			for (int i = 0, len = genericParamNames.size(); i < len; i++) {
				String type = genericParamNames.get(i);
				String argName = paramClassesArgNames.get(i);
	
				String lengthName = "arraySize";
				if (lengthName.equals(type))
					lengthName = "_" + lengthName;
				
				f.format(array(
					"public final {0}<{1}> {1}() '{'",
						"return {2};", 
					"'}'"), 
					Class.class.getName(), 
					type,
					argName);
				
				//f.format(array(
				f.println("public final " + Array.class.getName() + "<" + type + "> " + type + "(int " + lengthName + ") {");
				f.println("return " + ReificationUtils.class.getName() + ".newArray(" + argName + ", " + lengthName + ");");
				f.println("}");
			}
			f.println("};");
			for (FieldDeclaration propertyField : propertiesToAddToConstructors) {
				String propertyName = propertyField.getSimpleName();
				f.format("{0}.set{2}({1});", instanceName, propertyName, AbstractProcessor.capitalize(propertyName));
			}
			
			f.println("return " + instanceName + ";");
			f.println("}");
		}
		f.println("}");

		f.println("}");
		f.close();
		
		environment.addListener(new AnnotationProcessorListener() {
			
		});
		
		for (InterfaceType inter : decl.getSuperinterfaces()) {
			TypeMirror erased = environment.getTypeUtils().getErasure(inter);
//			log("inter = " + inter);
//			log("erased = " + erased);
			if (erased.toString().equals(interfaceQualifiedName)) {
				String fullTemplateInterfaceImplementsReference = interfaceQualifiedName + genericParamsUsage;
				if (!inter.toString().equals(fullTemplateInterfaceImplementsReference)) {
					printError(decl, "Template class " + decl.getSimpleName() + genericParamsDefinition + " must implement its template interface with the same parameters. Expecting " + interfaceName + genericParamsUsage);
				}
			}
		}
//		log("decl.getSuperinterfaces() = " + decl.getSuperinterfaces());
//		log("decl = " + decl);
	}
	
	private String getParameterTypesString(MethodDeclaration constructorContract) {
		List<String> list = new ArrayList<String>();
		for (ParameterDeclaration d : constructorContract.getParameters()) {
			list.add(d.getType().toString());
		}
		return implode(list);
	}

	private void processInstantiation(Declaration decl) {
		Instantiate instantiation = decl.getAnnotation(Instantiate.class);
		Template template = instantiation.type().getAnnotation(Template.class);
		if (template == null) {
			printError(decl, "Type " + instantiation.type().getName() + " is not a template. You must annotate it with " + Template.class.getName());
			return;
		}
	}
}

