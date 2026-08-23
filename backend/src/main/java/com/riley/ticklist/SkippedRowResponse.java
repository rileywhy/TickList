package com.riley.ticklist;

public record SkippedRowResponse(
    long recordNumber,
    String reason,
    String rawRow
) {
    public static SkippedRowResponse fromEntity(SkippedRow skippedRow) {
        return new SkippedRowResponse(
            skippedRow.getRecordNumber(),
            skippedRow.getReason(),
            skippedRow.getRawRow()
        );
    }
}