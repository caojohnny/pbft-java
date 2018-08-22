package com.gmail.woodyc40.pbft;

public enum MessageType {
    REQUEST,
    PRE_PREPARE,
    PREPARE,
    COMMIT,
    REPLY,

    CHECKPOINT,

    VIEW_CHANGE,
    NEW_VIEW
}
