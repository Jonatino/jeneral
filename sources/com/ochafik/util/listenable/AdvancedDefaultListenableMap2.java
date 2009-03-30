package com.ochafik.util.listenable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ochafik.util.CollectionAdapter;
import com.ochafik.util.DefaultAdapter;


public class AdvancedDefaultListenableMap2<K,V> implements ListenableMap<K,V> {
	Map<K,V> map;
 	ListenableSupport<K> keySetSupport;
 	WeakReference<ListenableSet<K>> keySetRef;
 	
 	ListenableSupport<V> valuesSupport;
 	WeakReference<ListenableCollection<V>> valuesRef;
 	
 	ListenableSupport<Map.Entry<K,V>> entrySetSupport;
 	WeakReference<ListenableSet<Map.Entry<K,V>>> entrySetRef;
 	
 	boolean propagatingEvent = false;
 	//ListenableSet<Map.Entry<K,V>> entrySet;
 	
 	public AdvancedDefaultListenableMap2(Map<K,V> map) {
		this.map = map;
		//keysCollectionSupport = new CollectionSupport<K>();
	}
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}
	public int size() {
		return map.size();
	}
	@Override
	public String toString() {
		return map.toString();
	}
	public void clear() {
		for (Iterator<K> it = keySet().iterator(); it.hasNext();) {
			it.next();
			it.remove();
		}
	}
	@Override
	public boolean equals(Object obj) {
		return map.equals(obj);
	}
	public V get(Object key) {
		return map.get(key);
	}
	@Override
	public int hashCode() {
		return map.hashCode();
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	private class ListenableEntry implements Map.Entry<K, V> {
		K key;
		V value;
		
		public ListenableEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}
		public V setValue(V value) {
			return put(key, value);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Map.Entry))
				return false;
				
			Map.Entry e = (Map.Entry)obj;
			if (key == null) {
				if (e.getKey() != null)
					return false;
			} else if (!key.equals(e.getKey()))
				return false;
			
			if (value == null) {
				if (e.getValue() != null)
					return false;
			}
			return value.equals(e.getValue());
		}
		
		@Override
		public String toString() {
			return "<"+getKey()+", "+getValue()+">";
		}
	}
	
	public V put(K key, V value) {
		V v = map.put(key,value);
		if (v !=null) {
			propagateUpdated(Collections.singleton(key), null);
		} else {
			propagateAdded(Collections.singleton(key), null);
		}
		return v;
	} 
	public void putAll(Map<? extends K, ? extends V> t) {
		for (Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
			put(e.getKey(),e.getValue());
		}
	}
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		V v = map.remove(key);
		if (v !=null) {
			propagateRemoved(Collections.singletonMap((K)key, (V)v).entrySet(), null);
		}
		return v;
	}
	
	/*Map<K, V> formerEntries;
	Collection<Map.Entry<K,V>> getFormerEntries(Collection<K> elements) {
		Collection<Map.Entry<K,V>> ret = new ArrayList<Map.Entry<K,V>>(elements.size());
		for (K k : elements) {
			ret.add(new ListenableEntry(k, formerEntries.get(k)));
		}
		return ret;
	}
	Collection<V> getFormerValues(Collection<K> elements) {
		Collection<V> ret = new ArrayList<V>(elements.size());
		for (K k : elements) {
			ret.add(formerEntries.get(k));
		}
		return ret;
	}*/
	Collection<Map.Entry<K,V>> getEntries(Collection<K> elements) {
		Collection<Map.Entry<K,V>> entries = new ArrayList<Map.Entry<K,V>>(elements.size());
		for (K k : elements) entries.add(new ListenableEntry(k, map.get(k)));
		return entries;
	}
	Collection<V> getValues(Collection<K> elements) {
		Collection<V> ret = new ArrayList<V>(elements.size());
		for (K k : elements) ret.add(map.get(k));
		return ret;
	}
	
	void propagateAdded(Collection<K> elements, ListenableCollection<?> eventSource) {
		if (propagatingEvent)
			throw new IllegalThreadStateException();
		
		propagatingEvent = true;
		try {	
			ListenableSet<K> keySet;
			if (keySetRef != null && (keySet = keySetRef.get()) != null && keySet != eventSource)
				keySetSupport.fireAdded(keySet, elements);
			
			ListenableSet<Map.Entry<K, V>> entrySet;
			if (entrySetRef != null && (entrySet = entrySetRef.get()) != null && entrySet != eventSource) {
				entrySetSupport.fireAdded(entrySet, getEntries(elements));
			}
				
			ListenableCollection<V> values;
			if (valuesRef != null && (values = valuesRef.get()) != null && values != eventSource) {
				valuesSupport.fireAdded(values, getValues(elements));
			}
		} finally {
			propagatingEvent = false;
		}
		//for (K k : elements)
		//	formerEntries.put(k, get(k));
	}
	void propagateRemoved(Collection<Map.Entry<K, V>> entries, ListenableCollection<?> eventSource) {
		if (propagatingEvent)
			throw new IllegalThreadStateException();
		
		propagatingEvent = true;
		try {	
			ListenableSet<K> keySet;
			if (keySetRef != null && (keySet = keySetRef.get()) != null && keySet != eventSource)
				keySetSupport.fireRemoved(keySet, new CollectionAdapter<Map.Entry<K,V>, K>(entries, entryToKeyAdapter));
			
			ListenableSet<Map.Entry<K, V>> entrySet;
			if (entrySetRef != null && (entrySet = entrySetRef.get()) != null && entrySet != eventSource) {
				entrySetSupport.fireRemoved(entrySet, entries);
			}
				
			ListenableCollection<V> values;
			if (valuesRef != null && (values = valuesRef.get()) != null && values != eventSource) {
				valuesSupport.fireRemoved(values, new CollectionAdapter<Map.Entry<K,V>, V>(entries, entryToValueAdapter));
			}
		} finally {
			propagatingEvent = false;
		}
	}
	void propagateUpdated(Collection<K> elements, ListenableCollection<?> eventSource) {
		if (propagatingEvent)
			throw new IllegalThreadStateException();
		
		propagatingEvent = true;
		try {	
			ListenableSet<K> keySet;
			if (keySetRef != null && (keySet = keySetRef.get()) != null && keySet != eventSource)
				keySetSupport.fireUpdated(keySet, elements);
			
			ListenableSet<Map.Entry<K, V>> entrySet;
			if (entrySetRef != null && (entrySet = entrySetRef.get()) != null && entrySet != eventSource) {
				//entrySetSupport.fireRemoved(entrySet, getFormerEntries(elements));
				//entrySetSupport.fireAdded(entrySet, getEntries(elements));
				entrySetSupport.fireUpdated(entrySet, getEntries(elements));
			}
				
			ListenableCollection<V> values;
			if (valuesRef != null && (values = valuesRef.get()) != null && values != eventSource) {
				//valuesSupport.fireRemoved(values, getFormerValues(elements));
				//valuesSupport.fireAdded(values, getValues(elements));
				valuesSupport.fireUpdated(values, getValues(elements));
			}
		} finally {
			propagatingEvent = false;
		}
		
		//for (K k : elements)
		//	formerEntries.put(k, get(k));
	}
	
	public ListenableSet<K> keySet() {
		ListenableSet<K> keySet;
		if (keySetRef == null || (keySet = keySetRef.get()) == null) {
			if (keySetSupport == null)
				keySetSupport = new ListenableSupport<K>();
			
			keySet = new DefaultListenableSet<K>(map.keySet(), keySetSupport);
 			keySet.addCollectionListener(new CollectionListener<K>() {
 				public void collectionChanged(CollectionEvent<K> e) {
 					//if (propagatingEvent)
 					//	return;
 					
 					//currentEventSource = e.getSource();
 					//try {
 					if (e.getType() == CollectionEvent.EventType.REMOVED) {
 						//propagateRemoved(e.getElements(), e.getSource());
 						//removeAll(e.getElements());
 					} else {
 						throw new UnsupportedOperationException("Set returned by ListenableMap.keySet() cannot be used to add or update elements");
 					}
 					//} finally {
 					//	currentEventSource = null;
 					//}
 				}

				
 			});
 			keySetRef = new WeakReference<ListenableSet<K>>(keySet);
		}
 		
 		return keySet;
	}
	Object currentEventSource;
	
	@SuppressWarnings("unused")
	private void removeAll(Collection<K> elements) {
		for (K k : elements)
			remove(k);
	}
	protected DefaultAdapter<Map.Entry<K, V>, K> entryToKeyAdapter = new DefaultAdapter<Map.Entry<K, V>, K>() { public K adapt(Map.Entry<K,V> value) { 
		return value.getKey(); 
	}};
	protected DefaultAdapter<Map.Entry<K, V>, V> entryToValueAdapter = new DefaultAdapter<Map.Entry<K, V>, V>() { public V adapt(Map.Entry<K,V> value) { 
		return value.getValue(); 
	}};
	
	public ListenableSet<Map.Entry<K,V>> entrySet() {
		ListenableSet<Map.Entry<K,V>> entrySet;
		if (entrySetRef == null || (entrySet = entrySetRef.get()) == null) {
			if (entrySetSupport == null)
				entrySetSupport = new ListenableSupport<Map.Entry<K,V>>();
			
			entrySet = new DefaultListenableSet<Map.Entry<K,V>>(map.entrySet(), entrySetSupport);
 			entrySet.addCollectionListener(new CollectionListener<Map.Entry<K,V>>() {
 				public void collectionChanged(CollectionEvent<Map.Entry<K,V>> e) {
 					if (e.getType() == CollectionEvent.EventType.REMOVED) {
 						propagateRemoved(e.getElements(), e.getSource());
 						//removeAll(e.getElements());
 					} else {
 						throw new UnsupportedOperationException("Set returned by ListenableMap.keySet() cannot be used to add or update elements");
 					}
 					
 					/*Collection<K> keys = new CollectionAdapter<Map.Entry<K, V>, K>(e.getElements(), entryToKeyAdapter);
 					
 					switch (e.getType()) {
 					case ADDED:
 						propagateAdded(keys, e.getSource());
 						break;
 					case REMOVED:
 						propagateRemoved(keys, e.getSource());
 						break;
 					case UPDATED:
 						propagateUpdated(keys, e.getSource());
 						break;
					}*/
 					/*if (propagatingEvent)
 						return;
 					
 					*/
 				}
 			});
 			entrySetRef = new WeakReference<ListenableSet<Map.Entry<K,V>>>(entrySet);
		}
 		
 		return entrySet;
	}
	
	public ListenableCollection<V> values() {
		ListenableCollection<V> values;
		if (valuesRef == null || (values = valuesRef.get()) == null) {
			if (valuesSupport == null)
				valuesSupport = new ListenableSupport<V>();
			
			values = new UnmodifiableListenableCollection<V>(new DefaultListenableCollection<V>(map.values(), valuesSupport));
			/*values.addCollectionListener(new CollectionListener<V>() {
 				public void collectionChanged(CollectionEvent<V> e) {
 					throw new UnsupportedOperationException("values() returned a read-only collection !");
 				}
 			});*/
 			valuesRef = new WeakReference<ListenableCollection<V>>(values);
		}
 		
 		return values;
	}
	
	public static void main(String[] args) {
		AdvancedDefaultListenableMap2<String, String> map = new AdvancedDefaultListenableMap2<String, String>(new HashMap<String, String>());
		map.keySet().addCollectionListener(new CollectionListener<String>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<String> e) {
			System.out.println("  keySet : " + e.getType() + " " + e.getElements());
		}});
		
		map.entrySet().addCollectionListener(new CollectionListener<Map.Entry<String, String>>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<Map.Entry<String, String>> e) {
			System.out.println("entrySet : " + e.getType() + " " + e.getElements());
		}});
		
		map.values().addCollectionListener(new CollectionListener<String>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<String> e) {
			System.out.println("  values : " + e.getType() + " " + e.getElements());
		}});
		
		for (int i = 0; i < 10; i++) {
			map.put("k "+i + "", "v "+i + "");
		}
		
		Iterator<String> it = map.keySet().iterator();
		it.next();
		it.remove();
	}
	
}
