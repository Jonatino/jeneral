package com.ochafik.lang.jeneral.examples;

import javax.swing.JTable;

import com.ochafik.lang.jeneral.Array;
import com.ochafik.lang.jeneral.Property;
import com.ochafik.lang.jeneral.annotations.Initializer;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Template;

@Template
public abstract class TArray<T, U, V, W> implements TArray_Template<T, U, V, W> {
	@Property
	final Array<T> array;
	
	@Initializer (returnNeutralValue = true)
	protected abstract U new_U();
	
	public TArray(int length) {
		array = T(length);
		U u = new_U();
		System.out.println("new_U() -> " + u);
		/*try {
			for (int i = length; i-- != 0;) {
				tables[i] = new_W();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} */
	}
	public TArray(U u, V v, W w) {
		array = T(10);
	}
	
	public void set(int i, T value) {
		array.set(i, value);
	}
	public boolean accept(T e) {
		return false;
	}
	
	public void list() {
		for (int i = 0, len = array.length(); i < len; i++) {
			System.out.println(array.get(i));
		}
		/*for (JTable table : tables) {
			System.out.println(table);
		}*/
	}
	
	public static void main(String[] args) {
		int length = 10;
		//TArray<Integer, Integer, MyTable> intArray = TArray_Template.Factory.newInstance(Integer.class, Integer.class, MyTable.class, length);
		//TArray<Integer, Integer> intArray = TArray_Template.Factory.newInstance(Integer.class, Integer.TYPE, length);
		TArray<Integer, Integer, Integer, Integer> intArray= TArray_Template.Factory.newInstance(Integer.TYPE, Integer.class, Integer.TYPE, Integer.TYPE, length);
		intArray.set(2, 3);
		intArray.list();
	}
}
