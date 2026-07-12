package com.hust.orderservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotEmpty
    private List<String> courseIds;
    
    private String userId;
    
    private String note;
}
