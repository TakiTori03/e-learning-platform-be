package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.WishlistRequest;
import com.hust.interactionservice.dto.response.WishlistResponse;
import com.hust.interactionservice.entity.Wishlist;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface WishlistMapper extends BaseMapper<Wishlist, WishlistRequest, WishlistResponse> {
}
