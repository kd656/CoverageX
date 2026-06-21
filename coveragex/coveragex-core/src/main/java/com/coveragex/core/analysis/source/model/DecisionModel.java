package com.coveragex.core.analysis.source.model;

import java.util.List;

public record DecisionModel(
        int decisionId,
        String kind,             // IF / WHILE / FOR / DO / TERNARY / ASSERT
        Range decisionRange,     // full statement range
        Range conditionRange,    // expression range
        List<OperandModel> operands,
        List<Object> expressionRpn // e.g. [0, 1, "AND"] or [0,1,2,"OR","AND"]
) {}
