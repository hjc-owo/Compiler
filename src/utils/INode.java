package utils;

public class INode<N, L> {
    // N - INode
    // L - IList
    private INode<N, L> prev = null;
    private INode<N, L> next = null;

    private N value;
    private IList<N, L> parent = null;

    public INode(N value) {
        this.value = value;
    }

    public INode(N value, IList<N, L> parent) {
        this.value = value;
        this.parent = parent;
    }

    public INode<N, L> getPrev() {
        return prev;
    }

    public void setPrev(INode<N, L> prev) {
        this.prev = prev;
    }

    public INode<N, L> getNext() {
        return next;
    }

    public void setNext(INode<N, L> next) {
        this.next = next;
    }

    public N getValue() {
        return value;
    }

    public IList<N, L> getParent() {
        return parent;
    }

    public void setParent(IList<N, L> parent) {
        this.parent = parent;
    }

    public void insertBefore(INode<N, L> node) {
        this.next = node;
        this.prev = node.prev;
        node.prev = this;
        if (this.prev != null) {
            this.prev.next = this;
        }
        this.parent = node.parent;
        this.parent.addNode();
        if (this.parent.getBegin() == node) {
            this.parent.setBegin(this);
        }
    }

    public void insertAfter(INode<N, L> node) {
        this.prev = node;
        this.next = node.next;
        node.next = this;
        if (this.next != null) {
            this.next.setPrev(this);
        }
        this.parent = node.getParent();
        this.parent.addNode();
        if (this.parent.getEnd() == node) {
            this.parent.setEnd(this);
        }
    }

    public void insertAtBegin(IList<N, L> parent) {
        this.parent = parent;
        if (parent.isEmpty()) {
            parent.setBegin(this);
            parent.setEnd(this);
            parent.addNode();
        } else {
            insertBefore(parent.getBegin());
        }
    }

    public void insertAtEnd(IList<N, L> parent) {
        this.parent = parent;
        if (parent.isEmpty()) {
            parent.setBegin(this);
            parent.setEnd(this);
            parent.addNode();
        } else {
            insertAfter(parent.getEnd());
        }
    }

    public INode<N, L> removeFromList() {
        parent.removeNode();
        if (parent.getBegin() == this) {
            this.parent.setBegin(this.next);
        }
        if (parent.getEnd() == this) {
            this.parent.setEnd(this.prev);
        }
        if (this.prev != null) {
            this.prev.setNext(this.next);
        }
        if (this.next != null) {
            this.next.setPrev(this.prev);
        }
        clear();
        return this;
    }

    public void clear() {
        this.prev = null;
        this.next = null;
        this.parent = null;
    }

}
