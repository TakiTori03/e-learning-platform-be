package com.hust.orderservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;

import com.hust.orderservice.dto.request.OrderRequest;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface OrderMapper extends BaseMapper<Order, OrderRequest, OrderResponse> {
    
    @Override
    @Mapping(target = "items", ignore = true)
    Order requestToEntity(OrderRequest request);

    @Override
    OrderResponse entityToResponse(Order entity);
}
