/*
   Copyright 2008 Olivier Chafik

   Licensed under the Apache License, Version 2.0 (the License);
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an AS IS BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This file comes from the Jalico project (Java Listenable Collections)

       http://jalico.googlecode.com/.
*/
package com.ochafik.util.listenable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

/**
 * Utility methods to make the most out of listenable collections.<br/>
 * Provides synchronized and read-only wrappers for listenable collections, lists, sets and maps.<br/>
 * Also provides two-ways automated synchronization between listenable collections (that "knows" about the differences between sets, lists and other kinds of collections).
 * @author ochafik
 *
 */
public class ListenableCollections {
	
	/**
	 * Create a listenable list that will dynamically reflect the contents of the source listenable collection.<br/>
	 * This is useful for instance when you want to put the contents of a set in a swing JList (then use new JList(new ListenableListModel(ListenableCollections.asList(yourSet)))). 
	 * @param <T> type of the elements of the collection
	 * @param source collection that is to be adapted to a listenable list
	 * @return source if it is already a listenable list, otherwise a new listenable list that is two-ways bound to the source collection. 
	 */
	public static <T> ListenableList<T> asList(ListenableCollection<T> source) {
		if (source instanceof ListenableList)
			return (ListenableList<T>)source;
		
		ListenableList<T> out = new DefaultListenableList<T>(new ArrayList<T>(source));
		bind(source, out);
		return out;
	}
	
	/**
	 * Setup two-ways automated synchronization between listenable collections<br/>
	 * Tries to deal with sets, lists and collections in the most intuitive way possible (synchronization should happen as the common sense would dictate it).<br/>
	 * It is possible to bind collections of different kinds (say, a set with a list).
 	 * @param <T> type of the elements of both collections
	 * @param a collection to keep synchronized with b
	 * @param b collection to keep synchronized with a
	 */
	public static <T> void bind(final ListenableCollection<T> a, final ListenableCollection<T> b) {
		CollectionListener<T> listener = new CollectionListener<T>() {
			boolean currentlyPropagating = false;
			
			@SuppressWarnings("unchecked")
			void add(T t, ListenableCollection<T> source, ListenableCollection<T> recipient) {
				if ((recipient instanceof List) && (source instanceof SortedSet)) {
					ListenableList<T> recipientList = (ListenableList<T>)recipient;
					Comparator<T> comparator = (Comparator<T>) ((SortedSet)source).comparator();
					int i;
					if (comparator != null) {
						i = Collections.binarySearch(recipientList, t, comparator);
					} else {
						// Assume T is comparable
						i = Collections.binarySearch((List)recipientList, t);
					}
					if (i >= 0) {
						recipientList.set(i, t);
					} else {
						recipientList.add(-i - 1, t);
					}
				} else {
					recipient.add(t);
				}
			}
			@SuppressWarnings("unchecked")
			public void collectionChanged(CollectionEvent<T> e) {
				// Avoid infinite propagation of events
				if (currentlyPropagating) 
					return;
				
				//System.out.println("Propagating "+e.getType() + " for elements "+e.getElements());
				try {
					currentlyPropagating = true;
					ListenableCollection<T> source = e.getSource(), recipient = source == a ? b : a;
					
					for (T t : e.getElements()) {
						switch (e.getType()) {
						case ADDED:
							add(t, source, recipient);
							break;
						case REMOVED:
							recipient.remove(t);
							break;
						case UPDATED:
							if (recipient instanceof List) {
								List<T> recipientList = (List<T>)recipient;
								int i = recipientList.indexOf(t);
								recipientList.set(i, t);
							} else if (recipient instanceof Set) {
								add(t, source, recipient);
							} else {
								// Add might do a duplicate, so first remove element.
								recipient.remove(t);
								add(t, source, recipient);
							}
							break;
						}
					}
				} finally {
					currentlyPropagating = false;
				}
			}
		};
		a.addCollectionListener(listener);
		b.addCollectionListener(listener);
	}
	
	public static final <T> ListenableSet<T> unmodifiableSet(ListenableSet<T> set) {
		return new UnmodifiableListenableSet<T>(set);
	}
	
	public static final <T> ListenableCollection<T> unmodifiableCollection(ListenableCollection<T> col) {
		return new UnmodifiableListenableCollection<T>(col);
	}
	
	public static final <K,V> ListenableMap<K,V> unmodifiableMap(ListenableMap<K,V> map) {
		return new UnmodifiableListenableMap<K,V>(map);
	}
	
	public static final <T> ListenableSet<T> synchronizedSet(ListenableSet<T> set) {
		return new SynchronizedListenableSet<T>(set);
	}
	
	public static final <T> ListenableCollection<T> synchronizedCollection(ListenableCollection<T> col) {
		return new SynchronizedListenableCollection<T>(col);
	}
	
	public static final <K,V> ListenableMap<K,V> synchronizedMap(ListenableMap<K,V> map) {
		return new SynchronizedListenableMap<K,V>(map);
	}
	
	/**
	 * Wraps a collection in a listenable collection.<br/>
	 * The type of the listenable collection returned will depend on that of the provided collection :
	 * <ul>
	 * <li>wrapping a Set<T> will return a ListenableSet&lt;T&gt;, using ListenableCollections.listenableSet(Set&lt;T&gt;)</li>
	 * <li>wrapping a List<T> will return a ListenableList&lt;T&gt;, using ListenableCollections.listenableList(List&lt;T&gt;)</li>
	 * <li>wrapping a ListenableCollection&lt;T&gt; will return the same object</li>
	 * <li>otherwise a instance of a ListenableCollection&lt;T&gt; will be returned.</li>
	 * </ul>
	 * @param <T>
	 * @param collectionToWrap
	 * @return listenable collection that uses the provided collection as storage
	 */
	public static final <T> ListenableCollection<T> listenableCollection(Collection<T> collectionToWrap) {
		if (collectionToWrap instanceof ListenableCollection)
			return (ListenableCollection<T>)collectionToWrap;
		
		if (collectionToWrap instanceof Set) {
			return listenableSet((Set<T>)collectionToWrap);
		} else if (collectionToWrap instanceof List) {
			return listenableList((List<T>)collectionToWrap);
		}
		return new DefaultListenableCollection<T>(collectionToWrap);
	}
	
	/**
	 * Wraps a list in a listenable list.<br/>
	 * If is provided with a list that implements the RandomAccess interface, this method returns a listenable list that also implements the RandomAccess interface. 
	 * @param <T>
	 * @param listToWrap
	 * @return listenable list that uses the provided list as storage
	 */
	public static final <T> ListenableList<T> listenableList(List<T> listToWrap) {
		if (listToWrap instanceof ListenableList)
			return (ListenableList<T>)listToWrap;
		
		if (listToWrap instanceof RandomAccess) {
			class RandomAccessListenableList extends DefaultListenableList<T> implements RandomAccess {
				public RandomAccessListenableList(List<T> l) {
					super(l);
				}
			};
			return new RandomAccessListenableList(listToWrap);
		}
		return new DefaultListenableList<T>(listToWrap);
	}
	
	/**
	 * Wraps a set in a listenable set.<br/>
	 * If is provided with a set that implements the SortedSet interface, this method returns a listenable set that also implements the SortedSet interface. 
	 * @param <T>
	 * @param setToWrap
	 * @return listenable set that uses the provided set as storage
	 */
	public static final <T> ListenableSet<T> listenableSet(Set<T> setToWrap) {
		if (setToWrap instanceof ListenableSet)
			return (ListenableSet<T>)setToWrap;
		
		if (setToWrap instanceof SortedSet) {
			return new DefaultListenableSortedSet<T>((SortedSet<T>)setToWrap);
		}
		return new DefaultListenableSet<T>(setToWrap);
	}
	
	public static final <K,V> ListenableMap<K,V> listenableMap(Map<K,V> x) {
		return new DefaultListenableMap<K,V>(x);
	}

	public static <U,V> Collection<V> adapt(Collection<U> col, Adapter<U, V> adapter) {
		return new AdaptedCollection<U, V>(col, adapter);
	}
	
}
