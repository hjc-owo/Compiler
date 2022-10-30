package utils;

import java.util.Iterator;

public class IList<N, L> implements Iterable<INode<N, L>> {
    private INode<N, L> begin;
    private INode<N, L> end;
    private final L value;
    private int size;

    public IList(L value) {
        this.begin = null;
        this.value = value;
        this.end = null;
        this.size = 0;
    }

    public INode<N, L> getBegin() {
        return begin;
    }

    public void setBegin(INode<N, L> begin) {
        this.begin = begin;
    }

    public INode<N, L> getEnd() {
        return end;
    }

    public void setEnd(INode<N, L> end) {
        this.end = end;
    }

    public L getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isEmpty() {
        return (this.getBegin() == null) && (this.getEnd() == null) && (getSize() == 0);
    }

    /**
     * 只改变size
     */
    public void addNode() {
        this.size++;
    }

    /**
     * 只改变size
     */
    public void removeNode() {
        this.size--;
    }

    @Override
    public Iterator<INode<N, L>> iterator() {
        return new ListIterator(this.getBegin());
    }

    class ListIterator implements Iterator<INode<N, L>> {
        INode<N, L> now = new INode<>(null);
        INode<N, L> next = null;

        public ListIterator(INode<N, L> head) {
            now.setNext(head);
        }

        @Override
        public boolean hasNext() {
            return next != null || now.getNext() != null;
        }

        @Override
        public INode<N, L> next() {
            if (next == null) {
                now = now.getNext();
            } else {
                now = next;
            }
            next = null;
            return now;
        }

        @Override
        public void remove() {
            INode<N, L> prev = now.getPrev();
            INode<N, L> next = now.getNext();
            IList<N, L> parent = now.getParent();
            if (prev != null) {
                prev.setNext(next);
            } else {
                parent.setBegin(next);
            }
            if (next != null) {
                next.setPrev(prev);
            } else {
                parent.setEnd(prev);
            }
            parent.removeNode();
            this.next = next;
            now.clear();
        }
    }
}
