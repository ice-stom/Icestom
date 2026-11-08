package io.gitlab.icestom.icestom.stages.qualifying;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.ordering.Tower;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.List;

public class QualifyingTimingTower extends Tower<QualifyingStage.Qualifier> {
    public QualifyingTimingTower() {
        super(List.of(
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "position"), Component.text("Pos"), entry -> entry.getIndex() + 1),
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "time"), Component.text("Time"), entry -> entry.getItem().getQualifyingTime()),
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "name"), Component.text("Name"), entry -> entry.getItem().getName())
        ));
    }
}
