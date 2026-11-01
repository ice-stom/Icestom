package io.gitlab.icestom.icestom.ordering;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class ForwardOrdering<T extends ForwardOrdering.Orderable<T>> implements Iterable<ForwardOrdering<T>.Entry> {

    private @Nullable Entry head;
    private @Nullable Entry tail;
    private int size;

    private final ArrayList<Entry> indexLookup = new ArrayList<>();

    public Entry add(T item) {
        Entry entry = new Entry(item);
        entry.owner = this;

        Entry previous = tail;
        Entry next = null;
        while (previous != null && item.betterThan(previous.item)) {
            next = previous;
            previous = previous.previous;
        }

        entry.previous = previous;
        entry.next = next;
        size++;

        if (previous == null) head = entry;
        else previous.next = entry;

        if (next == null) tail = entry;
        else next.previous = entry;

        indexInserted(entry);

        return entry;
    }

    public void update(Entry entry) {
        T item = entry.item;

        Entry target = entry.previous;
        while (target != null && item.betterThan(target.item)) {
            target = target.previous;
        }

        if (target == entry.previous) {
            return;
        }

        Entry firstShifted = target == null ? head : target.next;
        invalidateIndexFrom(firstShifted);

        Entry oldNext = entry.next;

        Entry oldPrevious = entry.previous;
        oldPrevious.next = oldNext;
        if (oldNext == null) tail = oldPrevious;
        else oldNext.previous = oldPrevious;

        Entry newNext = target == null ? head : target.next;
        entry.previous = target;
        entry.next = newNext;
        if (target == null) head = entry;
        else target.next = entry;
        newNext.previous = entry;
    }

    public void remove(Entry entry) {
        invalidateIndexFrom(entry);

        Entry previous = entry.previous;
        Entry next = entry.next;

        if (previous == null) head = next;
        else previous.next = next;

        if (next == null) tail = previous;
        else next.previous = previous;

        entry.previous = null;
        entry.next = null;
        entry.owner = null;
        entry.index = -1;
        size--;
    }

    public void clear() {
        Entry current = head;
        while (current != null) {
            Entry next = current.next;
            current.previous = null;
            current.next = null;
            current.owner = null;
            current.index = -1;
            current = next;
        }
        head = null;
        tail = null;
        size = 0;
        indexLookup.clear();
    }

    public int indexOf(Entry entry) {
        if (entry.owner != this) {
            throw new IllegalArgumentException("Entry is not part of this ordering");
        }

        int index = cachedIndexOf(entry);
        if (index >= 0) return index;

        extendIndexThrough(entry);
        return entry.index;
    }

    private int cachedIndexOf(Entry entry) {
        int index = entry.index;
        return index >= 0 && index < indexLookup.size() && indexLookup.get(index) == entry ? index : -1;
    }

    private void extendIndexThrough(Entry target) {
        indexLookup.ensureCapacity(size);

        Entry cursor = indexLookup.isEmpty() ? head : indexLookup.getLast().next;
        while (cursor != null) {
            cursor.index = indexLookup.size();
            indexLookup.add(cursor);
            if (cursor == target) return;
            cursor = cursor.next;
        }

        throw new IllegalStateException("Entry is not reachable from head");
    }

    private void truncateIndex(int newSize) {
        if (newSize < indexLookup.size()) {
            indexLookup.subList(newSize, indexLookup.size()).clear();
        }
    }

    private void invalidateIndexFrom(Entry entry) {
        int index = cachedIndexOf(entry);
        if (index >= 0) truncateIndex(index);
    }

    private void indexInserted(Entry entry) {
        int position;
        if (entry.previous == null) {
            position = 0;
        } else {
            int previousIndex = cachedIndexOf(entry.previous);
            if (previousIndex < 0) return;
            position = previousIndex + 1;
        }

        if (position < indexLookup.size()) {
            truncateIndex(position);
        } else if (entry.next == null) {
            entry.index = indexLookup.size();
            indexLookup.add(entry);
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public @Nullable Entry getHead() {
        return head;
    }

    public @Nullable Entry getTail() {
        return tail;
    }

    public @NonNull Iterator<Entry> iteratorFrom(Entry from) {
        return new Iterator<>() {
            private @Nullable Entry pointer = from;

            @Override
            public boolean hasNext() {
                return pointer != null;
            }

            @Override
            public Entry next() {
                Entry current = pointer;
                if (current == null) throw new NoSuchElementException();

                pointer = current.next;

                return current;
            }
        };
    }

    @Override
    public @NonNull Iterator<Entry> iterator() {
        return iteratorFrom(head);
    }

    public class Entry {
        private final T item;

        private @Nullable Entry next;
        private @Nullable Entry previous;

        private @Nullable ForwardOrdering<T> owner;

        private int index = -1;

        private Entry(T item) {
            this.item = item;
        }

        public T getItem() {
            return item;
        }

        public @Nullable Entry getNext() {
            return next;
        }

        public @Nullable Entry getPrevious() {
            return previous;
        }

        public int getIndex() {
            ForwardOrdering<T> owner = this.owner;
            return owner == null ? -1 : owner.indexOf(this);
        }
    }

    public interface Orderable<T extends Orderable<T>> {
        boolean betterThan(T other);
    }
}