package com.ochafik.lang.jeneral.examples;

import java.io.IOException;

import javax.swing.JTable;

import com.ochafik.lang.jeneral.Array;
import com.ochafik.lang.jeneral.TemplateContractViolationException;
import com.ochafik.lang.jeneral.annotations.InlineVelocity;
import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;

@Template
public abstract class TArray<T extends Comparable<T>, U, V, W> implements _TArray<T, U, V, W> {
	/** 
	 * This is sick
	 */
	@Property
	final Array<T> array; 
	
	/**
	 * int i() { return 0; }
	 * void processConstants() { return; }
	 */
	//@InlineVelocity
	//Glass  glass;
	
	@ParamConstructor(returnNeutralValue = true)
	protected abstract U new_U_neutral();
	
	@ParamConstructor
	protected abstract U new_U();
	 
	@ParamConstructor
	protected abstract U new_U(int e);
	
	@interface TypeContract { String name() default ""; }
	@interface IsPrimitive {}
	@interface FieldsContract {}
	@interface MethodsContract {}
	@interface ConstructorsContract {}
	@interface OptionalContractClause { String value(); }
	
	@TypeContract
	interface ValueType<T> extends ValueType_Contract<T> {
		interface Constructors<T> {
			@OptionalContractClause("optionalConstraint1")
			T T(int i);
			T T() throws IOException;
		}
		
		Glass getGlass();
		
		@OptionalContractClause("minMax")
		interface Fields<T> {
			T MAX_VALUE();
			T MIN_VALUE();
			
			@OptionalContractClause("int")
			int int_(T instance);
		}
	}
	
	interface ValueType_Contract<T> {
		@FieldsContract // runtime annotation, to allow names mangling
		ValueType.Fields<T> getFields();
		//@MethodsContract // runtime annotation, to allow names mangling
		//ValueType.Methods getMethods();
		@ConstructorsContract // runtime annotation, to allow names mangling
		ValueType.Constructors<T> getConstructors();
		
		class Glass {
			
		}
		class Contract {
			static <T> ValueType<T> getContract(Class<T> c, String... neededOptionalClauses) {
				return null;
			}
		}
	}
	
	public TArray(int length) {
		array = T(length);
		getArray();
		//System.out.println("glass : " +glass.i()); 
		System.out.println("new_U() -> " + new_U());
		System.out.println("new_U_neutral() -> " + new_U_neutral());
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
	
	interface MinMaxContract_Contract<T> {
		@FieldsContract // runtime annotation, to allow names mangling
		MinMaxContract.Fields<T> getFields();
		class Contract {
			static <T> MinMaxContract<T> getContract(Class<T> c, String... neededOptionalClauses) {
				return null;
			}
		}
	}
	
	@TypeContract
	@IsPrimitive
	interface MinMaxContract<T> extends MinMaxContract_Contract<T> {
		interface Fields<T> {
			T MAX_VALUE();
			T MIN_VALUE(); 
		}
	}
	
	public T min() {
      if (array.length() == 0)
          throw new IllegalStateException("cannot find minimum of an empty list");
      
      try {
		T().getField("MAX_VALUE").get(null);
		T().getMethod("MAX_VALUE").invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
      
      MinMaxContract<T> minMaxContract = MinMaxContract_Contract.Contract.getContract(T());
      if (minMaxContract != null) {
    	  T min = minMaxContract.getFields().MAX_VALUE();
    	  int _pos = array.length();
    	  Array<T> _data = array;
    	  
          for (int i = 0; i < _pos; i++ ) {
          	if (_data.get(i).compareTo(min) < 0) {
          		min = _data.get(i);
          	}
          }
          return min;
      }
      return empty_T();
  }
      
      @ParamConstructor
      abstract T empty_T();

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
		TArray<Integer, Integer, Integer, Integer> intArray= TArray.template.newInstance(Integer.TYPE, Integer.class, Integer.TYPE, Integer.TYPE, length);
		intArray.set(2, 3);
		intArray.list();
				
		//TArray__int.class;
		
		TArray<Integer, Integer, Integer, Integer> intArray2  = TArray.template.newInstance(Integer.TYPE, Integer.class, Integer.TYPE, Integer.TYPE, length);
	}
}
