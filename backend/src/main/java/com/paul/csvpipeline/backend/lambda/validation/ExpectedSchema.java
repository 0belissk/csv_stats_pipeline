package com.paul.csvpipeline.backend.lambda.validation;

import java.util.List;

public final class ExpectedSchema {

    private ExpectedSchema() {}

    public static List<ColumnDefinition> defaultSchema() {
        return List.of(
                new ColumnDefinition("id", ColumnType.INTEGER),
                new ColumnDefinition("name", ColumnType.STRING),
                new ColumnDefinition("email", ColumnType.EMAIL),
                new ColumnDefinition("amount", ColumnType.DECIMAL)
        );
    }
}
