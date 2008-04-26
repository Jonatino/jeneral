package com.ochafik.lang.jeneral.examples;

import javax.swing.JTable;

import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Template;

@Template
public abstract class TArray<T, U, V, W> implements TArray_Template<T, U, V, W> {
	final T[] array;
	//final JTable[] tables;
	
	protected abstract U new_U();
	
	public TArray(int length) {
		array = new_T_array(length);
		U u = new_U();
		System.out.println(u);
		/*try {
			for (int i = length; i-- != 0;) {
				tables[i] = new_W();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	public TArray(U u, V v, W w) {
		array = new_T_array(10);
	}
	
	
	public boolean accept(T e) {
		return false;
	}
	
	public void list() {
		/*for (JTable table : tables) {
			System.out.println(table);
		}*/
	}
	
	static class MyTable extends JTable {
		
	}
	public static void main(String[] args) {
		int length = 10;
		//TArray<Integer, Integer, MyTable> intArray = TArray_Template.Factory.newInstance(Integer.class, Integer.class, MyTable.class, length);
		//TArray<Integer, Integer> intArray = TArray_Template.Factory.newInstance(Integer.class, Integer.TYPE, length);
		TArray_Template.Factory.newInstance(Integer.class, Integer.class, Integer.TYPE, Integer.TYPE, length);
		//intArray.list();
	}
}
