package com.rj.ReguLens.controller;

import com.rj.ReguLens.dto.PolicyWithPolicyVersion;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionCreationRequestDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionResponseDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionUpdationRequestDto;
import com.rj.ReguLens.exception.BadRequestException;
import com.rj.ReguLens.service.PolicyVersionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/policyversion")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PolicyVersionController {

    PolicyVersionService policyVersionService;

    @GetMapping("/{id}")
    public ResponseEntity<PolicyVersionResponseDto> getPolicyVersionById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(policyVersionService.getPolicyVersionById(id));
    }

    @PostMapping("/{policyId}")
    public ResponseEntity<PolicyWithPolicyVersion> addPolicyVersion(
            @PathVariable UUID policyId,
            @RequestBody PolicyVersionCreationRequestDto policyVersionCreationRequestDto
    ) throws BadRequestException {
        return policyVersionService.addPolicyVersion(policyId, policyVersionCreationRequestDto);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<PolicyVersionResponseDto> updatePolicyVersion(
            @PathVariable UUID id,
            @RequestBody PolicyVersionUpdationRequestDto policyVersionUpdationRequestDto
    ) throws BadRequestException {
        return policyVersionService.updatePolicyVersion(id, policyVersionUpdationRequestDto);
    }

    @PatchMapping("/approve/{id}")
    public ResponseEntity<String> approvePolicyVersion(@PathVariable UUID id) {
        return policyVersionService.approvePolicyVersion(id);
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<String> activatePolicyVersion(@PathVariable UUID id) {
        return policyVersionService.activatePolicyVersion(id);
    }
}
