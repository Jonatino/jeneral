package com.ochafik.lang.jeneral.examples;

import com.ochafik.lang.jeneral.annotations.*;

@SummonTemplate(template = TDynamicArray.class, params = { @Param(@Value(Integer.class))})
public class TDynamicArrayTest {
	public static void main(String[] args) {
		TDynamicArray__int ia = new TDynamicArray__int();
	}
}
