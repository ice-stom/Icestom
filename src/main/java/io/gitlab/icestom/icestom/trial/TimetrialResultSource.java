package io.gitlab.icestom.icestom.trial;

import java.util.List;

public interface TimetrialResultSource {
    List<TimeTrial.Split> getSplits();
}
