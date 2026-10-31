package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.IceStom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.List;

public class RaceTimingTower extends io.gitlab.icestom.icestom.ordering.Tower<RaceStage.RaceParticipant> {
    public RaceTimingTower() {
        super(List.of(
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "name"), Component.text("Name"), entry -> entry.getItem().getName()),
                new TowerColumn<>(Key.key(IceStom.NAMESPACE, "delta"), Component.text("Delta"), Entry::getDistance)
        ));
    }
}
