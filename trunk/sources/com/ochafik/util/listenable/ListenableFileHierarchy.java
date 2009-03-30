/**
 * 
 */
package com.ochafik.util.listenable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.sun.jna.examples.FileMonitor;
import com.sun.jna.examples.FileMonitor.FileEvent;

public class ListenableFileHierarchy {
	private static void updateHierarchy(File file, Set<File> set, FileMonitor monitor, FileFilter fileFilter, boolean recurse) {
		if (fileFilter != null && !fileFilter.accept(file))
			return; // file is rejected
		
		//System.out.println("updateHierarchy("+file+")");
		if (!set.contains(file)) {
			// As set might be listenable, adding directly without checking existence in the set might trigger UPDATED events.
			set.add(file);
			
			System.out.println("monitoring "+file);
			try {
				monitor.addWatch(file, FileMonitor.FILE_ANY, false);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
		}
		
		if (recurse) {
			System.out.println("recursing on "+file);
			if (file.isDirectory())
				for (File child : file.listFiles())
					updateHierarchy(child, set, monitor, fileFilter, true);
		}
	}
	final FileMonitor fileMonitor;
	ListenableSet<File> rootFiles = ListenableCollections.listenableSet(new HashSet<File>());
	
	public ListenableFileHierarchy() {
		FileMonitor.FileListener filesListener = new FileMonitor.FileListener() {
			public void fileChanged(FileEvent e) {
				synchronized (monitoredFiles) {
					File file = e.getFile();
					switch (e.getType()) {
					case FileMonitor.FILE_DELETED:
					case FileMonitor.FILE_NAME_CHANGED_OLD:
					case FileMonitor.FILE_RENAMED:
					case FileMonitor.FILE_UNWATCHED:
						monitoredFiles.remove(file);
						break;

					case FileMonitor.FILE_WATCHED:
					case FileMonitor.FILE_CREATED:
					case FileMonitor.FILE_ATTRIBUTES_CHANGED:
					case FileMonitor.FILE_MODIFIED:
					case FileMonitor.FILE_SIZE_CHANGED:
					case FileMonitor.FILE_SECURITY_CHANGED:
						// This should trigger update event if the file is already in the set
						monitoredFiles.add(file);
					}
				}
			}
		};
		this.fileMonitor = FileMonitor.createInstance();
		if (fileMonitor == null)
			throw new UnsupportedOperationException("No file monitor implementation on this platform !");
		
		fileMonitor.addFileListener(filesListener);
		
		rootFiles.addCollectionListener(new CollectionListener<File>() {
			public void collectionChanged(CollectionEvent<File> e) {
				try {
					synchronized (fileMonitor) {
						switch (e.getType()) {
						case ADDED:
							for (File file : e.getElements())
								fileMonitor.addWatch(file, FileMonitor.FILE_ANY, true);
							break;
						case REMOVED:
							for (File file : e.getElements())
								fileMonitor.removeWatch(file);
							break;
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	final ListenableSet<File> monitoredFiles = ListenableCollections.listenableSet(new TreeSet<File>());
	
	public ListenableSet<File> getHierarchyFiles() {
		return ListenableCollections.unmodifiableSet(monitoredFiles);
	}
	
	public ListenableSet<File> getRootFiles() {
		return rootFiles;
	}
	FileFilter fileFilter = new FileFilter() { public boolean accept(File file) {
		return !file.isHidden();
	}};
	
	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	public FileFilter getFileFilter() {
		return fileFilter;
	}
	
	public static void main(String[] args) {
		ListenableFileHierarchy hierarchy = new ListenableFileHierarchy();
		hierarchy.getRootFiles().add(new File("/Users/ochafik/Desktop/test"));
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add("Center", new JScrollPane(new JList(new ListenableListModel<File>(ListenableCollections.asList(hierarchy.getHierarchyFiles())))));
		f.pack();
		f.setVisible(true);
		
	}
}