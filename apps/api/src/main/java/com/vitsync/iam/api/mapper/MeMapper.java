package com.vitsync.iam.api.mapper;

import com.vitsync.iam.api.dto.MeResponse;
import com.vitsync.iam.api.dto.SessionResponse;
import com.vitsync.iam.application.MeView;
import com.vitsync.iam.application.Session;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeMapper {

    MeResponse toResponse(MeView view);

    // El refresh token no se mapea: va a la cookie, nunca al cuerpo.
    SessionResponse toResponse(Session session);
}
