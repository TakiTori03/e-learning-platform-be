package com.hust.orderservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.orderservice.dto.response.PaymentResponse;
import com.hust.orderservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface PaymentMapper extends BaseMapper<Payment, Object, PaymentResponse> {

    @Override
    @Mapping(source = "order.id", target = "orderId")
    PaymentResponse entityToResponse(Payment entity);
}
