package com.vitsync.catalog.api.mapper;

import com.vitsync.catalog.api.dto.FacilityResponse;
import com.vitsync.catalog.api.dto.SpecialtyResponse;
import com.vitsync.catalog.application.FacilityView;
import com.vitsync.catalog.application.SpecialtyView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    SpecialtyResponse toResponse(SpecialtyView view);

    FacilityResponse toResponse(FacilityView view);
}
