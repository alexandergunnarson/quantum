/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.  Oracle designates this
* particular file as subject to the "Classpath" exception as provided
* by Oracle in the LICENSE file that accompanied this code.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
* or visit www.oracle.com if you need additional information or have any
* questions.
*/

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package quantum.core.data.queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -6903933977591709194L;

	static class Node<E> {
	    E item;
	
	    Node<E> next;
	
	    Node(E x) { item = x; }
	}
	
	private final int capacity;
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicBoolean writeBlocked = new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean(false);
	transient Node<E> head;
	private transient Node<E> last;
	private final ReentrantLock takeLock = new ReentrantLock();
	private final Condition notEmpty = takeLock.newCondition();
	private final ReentrantLock putLock = new ReentrantLock();
	private final Condition notFull = putLock.newCondition();
	private void signalNotEmpty() {
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lock();
	    try {
	        notEmpty.signal();
	    } finally {
	        takeLock.unlock();
	    }
	}
	
	private void signalNotFull() {
	    final ReentrantLock putLock = this.putLock;
	    putLock.lock();
	    try {
	        notFull.signal();
	    } finally {
	        putLock.unlock();
	    }
	}
	
	private void enqueue(Node<E> node) {
	    last = last.next = node;
	}
	
	private E dequeue() {
	    Node<E> h = head;
	    Node<E> first = h.next;
	    h.next = h; // help GC
	    head = first;
	    E x = first.item;
	    first.item = null;
	    return x;
	}
	
	void fullyLock() {
	    putLock.lock();
	    takeLock.lock();
	}
	
	void fullyUnlock() {
	    takeLock.unlock();
	    putLock.unlock();
	}
	
	boolean isFullyLocked() {
	    return (putLock.isHeldByCurrentThread() &&
	            takeLock.isHeldByCurrentThread());
	}
	    
	public LinkedBlockingQueue() {
	    this(Integer.MAX_VALUE);
	}
	
	public LinkedBlockingQueue(int capacity) {
	    if (capacity <= 0) throw new IllegalArgumentException();
	    this.capacity = capacity;
	    last = head = new Node<E>(null);
	}
	
	    
	public LinkedBlockingQueue(Collection<? extends E> c) {
	    this(Integer.MAX_VALUE);
	    final ReentrantLock putLock = this.putLock;
	    putLock.lock();
	    try {
	        int n = 0;
	        for (E e : c) {
	            if (e == null)
	                throw new NullPointerException();
	            if (n == capacity)
	                throw new IllegalStateException("Queue full");
	            enqueue(new Node<E>(e));
	            ++n;
	        }
	        count.set(n);
	    } finally {
	        putLock.unlock();
	    }
	}
	
	public int size() {
	    return count.get();
	}
	
	public int remainingCapacity() {
	    return capacity - count.get();
	}

	public void close() {
		if (!closed.get())
			closed.set(true);
			writeBlock();
	}

	public boolean isClosed() {
		return closed.get();
	}

	public void writeBlock() {
		if (!writeBlocked.get()) writeBlocked.set(true);
	}

	public boolean isWriteBlocked() {
		return writeBlocked.get();
	}
	
	public void put(E e) throws InterruptedException, IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    if (e == null) throw new NullPointerException();
	    int c = -1;
	    Node<E> node = new Node<E>(e);
	    final ReentrantLock putLock = this.putLock;
	    final AtomicInteger count = this.count;
	    putLock.lockInterruptibly();
	    try {
	        while (count.get() == capacity) {
	            notFull.await();
	        }
	        enqueue(node);
	        c = count.getAndIncrement();
	        if (c + 1 < capacity)
	            notFull.signal();
	    } finally {
	        putLock.unlock();
	    }
	    if (c == 0)
	        signalNotEmpty();
	}
	
	public boolean offer(E e, long timeout, TimeUnit unit)
	    throws InterruptedException, IllegalStateException {
	    if (isWriteBlocked()) throw new IllegalStateException();
	    if (e == null) throw new NullPointerException();

	    long nanos = unit.toNanos(timeout);
	    int c = -1;
	    final ReentrantLock putLock = this.putLock;
	    final AtomicInteger count = this.count;
	    putLock.lockInterruptibly();
	    try {
	        while (count.get() == capacity) {
	            if (nanos <= 0)
	                return false;
	            nanos = notFull.awaitNanos(nanos);
	        }
	        enqueue(new Node<E>(e));
	        c = count.getAndIncrement();
	        if (c + 1 < capacity)
	            notFull.signal();
	    } finally {
	        putLock.unlock();
	    }
	    if (c == 0)
	        signalNotEmpty();
	    return true;
	}
	
	public boolean offer(E e) throws IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    if (e == null) throw new NullPointerException();
	    
	    final AtomicInteger count = this.count;
	    if (count.get() == capacity)
	        return false;
	    int c = -1;
	    Node<E> node = new Node<E>(e);
	    final ReentrantLock putLock = this.putLock;
	    putLock.lock();
	    try {
	        if (count.get() < capacity) {
	            enqueue(node);
	            c = count.getAndIncrement();
	            if (c + 1 < capacity)
	                notFull.signal();
	        }
	    } finally {
	        putLock.unlock();
	    }
	    if (c == 0)
	        signalNotEmpty();
	    return c >= 0;
	}
	
	public E take() throws InterruptedException, IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    E x;
	    int c = -1;
	    final AtomicInteger count = this.count;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lockInterruptibly();
	    try {
	        while (count.get() == 0) {
	            notEmpty.await();
	        }
	        x = dequeue();
	        c = count.getAndDecrement();
	        if (c > 1)
	            notEmpty.signal();
	    } finally {
	        takeLock.unlock();
	    }
	    if (c == capacity)
	        signalNotFull();
	    return x;
	}
	
	public E poll(long timeout, TimeUnit unit) throws InterruptedException, IllegalStateException {
	    if (isWriteBlocked()) throw new IllegalStateException();
	    E x = null;
	    int c = -1;
	    long nanos = unit.toNanos(timeout);
	    final AtomicInteger count = this.count;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lockInterruptibly();
	    try {
	        while (count.get() == 0) {
	            if (nanos <= 0)
	                return null;
	            nanos = notEmpty.awaitNanos(nanos);
	        }
	        x = dequeue();
	        c = count.getAndDecrement();
	        if (c > 1)
	            notEmpty.signal();
	    } finally {
	        takeLock.unlock();
	    }
	    if (c == capacity)
	        signalNotFull();
	    return x;
	}
	
	public E poll() throws IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    final AtomicInteger count = this.count;
	    if (count.get() == 0)
	        return null;
	    E x = null;
	    int c = -1;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lock();
	    try {
	        if (count.get() > 0) {
	            x = dequeue();
	            c = count.getAndDecrement();
	            if (c > 1)
	                notEmpty.signal();
	        }
	    } finally {
	        takeLock.unlock();
	    }
	    if (c == capacity)
	        signalNotFull();
	    return x;
	}
	
	public E peek() throws IllegalStateException {
		if (isClosed()) throw new IllegalStateException();
	    if (count.get() == 0)
	        return null;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lock();
	    try {
	        Node<E> first = head.next;
	        if (first == null)
	            return null;
	        else
	            return first.item;
	    } finally {
	        takeLock.unlock();
	    }
	}

	// Source: Alexander Gunnarson
	public E blockingPeek() throws InterruptedException, IllegalStateException {
		if (isClosed()) throw new IllegalStateException();
	    final AtomicInteger count = this.count;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lockInterruptibly();
	    try {
	    	while (count.get() == 0) {
	            notEmpty.await();
	        }
	        Node<E> first = head.next;
	        if (first == null)
	            return null;
	        else
	            return first.item;
	    } finally {
	        takeLock.unlock();
	    }
	}

	// Source: Alexander Gunnarson
	public E blockingPeek(long timeout, TimeUnit unit) throws InterruptedException, IllegalStateException {
	    if (isClosed()) throw new IllegalStateException();
	    long nanos = unit.toNanos(timeout);
	    final AtomicInteger count = this.count;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lockInterruptibly();
	    try {
	        while (count.get() == 0) {
	            if (nanos <= 0)
	                return null;
	            nanos = notEmpty.awaitNanos(nanos);
	        }
	        Node<E> first = head.next;
	        if (first == null)
	            return null;
	        else
	            return first.item;
	    } finally {
	        takeLock.unlock();
	    }
	}
	
	void unlink(Node<E> p, Node<E> trail) {
	    p.item = null;
	    trail.next = p.next;
	    if (last == p)
	        last = trail;
	    if (count.getAndDecrement() == capacity)
	        notFull.signal();
	}
	
	public boolean remove(Object o) throws IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    if (o == null) return false;
	    fullyLock();
	    try {
	        for (Node<E> trail = head, p = trail.next;
	             p != null;
	             trail = p, p = p.next) {
	            if (o.equals(p.item)) {
	                unlink(p, trail);
	                return true;
	            }
	        }
	        return false;
	    } finally {
	        fullyUnlock();
	    }
	}

	public boolean contains(Object o) throws IllegalStateException {
		if (isClosed()) throw new IllegalStateException();
	    if (o == null) return false;
	    fullyLock();
	    try {
	        for (Node<E> p = head.next; p != null; p = p.next)
	            if (o.equals(p.item))
	                return true;
	        return false;
	    } finally {
	        fullyUnlock();
	    }
	}
	
	public Object[] toArray() {
	    fullyLock();
	    try {
	        int size = count.get();
	        Object[] a = new Object[size];
	        int k = 0;
	        for (Node<E> p = head.next; p != null; p = p.next)
	            a[k++] = p.item;
	        return a;
	    } finally {
	        fullyUnlock();
	    }
	}
	
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
	    fullyLock();
	    try {
	        int size = count.get();
	        if (a.length < size)
	            a = (T[])java.lang.reflect.Array.newInstance
	                (a.getClass().getComponentType(), size);
	
	        int k = 0;
	        for (Node<E> p = head.next; p != null; p = p.next)
	            a[k++] = (T)p.item;
	        if (a.length > k)
	            a[k] = null;
	        return a;
	    } finally {
	        fullyUnlock();
	    }
	}
	
	public String toString() {
	    fullyLock();
	    try {
	        Node<E> p = head.next;
	        if (p == null)
	            return "[]";
	
	        StringBuilder sb = new StringBuilder();
	        sb.append('[');
	        for (;;) {
	            E e = p.item;
	            sb.append(e == this ? "(this Collection)" : e);
	            p = p.next;
	            if (p == null)
	                return sb.append(']').toString();
	            sb.append(',').append(' ');
	        }
	    } finally {
	        fullyUnlock();
	    }
	}
	
	    
	public void clear() throws IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    fullyLock();
	    try {
	        for (Node<E> p, h = head; (p = h.next) != null; h = p) {
	            h.next = h;
	            p.item = null;
	        }
	        head = last;
	        if (count.getAndSet(0) == capacity)
	            notFull.signal();
	    } finally {
	        fullyUnlock();
	    }
	}
	
	public int drainTo(Collection<? super E> c) {
	    return drainTo(c, Integer.MAX_VALUE);
	}
	  
	public int drainTo(Collection<? super E> c, int maxElements) throws IllegalStateException {
		if (isWriteBlocked()) throw new IllegalStateException();
	    if (c == null)
	        throw new NullPointerException();
	    if (c == this)
	        throw new IllegalArgumentException();
	    if (maxElements <= 0)
	        return 0;
	    boolean signalNotFull = false;
	    final ReentrantLock takeLock = this.takeLock;
	    takeLock.lock();
	    try {
	        int n = Math.min(maxElements, count.get());
	        Node<E> h = head;
	        int i = 0;
	        try {
	            while (i < n) {
	                Node<E> p = h.next;
	                c.add(p.item);
	                p.item = null;
	                h.next = h;
	                h = p;
	                ++i;
	            }
	            return n;
	        } finally {
	            if (i > 0) {
	                head = h;
	                signalNotFull = (count.getAndAdd(-i) == capacity);
	            }
	        }
	    } finally {
	        takeLock.unlock();
	        if (signalNotFull)
	            signalNotFull();
	    }
	}
	
	public Iterator<E> iterator() {
	    return new Itr();
	}
	
	private class Itr implements Iterator<E> {
	    private Node<E> current;
	    private Node<E> lastRet;
	    private E currentElement;
	
	    Itr() {
	        fullyLock();
	        try {
	            current = head.next;
	            if (current != null)
	                currentElement = current.item;
	        } finally {
	            fullyUnlock();
	        }
	    }
	
	    public boolean hasNext() {
	        return current != null;
	    }
	
	        
		private Node<E> nextNode(Node<E> p) {
		    for (;;) {
		        Node<E> s = p.next;
		        if (s == p)
		            return head.next;
		        if (s == null || s.item != null)
		            return s;
		        p = s;
		    }
		}
		
		public E next() {
		    fullyLock();
		    try {
		        if (current == null)
		            throw new NoSuchElementException();
		        E x = currentElement;
		        lastRet = current;
		        current = nextNode(current);
		        currentElement = (current == null) ? null : current.item;
		        return x;
		    } finally {
		        fullyUnlock();
		    }
		}
		
		public void remove() {
			if (isWriteBlocked()) throw new IllegalStateException();
		    if (lastRet == null)
		        throw new IllegalStateException();
		    fullyLock();
		    try {
		        Node<E> node = lastRet;
		        lastRet = null;
		        for (Node<E> trail = head, p = trail.next;
		             p != null;
		             trail = p, p = p.next) {
		            if (p == node) {
		                unlink(p, trail);
		                break;
		            }
		        }
		    } finally {
		        fullyUnlock();
		    }
		}
	}
	
	static final class LBQSpliterator<E> implements Spliterator<E> {
	    static final int MAX_BATCH = 1 << 25;  // max batch array size;
	    final LinkedBlockingQueue<E> queue;
	    Node<E> current;    // current node; null until initialized
	    int batch;          // batch size for splits
	    boolean exhausted;  // true when no more nodes
	    long est;           // size estimate
	    LBQSpliterator(LinkedBlockingQueue<E> queue) {
	        this.queue = queue;
	        this.est = queue.size();
	    }
	
	    public long estimateSize() { return est; }
	
	    public Spliterator<E> trySplit() {
	        Node<E> h;
	        final LinkedBlockingQueue<E> q = this.queue;
	        int b = batch;
	        int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
	        if (!exhausted &&
	            ((h = current) != null || (h = q.head.next) != null) &&
	            h.next != null) {
	            Object[] a = new Object[n];
	            int i = 0;
	            Node<E> p = current;
	            q.fullyLock();
	            try {
	                if (p != null || (p = q.head.next) != null) {
	                    do {
	                        if ((a[i] = p.item) != null)
	                            ++i;
	                    } while ((p = p.next) != null && i < n);
	                }
	            } finally {
	                q.fullyUnlock();
	            }
	            if ((current = p) == null) {
	                est = 0L;
	                exhausted = true;
	            }
	            else if ((est -= i) < 0L)
	                est = 0L;
	            if (i > 0) {
	                batch = i;
	                return Spliterators.spliterator
	                    (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
	                     Spliterator.CONCURRENT);
	            }
	        }
	        return null;
	    }
	
	    public void forEachRemaining(Consumer<? super E> action) {
	        if (action == null) throw new NullPointerException();
	        final LinkedBlockingQueue<E> q = this.queue;
	        if (!exhausted) {
	            exhausted = true;
	            Node<E> p = current;
	            do {
	                E e = null;
	                q.fullyLock();
	                try {
	                    if (p == null)
	                        p = q.head.next;
	                    while (p != null) {
	                        e = p.item;
	                        p = p.next;
	                        if (e != null)
	                            break;
	                    }
	                } finally {
	                    q.fullyUnlock();
	                }
	                if (e != null)
	                    action.accept(e);
	            } while (p != null);
	        }
	    }
	
	    public boolean tryAdvance(Consumer<? super E> action) {
	        if (action == null) throw new NullPointerException();
	        final LinkedBlockingQueue<E> q = this.queue;
	        if (!exhausted) {
	            E e = null;
	            q.fullyLock();
	            try {
	                if (current == null)
	                    current = q.head.next;
	                while (current != null) {
	                    e = current.item;
	                    current = current.next;
	                    if (e != null)
	                        break;
	                }
	            } finally {
	                q.fullyUnlock();
	            }
	            if (current == null)
	                exhausted = true;
	            if (e != null) {
	                action.accept(e);
	                return true;
	            }
	        }
	        return false;
	    }
	
	    public int characteristics() {
	        return Spliterator.ORDERED | Spliterator.NONNULL |
	            Spliterator.CONCURRENT;
	    }
	}
	
	public Spliterator<E> spliterator() {
	    return new LBQSpliterator<E>(this);
	}
	
	private void writeObject(java.io.ObjectOutputStream s)
	    throws java.io.IOException {
	
	    fullyLock();
	    try {
	        // Write out any hidden stuff, plus capacity
	        s.defaultWriteObject();
	
	        // Write out all elements in the proper order.
	        for (Node<E> p = head.next; p != null; p = p.next)
	            s.writeObject(p.item);
	
	        // Use trailing null as sentinel
	        s.writeObject(null);
	    } finally {
	        fullyUnlock();
	    }
	}
	 
	private void readObject(java.io.ObjectInputStream s)
	    throws java.io.IOException, ClassNotFoundException {
	    // Read in capacity, and any hidden stuff
	    s.defaultReadObject();
	
	    count.set(0);
	    last = head = new Node<E>(null);
	
	    // Read in all elements and place in queue
	    for (;;) {
	        @SuppressWarnings("unchecked")
	        E item = (E)s.readObject();
	        if (item == null)
	            break;
	        add(item);
	    }
	}
}