package com.ochafik.lang.jeneral.runtime;


public abstract class AbstractTemplateClass implements com.ochafik.lang.jeneral.runtime.TemplateClass {
      private final Class<?>[] genericTypes;
	 public AbstractTemplateClass(Class<?>... genericTypes) {
		    this.genericTypes = genericTypes;
	 }
	 public Class<?>[] getGenericTypes() { return genericTypes; }
}
                
