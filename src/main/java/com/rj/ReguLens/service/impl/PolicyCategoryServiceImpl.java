package com.rj.ReguLens.service.impl;

import com.rj.ReguLens.dto.policy.PolicyCategoryCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCategoryCreationResponseDto;
import com.rj.ReguLens.entity.PolicyCategory;
import com.rj.ReguLens.exception.BadRequestException;
import com.rj.ReguLens.mapper.PolicyCategoryMapper;
import com.rj.ReguLens.repository.PolicyCategoryRepository;
import com.rj.ReguLens.service.PolicyCategoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PolicyCategoryServiceImpl implements PolicyCategoryService {

    PolicyCategoryRepository policyCategoryRepository;
    PolicyCategoryMapper policyCategoryMapper;

    @Override
    @Transactional
    public ResponseEntity<PolicyCategoryCreationResponseDto> createPolicyCategory(PolicyCategoryCreationRequestDto policyCategoryCreationRequestDto) throws BadRequestException {
        String category = policyCategoryCreationRequestDto.name();
        if (category == null || category.isBlank()) {
            throw new BadRequestException("PolicyCategory name is empty");
        }
        PolicyCategory policyCategory = PolicyCategory.builder()
                .name(category)
                .build();
        PolicyCategory savedPolicyCategory = policyCategoryRepository.save(policyCategory);
        PolicyCategoryCreationResponseDto policyCategoryCreationResponseDto = policyCategoryMapper.policyCategoryToPolicyCategoryResponseDto(savedPolicyCategory);
        return new ResponseEntity<>(policyCategoryCreationResponseDto, HttpStatus.CREATED);
    }
}
