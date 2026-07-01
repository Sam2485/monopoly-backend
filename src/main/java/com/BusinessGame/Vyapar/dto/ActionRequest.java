package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.ActionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {
    private ActionType action;
    private Integer propertyId;
}
