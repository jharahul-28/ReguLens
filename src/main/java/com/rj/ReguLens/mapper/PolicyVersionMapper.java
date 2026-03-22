package com.rj.ReguLens.mapper;

import com.rj.ReguLens.dto.policyVersion.PolicyVersionResponseDto;
import com.rj.ReguLens.entity.PolicyVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface PolicyVersionMapper{
    @Mapping(source = "policy.id", target = "policyId")
    @Mapping(source= "approvedBy.id", target = "approvedById")
    PolicyVersionResponseDto policyVersionToPolicyVersionResponseDto(PolicyVersion policyVersion);
    Set<PolicyVersionResponseDto> policyVersionSetToPolicyVersionResponseDto(Set<PolicyVersion> policyVersionSet);
}
