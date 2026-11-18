package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.service.dto.CreateMemberAccountCommand;
import com.afcgym.platform.service.dto.CreateMemberCommand;
import com.afcgym.platform.service.dto.RegisterMemberCredentialsCommand;
import com.afcgym.platform.service.dto.UpdateMemberCommand;
import java.util.List;

public interface IMemberService {

    MemberEntity create(CreateMemberCommand command);

    MemberEntity getById(Long id);

    List<MemberEntity> search(String search, MemberStatus status);

    MemberEntity update(Long id, UpdateMemberCommand command);

    MemberEntity deactivate(Long id);

    MemberEntity registerCredentials(Long id, RegisterMemberCredentialsCommand command);

    MemberEntity renew(Long id, MembershipType membershipType);

    UserEntity createAccount(Long id, CreateMemberAccountCommand command);
}
