package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.MemberMapper;
import com.afcgym.platform.mapper.UserMapper;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.service.IMemberService;
import com.afcgym.platform.service.dto.CreateMemberAccountCommand;
import com.afcgym.platform.service.dto.CreateMemberCommand;
import com.afcgym.platform.service.dto.RegisterMemberCredentialsCommand;
import com.afcgym.platform.service.dto.UpdateMemberCommand;
import com.afcgym.platform.web.dto.request.CreateMemberAccountRequest;
import com.afcgym.platform.web.dto.request.CreateMemberRequest;
import com.afcgym.platform.web.dto.request.RegisterMemberCredentialsRequest;
import com.afcgym.platform.web.dto.request.UpdateMemberRequest;
import com.afcgym.platform.web.dto.response.MemberResponse;
import com.afcgym.platform.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members")
@RequiredArgsConstructor
@Tag(name = "Members")
public class MembersController {

    private final IMemberService memberService;
    private final MemberMapper memberMapper;
    private final UserMapper userMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new member")
    public MemberResponse create(@Valid @RequestBody CreateMemberRequest request) {
        return memberMapper.toResponse(memberService.create(new CreateMemberCommand(
                request.fullName(),
                request.phone(),
                request.email(),
                request.membershipType())));
    }

    @GetMapping
    @Operation(summary = "Search members by name, code or phone, optionally filtered by status")
    public List<MemberResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MemberStatus status) {
        return memberService.search(search, status).stream().map(memberMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by id")
    public MemberResponse getById(@PathVariable Long id) {
        return memberMapper.toResponse(memberService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a member's contact data and membership type")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMemberRequest request) {
        return memberMapper.toResponse(memberService.update(id, new UpdateMemberCommand(
                request.fullName(),
                request.phone(),
                request.email(),
                request.membershipType())));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a member")
    public MemberResponse deactivate(@PathVariable Long id) {
        return memberMapper.toResponse(memberService.deactivate(id));
    }

    @PatchMapping("/{id}/credentials")
    @Operation(summary = "Register or update a member's PIN or fingerprint token")
    public MemberResponse registerCredentials(
            @PathVariable Long id,
            @Valid @RequestBody RegisterMemberCredentialsRequest request) {
        return memberMapper.toResponse(memberService.registerCredentials(id, new RegisterMemberCredentialsCommand(
                request.pin(),
                request.fingerprintToken())));
    }

    @PostMapping("/{id}/account")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the SOCIO login account of a member so they can book classes from the app")
    public UserResponse createAccount(@PathVariable Long id, @Valid @RequestBody CreateMemberAccountRequest request) {
        return userMapper.toResponse(memberService.createAccount(id, new CreateMemberAccountCommand(
                request.username(),
                request.email(),
                request.password())));
    }
}
