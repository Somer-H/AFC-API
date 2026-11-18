package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.afcgym.platform.service.dto.CreateMemberAccountCommand;
import com.afcgym.platform.service.dto.CreateMemberCommand;
import com.afcgym.platform.service.dto.RegisterMemberCredentialsCommand;
import com.afcgym.platform.service.dto.UpdateMemberCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    private MemberEntity existingMember;

    @BeforeEach
    void init() {
        existingMember = createMemberDummy();
    }

    @Test
    void givenAValidCommand_whenCreate_thenAssignAGeneratedCodeAndSave() {
        CreateMemberCommand command = new CreateMemberCommand("Ana Test", "5512345678", null, MembershipType.MONTHLY);
        when(memberRepository.save(any(MemberEntity.class))).thenReturn(existingMember, existingMember);

        MemberEntity result = memberService.create(command);

        verify(memberRepository, Mockito.times(2)).save(any(MemberEntity.class));
        assertEquals(existingMember, result);
    }

    @Test
    void givenAnExistingId_whenGetById_thenReturnTheMember() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));

        MemberEntity result = memberService.getById(1L);

        assertEquals(existingMember, result);
    }

    @Test
    void givenAnUnknownId_whenGetById_thenThrowNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> memberService.getById(99L));
    }

    @Test
    void givenNoFilters_whenSearch_thenReturnEverythingFromTheRepository() {
        when(memberRepository.findAll(any(Specification.class))).thenReturn(List.of(existingMember));

        List<MemberEntity> result = memberService.search(null, null);

        assertEquals(1, result.size());
    }

    @Test
    void givenAStatusFilter_whenSearch_thenKeepOnlyMatchingMembers() {
        MemberEntity expiredMember = createMemberDummy();
        when(memberRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(existingMember, expiredMember));

        List<MemberEntity> result = memberService.search(null, MemberStatus.ACTIVE);

        assertEquals(2, result.size());
    }

    @Test
    void givenAnExistingMember_whenUpdate_thenChangeItsProfile() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        UpdateMemberCommand command = new UpdateMemberCommand("Ana Updated", "5500000000", "ana@afcgym.com", MembershipType.QUARTERLY);

        MemberEntity result = memberService.update(1L, command);

        assertEquals("Ana Updated", result.getFullName());
        assertEquals(MembershipType.QUARTERLY, result.getMembershipType());
    }

    @Test
    void givenAnExistingMember_whenDeactivate_thenItBecomesInactive() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));

        MemberEntity result = memberService.deactivate(1L);

        assertFalse(result.isActive());
    }

    @Test
    void givenAnExpiredMembership_whenRenew_thenExtendFromToday() {
        MemberEntity expiredMember = MemberEntity.builder()
                .id(1L)
                .code("SOC-000001")
                .fullName("Ana Test")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(LocalDate.now().minusDays(10))
                .active(true)
                .createdAt(Instant.now())
                .build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(expiredMember));

        MemberEntity result = memberService.renew(1L, MembershipType.YEARLY);

        assertEquals(LocalDate.now().plusDays(365), result.getDueDate());
        assertEquals(MembershipType.YEARLY, result.getMembershipType());
    }

    @Test
    void givenAnActiveMembership_whenRenew_thenExtendFromTheCurrentDueDate() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        LocalDate expectedDueDate = existingMember.getDueDate().plusDays(MembershipType.MONTHLY.getDurationDays());

        MemberEntity result = memberService.renew(1L, MembershipType.MONTHLY);

        assertEquals(expectedDueDate, result.getDueDate());
    }

    @Test
    void givenAPin_whenRegisterCredentials_thenHashAndSaveIt() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        when(passwordEncoder.encode("4321")).thenReturn("hashed-new-pin");

        MemberEntity result = memberService.registerCredentials(1L, new RegisterMemberCredentialsCommand("4321", null));

        assertEquals("hashed-new-pin", result.getPinHash());
    }

    @Test
    void givenNeitherPinNorFingerprint_whenRegisterCredentials_thenThrowBadRequestException() {
        RegisterMemberCredentialsCommand command = new RegisterMemberCredentialsCommand(null, null);

        assertThrows(BadRequestException.class, () -> memberService.registerCredentials(1L, command));

        verify(memberRepository, never()).findById(any());
    }

    @Test
    void givenAMemberWithoutAccount_whenCreateAccount_thenSaveASocioUserAndLinkIt() {
        CreateMemberAccountCommand command = new CreateMemberAccountCommand("ana", "ana@afcgym.com", "Secret-123");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        when(userRepository.existsByUsername("ana")).thenReturn(false);
        when(userRepository.existsByEmail("ana@afcgym.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret-123")).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        UserEntity result = memberService.createAccount(1L, command);

        assertEquals(Role.SOCIO, result.getRole());
        assertEquals(existingMember.getFullName(), result.getFullName());
        assertEquals("hashed-password", result.getPasswordHash());
        assertEquals(50L, existingMember.getUserId());
    }

    @Test
    void givenAMemberThatAlreadyHasAnAccount_whenCreateAccount_thenThrowConflictException() {
        existingMember.linkAccount(50L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));

        assertThrows(ConflictException.class, () -> memberService.createAccount(
                1L, new CreateMemberAccountCommand("ana", "ana@afcgym.com", "Secret-123")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenATakenUsername_whenCreateAccount_thenThrowConflictException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(ConflictException.class, () -> memberService.createAccount(
                1L, new CreateMemberAccountCommand("admin", "ana@afcgym.com", "Secret-123")));
    }

    @Test
    void givenAnInactiveMember_whenCreateAccount_thenThrowBadRequestException() {
        existingMember.deactivate();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));

        assertThrows(BadRequestException.class, () -> memberService.createAccount(
                1L, new CreateMemberAccountCommand("ana", "ana@afcgym.com", "Secret-123")));
    }

    private UserEntity withId(UserEntity user) {
        return UserEntity.builder()
                .id(50L)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private MemberEntity createMemberDummy() {
        return MemberEntity.builder()
                .id(1L)
                .code("SOC-000001")
                .fullName("Ana Test")
                .phone("5512345678")
                .membershipType(MembershipType.MONTHLY)
                .dueDate(LocalDate.now().plusDays(30))
                .pinHash("hashed-pin")
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
