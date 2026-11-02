package io.gitlab.icestom.icestom.stages.race;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.ordering.Tower;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.List;

public class RaceTimingTower extends Tower<RaceStage.RaceParticipant> {
    public RaceTimingTower() {
        super(List.of(
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "position"), Component.text("Pos"), entry -> entry.getIndex() + 1),
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "delta"), Component.text("Delta"), entry -> entry.getIndex() == 0 ? null : entry.getItem().getDeltaToAhead()),
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "name"), Component.text("Name"), entry -> entry.getItem().getName())
        ));
    }
}
