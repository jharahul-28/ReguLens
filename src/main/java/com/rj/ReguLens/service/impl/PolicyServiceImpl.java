package com.rj.ReguLens.service.impl;

import com.rj.ReguLens.dto.policy.PolicyCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCreationResponseDto;
import com.rj.ReguLens.dto.policy.PolicyUpdationRequestDto;
import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyCategory;
import com.rj.ReguLens.entity.User;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.mapper.PolicyMapper;
import com.rj.ReguLens.repository.PolicyCategoryRepository;
import com.rj.ReguLens.repository.PolicyRepository;
import com.rj.ReguLens.repository.UserRepository;
import com.rj.ReguLens.service.PolicyService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static java.util.UUID.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PolicyServiceImpl implements PolicyService {

    PolicyRepository policyRepository;
    PolicyCategoryRepository policyCategoryRepository;
    UserRepository userRepository;
    PolicyMapper policyMapper;

    @Override
    public ResponseEntity<PolicyCreationResponseDto> createPolicy(PolicyCreationRequestDto policyCreationRequest) throws BadRequestException {
        String title= policyCreationRequest.title();
        String description= policyCreationRequest.description();
        Set<String> policyCategories= policyCreationRequest.categories();
        if(title==null || title.isEmpty()){
            throw new BadRequestException("Title is invalid");
        }
        if(description==null || description.isEmpty()){
            throw new BadRequestException("Description is invalid");
        }
        if(policyRepository.existsByTitle(title)){
            throw new BadRequestException("Title already exists");
        }
        Set<PolicyCategory> categories= new HashSet<>();
        for(String category: policyCategories){
            categories.add(policyCategoryRepository.findByName(category));
        }
        User owner= userRepository.findById(fromString("dbf4f3f4-6084-48bc-bef5-a54721b0fce5")).orElseThrow( () -> new ResourceNotFound("User not found"));
        Policy policy= Policy.builder()
                .title(title)
                .description(description)
                .categories(categories)
                .createdAt(LocalDateTime.now())
                .policyVersions(null)
                .owner(owner)
                .active(false)
                .build();
        Policy savedPolicy= policyRepository.save(policy);
        return new ResponseEntity<>(policyMapper.policyToPolicyCreationResponseDto(savedPolicy), HttpStatus.CREATED);
    }

    @Transactional
    @Override
    public ResponseEntity<PolicyCreationResponseDto> updatePolicy(PolicyUpdationRequestDto policyUpdationRequest) throws BadRequestException {
        Policy policy= policyRepository.findById(policyUpdationRequest.id()).orElseThrow(()-> new ResourceNotFound("Policy not found with id: "+ policyUpdationRequest.id()));
        if(policyUpdationRequest.title()==null ||  policyUpdationRequest.title().isEmpty()){
            throw new BadRequestException("Title is not valid");
        }
        if(policyUpdationRequest.description()==null ||  policyUpdationRequest.description().isEmpty()){
            throw new BadRequestException("Description is not valid");
        }
        policy.setTitle(policyUpdationRequest.title());
        policy.setDescription(policyUpdationRequest.description());
        policyRepository.save(policy);
        return ResponseEntity.ok().body(policyMapper.policyToPolicyCreationResponseDto(policy));
    }
}