package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.web.dto.response.MemberResponse;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "status", source = "dueDate", qualifiedByName = "toStatus")
    MemberResponse toResponse(MemberEntity entity);

    @Named("toStatus")
    default String toStatus(LocalDate dueDate) {
        return MemberStatus.fromDueDate(dueDate).name();
    }
}
