package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.enums.CheckInMethod;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.service.ICheckInService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements ICheckInService {

    private static final String INVALID_CREDENTIAL_MESSAGE = "PIN o huella inválidos";

    private final IMemberRepository memberRepository;
    private final ICheckInRepository checkInRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CheckInEntity checkIn(Long memberId, String pin, String fingerprintToken) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("No se encontró el socio solicitado"));
        if (!member.isActive()) {
            throw new BadRequestException("El socio no está activo");
        }
        CheckInMethod method = resolveMethod(member, pin, fingerprintToken);
        CheckInEntity checkIn = CheckInEntity.builder()
                .memberId(member.getId())
                .method(method)
                .checkedInAt(Instant.now())
                .build();
        return checkInRepository.save(checkIn);
    }

    private CheckInMethod resolveMethod(MemberEntity member, String pin, String fingerprintToken) {
        boolean hasPin = pin != null && !pin.isBlank();
        boolean hasFingerprint = fingerprintToken != null && !fingerprintToken.isBlank();
        if (!hasPin && !hasFingerprint) {
            throw new BadRequestException("Se debe enviar un PIN o una huella para el check-in");
        }
        if (hasPin) {
            if (member.getPinHash() == null || !passwordEncoder.matches(pin, member.getPinHash())) {
                throw new BadRequestException(INVALID_CREDENTIAL_MESSAGE);
            }
            return CheckInMethod.PIN;
        }
        if (member.getFingerprintToken() == null || !passwordEncoder.matches(fingerprintToken, member.getFingerprintToken())) {
            throw new BadRequestException(INVALID_CREDENTIAL_MESSAGE);
        }
        return CheckInMethod.FINGERPRINT;
    }
}
