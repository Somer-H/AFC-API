package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.persistance.repositories.specifications.MemberSpecifications;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.dto.CreateMemberAccountCommand;
import com.afcgym.platform.service.dto.CreateMemberCommand;
import com.afcgym.platform.service.dto.RegisterMemberCredentialsCommand;
import com.afcgym.platform.service.dto.UpdateMemberCommand;
import com.afcgym.platform.utils.SecureTokenGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements IMemberService {

    private static final String CODE_PREFIX = "SOC-";
    private static final String TEMP_CODE_PREFIX = "TMP-";

    private final IMemberRepository memberRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberEntity create(CreateMemberCommand command) {
        LocalDate dueDate = LocalDate.now().plusDays(command.membershipType().getDurationDays());
        MemberEntity member = MemberEntity.builder()
                .code(TEMP_CODE_PREFIX + SecureTokenGenerator.generate(8))
                .fullName(command.fullName())
                .phone(command.phone())
                .email(command.email())
                .membershipType(command.membershipType())
                .dueDate(dueDate)
                .active(true)
                .createdAt(Instant.now())
                .build();
        member = memberRepository.save(member);
        member.assignCode(CODE_PREFIX + String.format("%06d", member.getId()));
        return memberRepository.save(member);
    }

    @Override
    public MemberEntity getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el socio solicitado"));
    }

    @Override
    public List<MemberEntity> search(String search, MemberStatus status) {
        Specification<MemberEntity> specification = (search == null || search.isBlank())
                ? MemberSpecifications.all()
                : MemberSpecifications.searchMatches(search);
        List<MemberEntity> members = memberRepository.findAll(specification);
        if (status == null) {
            return members;
        }
        return members.stream()
                .filter(member -> MemberStatus.fromDueDate(member.getDueDate()) == status)
                .toList();
    }

    @Override
    @Transactional
    public MemberEntity update(Long id, UpdateMemberCommand command) {
        MemberEntity member = getById(id);
        member.updateProfile(command.fullName(), command.phone(), command.email(), command.membershipType());
        return member;
    }

    @Override
    @Transactional
    public MemberEntity deactivate(Long id) {
        MemberEntity member = getById(id);
        member.deactivate();
        if (member.getUserId() != null) {
            userRepository.findById(member.getUserId()).ifPresent(UserEntity::deactivate);
        }
        return member;
    }

    @Override
    @Transactional
    public MemberEntity registerCredentials(Long id, RegisterMemberCredentialsCommand command) {
        boolean hasPin = command.pin() != null && !command.pin().isBlank();
        boolean hasFingerprint = command.fingerprintToken() != null && !command.fingerprintToken().isBlank();
        if (!hasPin && !hasFingerprint) {
            throw new BadRequestException("Se debe enviar un PIN o una huella");
        }
        MemberEntity member = getById(id);
        member.updateCredentials(
                hasPin ? passwordEncoder.encode(command.pin()) : null,
                hasFingerprint ? passwordEncoder.encode(command.fingerprintToken()) : null);
        return member;
    }

    @Override
    @Transactional
    public MemberEntity renew(Long id, MembershipType membershipType) {
        MemberEntity member = getById(id);
        LocalDate base = member.getDueDate().isAfter(LocalDate.now()) ? member.getDueDate() : LocalDate.now();
        member.renew(membershipType, base.plusDays(membershipType.getDurationDays()));
        return member;
    }

    @Override
    @Transactional
    public UserEntity createAccount(Long id, CreateMemberAccountCommand command) {
        MemberEntity member = getById(id);
        if (!member.isActive()) {
            throw new BadRequestException("El socio no está activo");
        }
        if (member.getUserId() != null) {
            throw new ConflictException("El socio ya tiene una cuenta de acceso");
        }
        if (userRepository.existsByUsername(command.username())) {
            throw new ConflictException("Ya existe una cuenta con ese usuario");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
        UserEntity user = userRepository.save(UserEntity.builder()
                .username(command.username())
                .fullName(member.getFullName())
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .role(Role.SOCIO)
                .active(true)
                .createdAt(Instant.now())
                .build());
        member.linkAccount(user.getId());
        return user;
    }
}
