package io.gitlab.icestom.icestom.timetrial;

import java.util.List;

public interface TimetrialResultSource {
    List<TimeTrial.Split> getSplits();

    default long getTime() {
        return getSplits().getLast().ms();
    }
}
