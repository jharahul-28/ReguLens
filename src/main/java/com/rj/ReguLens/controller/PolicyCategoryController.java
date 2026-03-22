package com.rj.ReguLens.controller;

import com.rj.ReguLens.dto.policy.PolicyCategoryCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCategoryCreationResponseDto;
import com.rj.ReguLens.service.PolicyCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policycategory")
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
@RequiredArgsConstructor
public class PolicyCategoryController {

    PolicyCategoryService policyCategoryService;

    @PostMapping
    public ResponseEntity<PolicyCategoryCreationResponseDto> createPolicyCategory(@RequestBody PolicyCategoryCreationRequestDto policyCategoryCreationRequestDto) throws BadRequestException {
        return policyCategoryService.createPolicyCategory(policyCategoryCreationRequestDto);
    }
}
