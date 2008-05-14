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
package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ochafik.lang.jeneral.annotations.TemplatesPrimitives;
import com.ochafik.lang.jeneral.annotations.Include;
import com.ochafik.lang.jeneral.annotations.Includes;
import com.ochafik.lang.jeneral.annotations.SummonTemplate;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.annotations.Value;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/*
include META-INF/services/com.sun.mirror.apt.AnnotationProcessorFactory
*/

// apt -cp sources:classes -factory com.ochafik.lang.jeneral.TemplateProcessorFactory -d classes/ -s sources/.apt_generated2 sources/DefaultPackageTemplate.java
public class TemplateProcessorFactory implements AnnotationProcessorFactory {

	PrintWriter pout;
	protected PrintWriter getLogger() {
		if (pout == null) {
			try {
				pout = new PrintWriter(new FileWriter(new File("/Users/ochafik/Prog/Java/templates_factory_logs.txt"), true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pout;
	}
	
	public TemplateProcessorFactory() {
		log("new TemplateProcessorFactory");
	}
	
	protected void log(Object o) {
		getLogger().println(o);
		getLogger().flush();
	}
	Map<InstantiationParams, Long> lastInstantiations = new HashMap<InstantiationParams, Long>();
	public boolean needsInstantiation(InstantiationParams params) {
		Long last = lastInstantiations.get(params);
		if (last == null)
			return true;
		
		File f = params.templateFile;
		if (f != null)
			return f.lastModified() > last;
			
		return true;
	}
	
	public void declareInstantiation(InstantiationParams params) {
		lastInstantiations.put(params, System.currentTimeMillis());
	}
	
	public AnnotationProcessor getProcessorFor(
			Set<AnnotationTypeDeclaration> atds,
			AnnotationProcessorEnvironment env) {
		
		return new TemplateProcessor(env, this);
	}

	public Collection<String> supportedAnnotationTypes() {
		return Arrays.asList(new String[] {
			Template.class.getName(), 
			SummonTemplate.class.getName(),
			Param.class.getName(),
			Value.class.getName(),
			Property.class.getName(),
			TemplatesPrimitives.class.getName(),
			ParamConstructor.class.getName(),
			//Array.class.getName(),
			Include.class.getName(),
			Includes.class.getName()
		});
	}

	@SuppressWarnings("unchecked")
	public Collection<String> supportedOptions() {
		return Collections.EMPTY_LIST;
	}

}
