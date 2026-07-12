package com.hust.courseservice.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionLogType {
    CREATE("Create", "Created"),
    UPDATE("Update", "Updated"),
    ACTIVATE("Activate", "Activated"),
    DEACTIVATE("Deactivate", "Deactivated"),
    ACCEPT("Accept", "Accepted"),
    REJECT("Reject", "Rejected"),
    CONFIRM("Confirm", "Confirmed"),
    DELETE("Delete", "Deleted");

    private final String code;
    private final String name;
}
