package com.ochafik.util.listenable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;

public class LazyList<T> extends SynchronizedListenableList<T> {
	Semaphore semaphore = new Semaphore(1);
	
	public LazyList(List<T> list) {
		super(list);
	}

	public synchronized boolean finished() {
		if (!semaphore.tryAcquire())
			return false;
		
		semaphore.release();
		return true;
	}
	public synchronized LazyList<T> buildInNewThread(final Runnable runnable) throws InterruptedException {
		semaphore.acquire();
		new Thread() {
			public void run() {
				try {
					runnable.run();
				} finally {
					semaphore.release();
				}
			}
		}.start();
		return this;
	}
	
	public int available() {
		return super.size();
	}
	
	@Override
	public Iterator<T> iterator() {
		return new SynchronizedListenableIterator() {
			@Override
			public boolean hasNext() {
				synchronized (mutex) {
					if (!super.hasNext())
						try {
							waitForEnd();
						} catch (InterruptedException e) {
							e.printStackTrace();
							return false;
						}
					return super.hasNext();
				}
			}
			@Override
			public T next() {
				if (hasNext())
					return super.next();
				else
					throw new NoSuchElementException();
			}
		};
	}
	
	public void waitForEnd() throws InterruptedException {
		semaphore.acquire();
		semaphore.release();
	}
	
	@Override
	public synchronized int size() {
		try {
			waitForEnd();
			return super.size();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
