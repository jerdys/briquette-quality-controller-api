package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BriquetteSide {
    TOP("СВЕРХУ"),
    BOTTOM("СНИЗУ"),
    LEFT("СЛЕВА"),
    RIGHT("СПРАВА"),
    FRONT("СПЕРЕДИ"),
    BACK("СЗАДИ");

    @Getter
    private final String russianNaming;
}
