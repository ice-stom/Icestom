package io.gitlab.icestom.icestom.ordering;

public interface TowerHolder<T extends ForwardOrdering.Orderable<T>> {
    Tower<T> getTower();
}
