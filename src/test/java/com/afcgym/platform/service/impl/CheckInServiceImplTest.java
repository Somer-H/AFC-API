package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.enums.CheckInMethod;
import com.afcgym.platform.persistance.repositories.ICheckInRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CheckInServiceImplTest {

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private ICheckInRepository checkInRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CheckInServiceImpl checkInService;

    private MemberEntity activeMember;

    @BeforeEach
    void init() {
        activeMember = MemberEntity.builder()
                .id(1L)
                .fullName("Ana Test")
                .pinHash("hashed-pin")
                .fingerprintToken("hashed-finger")
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void givenAValidPin_whenCheckIn_thenSaveACheckInWithMethodPin() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("1234", "hashed-pin")).thenReturn(true);
        CheckInEntity saved = CheckInEntity.builder()
                .id(1L)
                .memberId(1L)
                .method(CheckInMethod.PIN)
                .checkedInAt(Instant.now())
                .build();
        when(checkInRepository.save(any(CheckInEntity.class))).thenReturn(saved);

        CheckInEntity result = checkInService.checkIn(1L, "1234", null);

        assertEquals(CheckInMethod.PIN, result.getMethod());
    }

    @Test
    void givenAValidFingerprintToken_whenCheckIn_thenSaveACheckInWithMethodFingerprint() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("finger-token", "hashed-finger")).thenReturn(true);
        CheckInEntity saved = CheckInEntity.builder()
                .id(1L)
                .memberId(1L)
                .method(CheckInMethod.FINGERPRINT)
                .checkedInAt(Instant.now())
                .build();
        when(checkInRepository.save(any(CheckInEntity.class))).thenReturn(saved);

        CheckInEntity result = checkInService.checkIn(1L, null, "finger-token");

        assertEquals(CheckInMethod.FINGERPRINT, result.getMethod());
    }

    @Test
    void givenAnUnknownMember_whenCheckIn_thenThrowNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> checkInService.checkIn(99L, "1234", null));

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void givenAnInactiveMember_whenCheckIn_thenThrowBadRequestException() {
        MemberEntity inactiveMember = MemberEntity.builder()
                .id(2L)
                .fullName("Inactive Member")
                .pinHash("hashed-pin")
                .active(false)
                .createdAt(Instant.now())
                .build();
        when(memberRepository.findById(2L)).thenReturn(Optional.of(inactiveMember));

        assertThrows(BadRequestException.class, () -> checkInService.checkIn(2L, "1234", null));

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void givenNeitherPinNorFingerprint_whenCheckIn_thenThrowBadRequestException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));

        assertThrows(BadRequestException.class, () -> checkInService.checkIn(1L, null, null));

        verify(checkInRepository, never()).save(any());
    }

    @Test
    void givenAWrongPin_whenCheckIn_thenThrowBadRequestException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("0000", "hashed-pin")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> checkInService.checkIn(1L, "0000", null));

        verify(checkInRepository, never()).save(any());
    }
}
