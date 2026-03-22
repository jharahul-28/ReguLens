package com.rj.ReguLens.service.impl;

import com.rj.ReguLens.dto.*;
import com.rj.ReguLens.dto.policy.PolicyResponse;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionCreationRequestDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionResponseDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionUpdationRequestDto;
import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyVersion;
import com.rj.ReguLens.entity.PolicyVersionStatusEnum;
import com.rj.ReguLens.entity.User;
import com.rj.ReguLens.exception.InvalidPolicyState;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.mapper.PolicyMapper;
import com.rj.ReguLens.mapper.PolicyVersionMapper;
import com.rj.ReguLens.repository.PolicyRepository;
import com.rj.ReguLens.repository.PolicyVersionRepository;
import com.rj.ReguLens.repository.UserRepository;
import com.rj.ReguLens.service.PolicyVersionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.fromString;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PolicyVersionServiceImpl implements PolicyVersionService {

    PolicyRepository policyRepository;
    PolicyVersionRepository policyVersionRepository;
    PolicyMapper policyMapper;
    PolicyVersionMapper policyVersionMapper;
    UserRepository userRepository;

    @Transactional
    @Override
    public ResponseEntity<PolicyWithPolicyVersion> addPolicyVersion(UUID policyId, PolicyVersionCreationRequestDto policyVersionCreationRequestDto) throws BadRequestException {
        Policy policy= policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFound("Policy not found with id: " + policyId));
        if(policyVersionCreationRequestDto.content() == null || policyVersionCreationRequestDto.content().isEmpty()) {
            throw new BadRequestException("Content is invalid");
        }
        if(policyVersionCreationRequestDto.effectiveFrom().isAfter(policyVersionCreationRequestDto.effectiveTo())) {
            throw new BadRequestException("Effective dates are invalid");
        }
        if(policyVersionCreationRequestDto.effectiveTo().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Effective to date is invalid");
        }
        int nextVersion = policyVersionRepository
                .findTopByPolicyIdOrderByVersionDesc(policyId)
                .map(pv -> pv.getVersion() + 1)
                .orElse(1);
        PolicyVersion policyVersion= PolicyVersion.builder()
                .policy(policy)
                .version(nextVersion)
                .content(policyVersionCreationRequestDto.content())
                .approvedBy(null)
                .status(PolicyVersionStatusEnum.DRAFT)
                .createdAt(LocalDateTime.now())
                .effectiveFrom(policyVersionCreationRequestDto.effectiveFrom())
                .effectiveTo(policyVersionCreationRequestDto.effectiveTo())
                .build();
        PolicyVersion savedPolicyVersion= policyVersionRepository.save(policyVersion);
        Set<PolicyVersion> policyVersions= policy.getPolicyVersions();
        policyVersions.add(savedPolicyVersion);
        policy.setPolicyVersions(policyVersions);
        policyRepository.save(policy);
        Set<PolicyVersionResponseDto> policyVersionResponseDto=
                policyVersionMapper.policyVersionSetToPolicyVersionResponseDto(policyVersions);
        PolicyResponse policyResponse= policyMapper.policyToPolicyResponse(policy);
        PolicyWithPolicyVersion policyWithPolicyVersion= new PolicyWithPolicyVersion(
                policyResponse,
                policyVersionResponseDto
        );
        return new ResponseEntity<>(policyWithPolicyVersion, HttpStatus.CREATED);
    }

    @Transactional
    @Override
    public ResponseEntity<PolicyVersionResponseDto> updatePolicyVersion(UUID id, PolicyVersionUpdationRequestDto policyVersionUpdationRequestDto) throws BadRequestException {
        PolicyVersion policyVersion = policyVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Policy Version not found with id: "+ id));
        if (!policyVersion.getStatus().equals(PolicyVersionStatusEnum.DRAFT)) {
            throw new InvalidPolicyState("Only draft policy version can be updated");
        };
        Integer version = policyVersion.getVersion();
        if(policyVersionUpdationRequestDto.content() != null && !policyVersionUpdationRequestDto.content().isEmpty()) {
            policyVersion.setContent(policyVersionUpdationRequestDto.content());
        }
        if(policyVersionUpdationRequestDto.effectiveFrom() != null)
            policyVersion.setEffectiveFrom(policyVersionUpdationRequestDto.effectiveFrom());
        if(policyVersionUpdationRequestDto.effectiveTo() != null &&
                (policyVersionUpdationRequestDto.effectiveTo().isBefore(LocalDateTime.now())
                        || policyVersionUpdationRequestDto.effectiveTo().isBefore(policyVersion.getEffectiveFrom()))) {
            throw new BadRequestException("Effective dates are invalid");
        }
        if(policyVersionUpdationRequestDto.effectiveTo() != null && policyVersionUpdationRequestDto.effectiveTo().isAfter(LocalDateTime.now()))
            policyVersion.setEffectiveTo(policyVersionUpdationRequestDto.effectiveTo());
        PolicyVersionResponseDto updatedPoliceVersion = policyVersionMapper.policyVersionToPolicyVersionResponseDto(policyVersion);
        return ResponseEntity.ok().body(updatedPoliceVersion);
    }

    @Transactional
    @Override
    public ResponseEntity<String> approvePolicyVersion(UUID id) {
        PolicyVersion policyVersion = policyVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Policy Version not found with id: "+ id));
        if(policyVersion.getApprovedBy() != null)
            return new ResponseEntity<>("Policy version is already approved", HttpStatus.CONFLICT);
        User approvedBy= userRepository.findById(fromString("dbf4f3f4-6084-48bc-bef5-a54721b0fce5")).orElseThrow(() -> new ResourceNotFound("User not found"));
        if(policyVersion.getEffectiveTo().isBefore(LocalDateTime.now()))
            return new ResponseEntity<>("Policy Version has expired", HttpStatus.CONFLICT);
        policyVersion.setApprovedBy(approvedBy);
        policyVersion.setStatus(PolicyVersionStatusEnum.APPROVED);
        policyVersionRepository.save(policyVersion);
        return ResponseEntity.ok().body("Approved");
    }

    @Transactional
    @Override
    public ResponseEntity<String> activatePolicyVersion(UUID id) {
        PolicyVersion policyVersion = policyVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Policy Version not found with id: "+ id));
        if(policyVersion.getApprovedBy() == null)
            return new ResponseEntity<>("Policy version is not approved", HttpStatus.CONFLICT);
        //Expire active policy versions
        UUID policyId= policyVersion.getPolicy().getId();
        Set<PolicyVersion> policyVersionSet= policyVersionRepository.findByPolicyIdAndStatus(policyId, PolicyVersionStatusEnum.ACTIVE);
        policyVersionSet.forEach(version->{
            version.setStatus(PolicyVersionStatusEnum.EXPIRED);
        });
        policyVersion.setStatus(PolicyVersionStatusEnum.ACTIVE);
        policyVersionRepository.save(policyVersion);
        return ResponseEntity.ok().body("Policy version has been activated");
    }

    @Override
    public PolicyVersionResponseDto getPolicyVersionById(UUID policyId) {
        PolicyVersion policyVersion= policyVersionRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFound("Policy Version not found with id: "+ policyId));
        return policyVersionMapper.policyVersionToPolicyVersionResponseDto(policyVersion);
    }
}
