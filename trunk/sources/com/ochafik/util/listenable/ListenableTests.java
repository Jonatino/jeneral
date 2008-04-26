package com.ochafik.util.listenable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class ListenableTests {


	public static <K, V> Map<K, V> CopyHalf(Map<K, V> map) {
		Map<K, V> ret = new HashMap<K, V>();
		int s = map.size();
		int c = 0;
		for (Map.Entry<K, V> e : map.entrySet()) {
			ret.put(e.getKey(), e.getValue());
			c++;
			if (c >= s / 2)
				break;
		}
		return ret;
	}
	public static void main(String[] args) {
		DefaultListenableMap<String, String> map = new DefaultListenableMap<String, String>(new HashMap<String, String>());
		map.keySet().addCollectionListener(new CollectionListener<String>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<String> e) {
			System.out.println("  keySet : " + e.getType() + " " + e.getElements());
		}});
		
		map.entrySet().addCollectionListener(new CollectionListener<Map.Entry<String, String>>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<Map.Entry<String, String>> e) {
			System.out.println("entrySet : " + e.getType() + " " + e.getElements());
		}});
		
		map.values().addCollectionListener(new CollectionListener<String>() { public void collectionChanged(com.ochafik.util.listenable.CollectionEvent<String> e) {
			System.out.println("  values : " + e.getType() + " " + e.getElements());
		}});
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add("Center", 
			new JScrollPane(
				new JList(
					new ListenableListModel<Map.Entry<String, String>>(
						ListenableCollections.asList(
							map.entrySet()
						)
					)
				)
			)
		);
		f.pack();
		f.setVisible(true);
		
		//Map<String, String> map = new HashMap<String, String>();
		
		try { Thread.sleep(3000); } catch (InterruptedException e1) {}
		
		for (int i = 0; i < 10; i++) {
			map.put("k "+i + "", "v "+i + "");
		}
		
		System.out.println("Before :\n" +map);
		System.out.println();
		
		try { Thread.sleep(2000); } catch (InterruptedException e1) {}
		
		Iterator<String> it = map.keySet().iterator();
		it.next();
		it.remove();
		System.out.println("After keySet.remove :\n" +map);
		System.out.println();
		
		try { Thread.sleep(3000); } catch (InterruptedException e1) {}
		
		Iterator<Map.Entry<String, String>> itEnt = map.entrySet().iterator();
		itEnt.next();
		itEnt.remove();
		System.out.println("After entrySet.remove :\n" +map);
		System.out.println();
		
		Map<String, String> half = CopyHalf(map);
		System.out.println("Half :\n" +half);
		System.out.println();
		
		Map<String, String> copy = new DefaultListenableMap<String, String>(new HashMap<String, String>(map));
		
		try { Thread.sleep(3000); } catch (InterruptedException e1) {}
		
		map.keySet().retainAll(half.keySet());
		System.out.println("After keyset retain all on half :\n" +map);
		System.out.println();
		
		copy.keySet().removeAll(half.keySet());
		System.out.println("After keyset remove all on half :\n" +copy);
		System.out.println();
	}

}
