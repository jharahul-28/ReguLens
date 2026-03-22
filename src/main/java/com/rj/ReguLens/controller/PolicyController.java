package com.rj.ReguLens.controller;

import com.rj.ReguLens.dto.policy.PolicyCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCreationResponseDto;
import com.rj.ReguLens.dto.policy.PolicyUpdationRequestDto;
import com.rj.ReguLens.service.PolicyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/policy")
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    PolicyService policyService;

    @PostMapping
    public ResponseEntity<PolicyCreationResponseDto> createPolicy(@RequestBody PolicyCreationRequestDto policyCreationRequest) throws BadRequestException {
        log.info("Creating policy");
        return policyService.createPolicy(policyCreationRequest);
    }

    @PatchMapping("/update")
    public ResponseEntity<PolicyCreationResponseDto> updatePolicy(@RequestBody PolicyUpdationRequestDto policyUpdationRequest) throws BadRequestException {
        log.info("Updating policy");
        return policyService.updatePolicy(policyUpdationRequest);
    }
}
