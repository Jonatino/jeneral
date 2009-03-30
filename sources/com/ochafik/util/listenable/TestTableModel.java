package com.ochafik.util.listenable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class TestTableModel {
	public static void main(String[] args) {
		final JFrame f = new JFrame();
		final ListenableList<String> list = ListenableCollections.synchronizedList(ListenableCollections.listenableList(new ArrayList<String>()));
		ListenableListTableModel<String> model = new ListenableListTableModel<String>(list);
		JTable display = new JTable(model);
		
		ListenableListModel<String> model = new ListenableListModel<String>(list);
		JList display = new JList(model);
		f.getContentPane().add("Center", new JScrollPane(display));
		f.setSize(300, 600);
		final Process[] process = new Process[1];
		f.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) {
			process[0].destroy();
		}});
		new Thread() { public void run() {
			try {
				Process p = process[0] = Runtime.getRuntime().exec(new String[] {"find", "/"});
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					f.setVisible(true);
				}});
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					list.add(line);
					Thread.yield();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}}.start();
	}
}
