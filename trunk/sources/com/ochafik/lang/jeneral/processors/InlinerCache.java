package com.ochafik.lang.jeneral.processors;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.ochafik.lang.jeneral.annotations.Inlinable;

import spoon.processing.FactoryAccessor;
import spoon.reflect.Factory;
import spoon.reflect.code.CtInvocation;

public class InlinerCache {
	
	private final Inliner corruptedInliner = new Inliner() { public void process(CtInvocation<?> element) {} };
	Map<Method, Inliner> inliners = new HashMap<Method, Inliner>();
	
	public InlinerCache(Factory factory) {
		this.factory = factory;
	}
	public Inliner getInliner(Method m) {
		if (m == null)
			return null;
		
		Inliner inliner = inliners.get(m);
		if (inliner == null) {
			Inlinable inlinable = m.getAnnotation(Inlinable.class);
			if (inlinable == null)
				return null;
				
			try {
				inliner = inlinable.inliner().newInstance();
				inliner.setTag(inlinable.tag());
				inliner.setFactory(factory);
			} catch (Exception e) {
				e.printStackTrace();
				inliner = corruptedInliner;
			}
			inliners.put(m, inliner);
		}
		return inliner == corruptedInliner ? null : inliner;
	}
	final Factory factory;
}
