package com.supplyforge.ai.infrastructure.excel;

/** Báo hiệu đã đọc đủ header + mẫu — dừng SAX sớm (unchecked). */
public final class PeekAbortException extends RuntimeException {

    public PeekAbortException() {
        super(null, null, false, false);
    }
}
