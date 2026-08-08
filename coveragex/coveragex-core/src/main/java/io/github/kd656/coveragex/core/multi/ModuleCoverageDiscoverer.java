package io.github.kd656.coveragex.core.multi;

import java.util.List;

/**
 * Strategy for discovering modules in whichever build is driving the current
 * aggregation.
 */
public interface ModuleCoverageDiscoverer {

    List<ModuleCoverageDescriptor> discover();
}
