package com.ochafik.lang.jeneral;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.ochafik.lang.jeneral.Templates.TemplatesPrimitive;
import com.ochafik.lang.templates.annotations.Instantiate;
import com.ochafik.lang.templates.annotations.Param;
import com.ochafik.lang.templates.annotations.Template;
import com.ochafik.lang.templates.annotations.TemplatesHelper;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/*
include META-INF/services/com.sun.mirror.apt.AnnotationProcessorFactory
*/
public class TemplateProcessorFactory implements AnnotationProcessorFactory {

	public AnnotationProcessor getProcessorFor(
			Set<AnnotationTypeDeclaration> atds,
			AnnotationProcessorEnvironment env) {
		
		return new TemplateProcessor(env);
	}

	public Collection<String> supportedAnnotationTypes() {
		return Arrays.asList(new String[] {
			Template.class.getName(), 
			Instantiate.class.getName(),
			Param.class.getName(),
			TemplatesPrimitive.class.getName(),
			TemplatesHelper.class.getName()
		});
	}

	public Collection<String> supportedOptions() {
		return Collections.EMPTY_LIST;
	}

}
