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

import static com.ochafik.lang.SyntaxUtils.array;
import static com.ochafik.util.string.StringUtils.implode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ochafik.lang.jeneral.AbstractProcessor.LinesFormatter;
import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Instantiate;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.annotations.TemplatesHelper;
import com.ochafik.util.CompoundCollection;
import com.ochafik.util.listenable.Pair;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;

/*
cd /Users/ochafik/Prog/Java && rm templates_logs.txt >/dev/null ; apt -factory com.ochafik.lang.templates.TemplateProcessorFactory -d classes/ -s sources/.apt_generated/ -cp sources:classes sources/com/ochafik/lang/templates/*.java && open templates_logs.txt
 */
public class TemplateProcessor extends AbstractProcessor {
	private static final String 
		GENERATED_INTERFACE_SUFFIX = "_Template",
		GENERATED_FACTORY_NAME = "Factory",
		GENERATED_IMPLEMENTATION_NAME = "Reification";
		
	private Class<?>[] deps = array(
		ReificationUtils.class,
		TemplatesHelper.class
	);
 
	public TemplateProcessor(AnnotationProcessorEnvironment env){
		super(env);
		//log("Template Processor created");
	}

	class TemplateInfo {
		ClassDeclaration classDeclaration;
		String templateInterfaceQualifiedName;
		String templateInterfaceName;
		String packageName;
		String genericParamsDefinition;
		
		List<String> genericParamNames;
		String genericParamsUsage;
		
		Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> paramConstructorContracts;
		List<FieldDeclaration> properties;
		List<FieldDeclaration> propertiesToAddToConstructors;
		String qualifiedTemplateNameWithGenericsUsage;
		
		public TemplateInfo(ClassDeclaration classDeclaration) {
			this.classDeclaration = classDeclaration;
			templateInterfaceQualifiedName = classDeclaration.getQualifiedName() + GENERATED_INTERFACE_SUFFIX;
			templateInterfaceName = classDeclaration.getSimpleName() + GENERATED_INTERFACE_SUFFIX;
			
			packageName = classDeclaration.getPackage().getQualifiedName();
			List<String> genDefs = new ArrayList<String>();
			for (TypeParameterDeclaration d : classDeclaration.getFormalTypeParameters()) {
				genDefs.add(d.getSimpleName() + wrappedImplosion(d.getBounds(), " extends ", ""));
			}
			genericParamsDefinition = wrappedImplosion(genDefs, "<", ">");
			
			genericParamNames = new ArrayList<String>(getFormalTypeNames(classDeclaration));
			genericParamsUsage = wrappedImplosion(genericParamNames, "<", ">");
			
			paramConstructorContracts = getGenericParamConstructorsContracts(classDeclaration, genericParamNames);
			properties = new ArrayList<FieldDeclaration>();
			propertiesToAddToConstructors = new ArrayList<FieldDeclaration>();
			for (FieldDeclaration field : classDeclaration.getFields()) {
				Property prop = field.getAnnotation(Property.class);
				if (prop == null)
					continue;
				
				if (field.getModifiers().contains(Modifier.PRIVATE)) {
					printError(field, "Properties cannot be private.");
				}
					
				properties.add(field);
				
				if (prop.addToConstructors()) {
					if (field.getModifiers().contains(Modifier.FINAL))
						printError(field, "Cannot define a final field in any of this template's implementation constructors, as final fields must be initialized by a constructor of this class.");
					else 
						propertiesToAddToConstructors.add(field);
				}
			}
			qualifiedTemplateNameWithGenericsUsage = classDeclaration.getQualifiedName() + genericParamsUsage;
		}
	};
	
	class ConstructorInfo {
		final ConstructorDeclaration constructorDeclaration;
		final List<String> originalCtorArgNames;
		final Set<String> existingArgumentNames;
		final Map<String, String> genericParamClassArgNames;
		final Map<String, String> propertiesInitArgNames;
		final String arraySizeArgName;
		final List<String> generatedConstructorArgumentsDeclarations;
		final List<String> generatedConstructorArguments;
		final List<String> generatedFactoryArgumentsDeclarations;
		final List<String> generatedFactoryArguments;
		public ConstructorInfo(TemplateInfo templateClassInfo, ConstructorDeclaration constructorDeclaration) {
			this.constructorDeclaration = constructorDeclaration;
			originalCtorArgNames = getArgumentNames(constructorDeclaration, null);
			existingArgumentNames = new TreeSet<String>(originalCtorArgNames);
			genericParamClassArgNames = new TreeMap<String, String>();
			propertiesInitArgNames = new TreeMap<String, String>();
			
			for (String genericParamName : templateClassInfo.genericParamNames)
				genericParamClassArgNames.put(genericParamName, chooseUniqueName(decapitalize(genericParamName), existingArgumentNames, true)); 
			for (FieldDeclaration propertyDecl : templateClassInfo.propertiesToAddToConstructors)
				propertiesInitArgNames.put(propertyDecl.getSimpleName(), chooseUniqueName(decapitalize(propertyDecl.getSimpleName()), existingArgumentNames, true));
			
			arraySizeArgName = chooseUniqueName("arraySize", existingArgumentNames, false);
			
			// Create the list of arguments declarations
			generatedFactoryArgumentsDeclarations = new ArrayList<String>();
			generatedFactoryArguments = new ArrayList<String>();
			for (String genericParamName : templateClassInfo.genericParamNames) {
				String name = genericParamClassArgNames.get(genericParamName);
				generatedFactoryArgumentsDeclarations.add("final " + typedClass(genericParamName) + " " + name);
				generatedFactoryArguments.add(name);
			}
			
			generatedConstructorArgumentsDeclarations = new ArrayList<String>();
			generatedConstructorArguments = new ArrayList<String>();
			for (Declaration d : union(constructorDeclaration.getParameters(), templateClassInfo.propertiesToAddToConstructors)) {
				String type = getType(d);
				if (type == null)
					continue;
				
				String name = d.getSimpleName();
				try {
					TypeDeclaration t = environment.getTypeDeclaration(type);
					type = t.getQualifiedName();
				} catch (Exception ex) {}
				generatedConstructorArgumentsDeclarations.add("final " + type + " " + name);
				generatedConstructorArguments.add(name);
			}
			generatedFactoryArgumentsDeclarations.addAll(generatedConstructorArgumentsDeclarations);
			generatedFactoryArguments.addAll(generatedConstructorArguments);
		}
	}
	static String getType(Declaration d) {
		if (d instanceof ParameterDeclaration) {
			return ((ParameterDeclaration)d).getType().toString();
		}
		if (d instanceof FieldDeclaration) {
			return ((FieldDeclaration)d).getType().toString();
		}
		return null;
	}
	static <E> Collection<? extends E> union(Collection<? extends E>... cols) {
		return new CompoundCollection<E>(Arrays.asList(cols));
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
				logError(dec, t);
			}
		}
		
		AnnotationTypeDeclaration instantiationAnno = getAnnotationType(Instantiate.class);
		for (Declaration dec : environment.getDeclarationsAnnotatedWith(instantiationAnno)) {
			try {
				processInstantiation(dec);
			} catch (Throwable t) {
				logError(dec, t);
			}
		}
	}
	
	private void processTemplateClass(ClassDeclaration classDeclaration) throws IOException {
		AnnotationMirror templateAnnotationMirror = null;
		for (AnnotationMirror annoMirror : classDeclaration.getAnnotationMirrors()) {
			if (annoMirror.getAnnotationType().toString().equals(Template.class.getName())) {
				templateAnnotationMirror = annoMirror;
				break;
			}
		}
		if (templateAnnotationMirror == null) 
			return;
		
		if (classDeclaration.getDeclaringType() != null) {
			printError(templateAnnotationMirror.getPosition(), "Cannot define nested classes as templates.");
			return;
		}
		
		if (!classDeclaration.getModifiers().contains(Modifier.ABSTRACT)) {
			printError(classDeclaration, "Template "+classDeclaration.getQualifiedName()+" must be declared as abstract.");
		}
		
		TemplateInfo templateClassInfo = new TemplateInfo(classDeclaration);
		
		LinesFormatter f = new LinesFormatter(environment.getFiler().createSourceFile(templateClassInfo.templateInterfaceQualifiedName), "");
		
		f.println(array(
			"//",
			"// This file was autogenerated by " + getClass().getName() + " from " + classDeclaration.getQualifiedName(),
			"//",
			templateClassInfo.packageName.length() == 0 ? null : "package " + templateClassInfo.packageName + ";",
			"",
			"interface " + templateClassInfo.templateInterfaceName + templateClassInfo.genericParamsDefinition + " extends " + TemplateInstance.class.getName() + " {",
			""
		));
		
		for (FieldDeclaration field : templateClassInfo.properties) {
			String propertyName = field.getSimpleName();
			
			f.println(field.getDocComment());
			f.format("{0} get{1}();", field.getType(), capitalize(propertyName));
			f.println();
			
			if (!field.getModifiers().contains(Modifier.FINAL)) {
				f.println(field.getDocComment());
				f.format("void set{0}({1} {2});", capitalize(propertyName), field.getType(), propertyName);
				f.println();
			}
		}
		
		// Headers for T() and T(int)
		for (String genericParamName : templateClassInfo.genericParamNames) {
			String lengthName = chooseUniqueName("arraySize", Collections.singleton(genericParamName), false);
			
			f.format(array(
					"/** Get the class of the generic parameter {1} */",
					"{0} {1}();",
					"",
					"/** Create a new array of elements of class {1}.<br/>",
					"    Equivalent to a call to java.lang.reflect.Array.newInstance({2}.{1}(), " + lengthName + ") */",
					"{3}<{1}> " + genericParamName + "(int " + lengthName + ");",
					""
				),
				typedClass(genericParamName),
				genericParamName, 
				templateClassInfo.templateInterfaceName,
				Array.class.getName()
			);
			
			
		}
		
		List<ConstructorInfo> constructorInfos = new ArrayList<ConstructorInfo>();
		for (ConstructorDeclaration originalConstructor : classDeclaration.getConstructors())
			if (originalConstructor.getModifiers().contains(Modifier.PUBLIC))
				constructorInfos.add(new ConstructorInfo(templateClassInfo, originalConstructor));
		
		createFactoryClassCode(f, templateClassInfo, constructorInfos);
		
		f.println("}");
		f.close();
		
		checkTemplateClassGenericParametersMatchItsInterface(templateClassInfo);
	}

	private void createImplementationClassCode(LinesFormatter f, TemplateInfo templateInfo, List<ConstructorInfo> ctorInfos) {
		f.println("/// Concrete implementation of " + templateInfo.classDeclaration.getSimpleName() + templateInfo.genericParamsUsage);
		//f.println("@SuppressWarnings(\"unchecked\")");
		f.println("private static abstract class " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsDefinition + " extends " + templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage + " {");
	
		Set<String> existingFields = new TreeSet<String>();
		existingFields.addAll(templateInfo.genericParamNames);
		
		for (ConstructorInfo ctorInfo : ctorInfos) {
			//String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public " + GENERATED_IMPLEMENTATION_NAME + "(%s) %s {",
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")
			); 
			
			f.println("super(" + implode(ctorInfo.originalCtorArgNames, ", ") + ");");
			
			// Generate properties setting statements
			for (FieldDeclaration propertyField : templateInfo.propertiesToAddToConstructors) {
				String propertyName = propertyField.getSimpleName();
				f.println("this." + propertyName + " = " + ctorInfo.propertiesInitArgNames.get(propertyName) + ";");
			}
			
			f.println("}");
			f.println();
		}
		
		writeGettersAndSetters(f, templateInfo);
		writeGenericParameterConstructingMethodsCode(f, templateInfo);
		
		
		f.println("public " + TemplateClass.class.getName() + " getTemplateClass() {");
		f.println("throw new " + UnsupportedOperationException.class.getName() + "();");
		f.println("}");
		
		f.println("}");		
	}
	
	private void writeGenericParamRelatedMethods(LinesFormatter f, TemplateInfo templateInfo, ConstructorInfo ctorInfo) {
		// Generate methods T() (generic parameter class getter) and T(int) (array builder)
		for (String genericParamName : templateInfo.genericParamNames) {
			String argName = ctorInfo.genericParamClassArgNames.get(genericParamName);
			f.println(array(
				"public final " + typedClass(genericParamName) + " " + genericParamName + "() { return " + argName + "; }",
				"public final " + Array.class.getName() + "<" + genericParamName + "> " + genericParamName + "(int arraySize) {",
					"return " + ReificationUtils.class.getName() + ".newArray(" + argName + ", arraySize);",
				"}"
			));
		}
		
	}
	private void writeGettersAndSetters(LinesFormatter f, TemplateInfo templateInfo) {
		// Implement properties getters and setters
		for (FieldDeclaration field : templateInfo.properties) {
			String capitalizedName = capitalize(field.getSimpleName());
			
			f.println(field.getDocComment());
			f.format("public {0} get{2}() '{' return {1}; '}'",
					field.getType(),
					field.getSimpleName(),
					capitalizedName);
			f.println();
			
			if (!field.getModifiers().contains(Modifier.FINAL)) {
				f.println(field.getDocComment());
				f.format("public void set{2}({0} {1}) '{' this.{1} = {1}; '}'",
						field.getType(),
						field.getSimpleName(),
						capitalizedName);
				f.println();
			}
		}
	}
	private void createFactoryClassCode(LinesFormatter f, TemplateInfo templateInfo, List<ConstructorInfo> ctorInfos) {
		f.println("/// Factory class for " + templateInfo.classDeclaration.getSimpleName() + templateInfo.genericParamsUsage);
		f.println("@SuppressWarnings(\"unchecked\")");
		f.println("public final static class " + GENERATED_FACTORY_NAME + " {");
		
		for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			
			f.printfn(
					"public static final %s %s newInstance(%s) %s {",
					templateInfo.genericParamsDefinition,
					qualifiedTemplateInterfaceNameWithGenericsUsage,
					implode(ctorInfo.generatedFactoryArgumentsDeclarations), 
					wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
			
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			f.printfn("%s %s = new %s(%s) {",
				templateInfo.qualifiedTemplateNameWithGenericsUsage,
				instanceName,
				GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage,
				implode(ctorInfo.generatedConstructorArguments)
			);
			
			writeGenericParamRelatedMethods(f, templateInfo, ctorInfo);
			//createImplementationClassCode(f, templateInfo, ctorInfos, true, ctorInfo.genericParamClassArgNames);
			
			f.println("};");
			f.println("return " + instanceName + ";");
			f.println("}");
		}
		
		createImplementationClassCode(f, templateInfo, ctorInfos);
		
		/*for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public static final %s %s newInstance(%s) %s {",
				templateInfo.genericParamsDefinition,
				qualifiedTemplateInterfaceNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
					 
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			f.println("}");
		}
		//createImplementationClassCode(f, templateInfo, ctorInfos, false, null);
		*/
		/*
		for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public static final %s %s newInstance(%s) %s {",
				templateInfo.genericParamsDefinition,
				qualifiedTemplateInterfaceNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
					 
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			f.printfn("%s %s = new %s(%s) {",
				templateInfo.qualifiedTemplateNameWithGenericsUsage,
				instanceName,
				templateInfo.qualifiedTemplateNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArguments)
			);
			
			createImplementationClassCode(f, templateInfo, ctorInfos, true, ctorInfo.genericParamClassArgNames);
			
			f.println("};");
			
			f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			//f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			f.println("}");
		}*/
		
		
		
		f.println("}");		
	}

	private void checkTemplateClassGenericParametersMatchItsInterface(TemplateInfo templateClassInfo) {

		String fullTemplateInterfaceImplementsReference = (templateClassInfo.templateInterfaceQualifiedName + templateClassInfo.genericParamsUsage).replaceAll("\\s+", "");
		for (InterfaceType inter : templateClassInfo.classDeclaration.getSuperinterfaces()) {
			TypeMirror erased = environment.getTypeUtils().getErasure(inter);
			if (erased.toString().equals(templateClassInfo.templateInterfaceQualifiedName)) {
				if (!inter.toString().replaceAll("\\s+", "").equals(fullTemplateInterfaceImplementsReference)) {
					printError(templateClassInfo.classDeclaration, "Template class " + templateClassInfo.classDeclaration.getSimpleName() + templateClassInfo.genericParamsDefinition + " must implement its template interface with the same parameters. Expecting " + templateClassInfo.templateInterfaceName + templateClassInfo.genericParamsUsage);
				}
			}
		}
	}
	
	/**
	Generate generic parameter constructing methods, based on param constructor contracts such as :
 	@@Initializer T new_T(...) throws ...;
	*/
	private void writeGenericParameterConstructingMethodsCode(LinesFormatter f, TemplateInfo templateClassInfo) {
		for (Map.Entry<String, Pair<MethodDeclaration, List<MethodDeclaration>>> e : templateClassInfo.paramConstructorContracts.entrySet()) {
			String paramName = e.getKey();
			
			MethodDeclaration neutralValueDeclaration = e.getValue().getFirst();
			if (neutralValueDeclaration != null) {
				f.println(neutralValueDeclaration.getDocComment());
				f.println("@SuppressWarnings(\"unchecked\")");
				f.println("public " + paramName + " " + neutralValueDeclaration.getSimpleName() + "() {");
				f.println("return " + ReificationUtils.class.getName() + ".getNeutralValue(" + paramName + "());");
				//f.println("return " + paramName + "().isPrimitive() ? (" + paramName + ")0 : null;");
				f.println("}");
			}
			for (MethodDeclaration constructorContract : e.getValue().getSecond()) {
				String contractThrowsClause = wrappedImplosion(constructorContract.getThrownTypes(), "throws ", "");
					
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

				Set<String> existingArgNames = new TreeSet<String>(paramConstructorArgsNames);
				
				String exName = chooseUniqueName("ex", existingArgNames, true),
					innerEx = chooseUniqueName("innerEx", existingArgNames, true),
					innerExClass = chooseUniqueName("innerExClass", existingArgNames, true);
				
				f.println(array(
					"try {",
					"return " + ReificationUtils.class.getName() + ".newInstance(" + paramName +"(), new Class[] {" + implode(paramConstructorArgsTypes) + "}, new Object[] {" + implode(paramConstructorArgsNames) + "});",
					//"return (" + paramName +")" + paramName + "().getConstructor(" + implode(paramConstructorArgsTypes) + ").newInstance(" + implode(paramConstructorArgsNames)+");",
					"} catch (" + SecurityException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Cannot access to the constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ")\", " + exName + ");",
					"} catch (" + IllegalAccessException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Cannot invoke the constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") \", " + exName + ");",
					"} catch (" + NoSuchMethodException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"The expected constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") does not exist\", " + exName + ");",
					"} catch (" + IllegalArgumentException.class.getName() + " " + exName + ") {",
					"	throw new " + RuntimeException.class.getName() + "(\"Internal Jeneral exception\", " + exName + ");",
					"} catch (" + InstantiationException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Template parameter class \" + " + paramName + "().getName() + \" is abstract and cannot be instantiated\", " + exName + ");",
					"} catch (" + InvocationTargetException.class.getName() + " " + exName + ") {",
					"	" + Throwable.class.getName() + " " + innerEx + " = " + exName + ".getCause();",
					"	" + typedClass("? extends " + Throwable.class.getName()) + " " + innerExClass + " = " + innerEx + ".getClass();"
				));
				f.println("if (" + RuntimeException.class.getName() + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + RuntimeException.class.getName() + ")" + innerEx + ";");
				for (ReferenceType expectedException : constructorContract.getThrownTypes())
					f.println("if (" + expectedException + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + expectedException + ")" + innerEx + ";");
				
				f.println(array(
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Template parameter constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") threw a undeclared checked exception of type \" + " + innerEx + ".getClass().getName(), " + innerEx + ");",
					"}"
				));
				
				f.println("}");
			}
		}
	}
	
	/**
	Get the list of generic parameter constructing methods contracts such as :
 	@@Initializer T new_T(...) throws ...;
	*/
	private Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> getGenericParamConstructorsContracts(ClassDeclaration decl, List<String> genericParamNames) {
		Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> paramConstructorContractsByParam = new HashMap<String, Pair<MethodDeclaration,List<MethodDeclaration>>>();
		for (MethodDeclaration constructorContract : decl.getMethods()) {
			ParamConstructor declAnn = constructorContract.getAnnotation(ParamConstructor.class);
			if (declAnn == null)
				continue;
			
			boolean canReturnNeutralValue = constructorContract.getParameters().size() == 0;
			
			if (declAnn.returnNeutralValue() && !canReturnNeutralValue) {
				printError(constructorContract, "Neutral values may only be returned from initializers with no argument.");
			}
			
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
			Pair<MethodDeclaration, List<MethodDeclaration>> pair = paramConstructorContractsByParam.get(paramType);
			if (pair == null) {
				pair = new Pair<MethodDeclaration, List<MethodDeclaration>>(null, new ArrayList<MethodDeclaration>());
				paramConstructorContractsByParam.put(paramType, pair);
			}
			if (declAnn.returnNeutralValue()) {
				if (pair.getFirst() != null) {
					printError(constructorContract, "Duplicate neutral value getter for parameter " + paramType);
					printError(pair.getFirst(), "Duplicate neutral value getter for parameter " + paramType);
				} else {
					pair.setFirst(constructorContract);
				}
				continue;
			}
			List<MethodDeclaration> constructorContracts = pair.getSecond(); 
			
			boolean hasDuplicates = false;
			
			String paramConstructorArgs = constructParameterTypesString(constructorContract);
			for (MethodDeclaration existingContract : constructorContracts) {
				if (constructParameterTypesString(existingContract).equals(paramConstructorArgs)) {
					printError(constructorContract, "Duplicate constructor contract for parameter " + paramType);
					printError(existingContract, "Duplicate constructor contract for parameter " + paramType);
					hasDuplicates = true;
				}
			}
			if (!hasDuplicates)
				constructorContracts.add(constructorContract);
		}
		return paramConstructorContractsByParam;
	}

	private String constructParameterTypesString(MethodDeclaration constructorContract) {
		List<String> list = new ArrayList<String>();
		for (ParameterDeclaration d : constructorContract.getParameters()) {
			list.add(d.getType().toString());
		}
		return implode(list);
	}

	private void processInstantiation(Declaration decl) {
		Instantiate instantiation = decl.getAnnotation(Instantiate.class);
		Template template = instantiation.template().getAnnotation(Template.class);
		if (template == null) {
			printError(decl, "Type " + instantiation.template().getName() + " is not a template. You must annotate it with " + Template.class.getName());
			return;
		}
	}
}

