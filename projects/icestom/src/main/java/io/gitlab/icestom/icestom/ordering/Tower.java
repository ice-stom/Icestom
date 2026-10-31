package io.gitlab.icestom.icestom.ordering;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Function;

public class Tower<R extends ForwardOrdering.Orderable<R>> extends ForwardOrdering<R> {

    private final List<TowerColumn<R, ?>> columns;
    private final WeakHashMap<R, Object[]> values = new WeakHashMap<>();

    public Tower(List<TowerColumn<R, ?>> columns) {
        this.columns = columns;
    }

    public Object[] getRow(ForwardOrdering<R>.Entry row) {
        return values.get(row.getItem());
    }

    public List<TowerColumn<R, ?>> getColumns() {
        return columns;
    }

    private void updateColumns(Entry entry) {
        int size = columns.size();

        Object[] row = new Object[size];

        int i = 0;
        for (TowerColumn<R, ?> column : columns) {
            row[i++] = column.update.apply(entry);
        }

        values.put(entry.getItem(), row);
    }
    @Override
    public ForwardOrdering<R>.Entry add(R item) {
        ForwardOrdering<R>.Entry entry = super.add(item);

        updateColumns(entry);

        return entry;
    }

    @Override
    public void update(ForwardOrdering<R>.Entry entry) {
        updateColumns(entry);
    }

    public record TowerColumn<R extends Orderable<R>, V>(
            net.kyori.adventure.key.Key identifier,
            Component name,
            Function<ForwardOrdering<R>.Entry, V> update
    ) {
        @Override
        public int hashCode() {
            return identifier.hashCode();
        }
    }
}
