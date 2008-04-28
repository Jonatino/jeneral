package com.ochafik.lang.jeneral;

public abstract class AbstractTemplateClass implements com.ochafik.lang.jeneral.TemplateClass {
      private final Class<?>[] genericTypes;
	 public AbstractTemplateClass(Class<?>... genericTypes) {
		    this.genericTypes = genericTypes;
	 }
	 public Class<?>[] getGenericTypes() { return genericTypes; }
}
                
