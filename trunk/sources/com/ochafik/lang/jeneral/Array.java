package com.ochafik.lang.jeneral;

import com.ochafik.lang.templates.annotations.TemplatesHelper;

@TemplatesHelper
public interface Array<T> {
	public T get(int i);
	public void set(int i, T value);
	public int length();
	public Object getArray();
}
