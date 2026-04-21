package com.ft.budget.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertType {
    WARNING_50(50),
    WARNING_80(80),
    EXCEEDED_100(100);

    private final int threshold;
}
