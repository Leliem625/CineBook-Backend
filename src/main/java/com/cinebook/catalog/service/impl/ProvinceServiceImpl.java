package com.cinebook.catalog.service.impl;

import com.cinebook.catalog.dto.ProvinceDto;
import com.cinebook.catalog.entity.Province;
import com.cinebook.catalog.repository.ProvinceRepository;
import com.cinebook.catalog.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Cài đặt {@link ProvinceService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;

    @Override
    public List<ProvinceDto> getProvince() {
        return provinceRepository.findAll(Sort.by("sortOrder", "name")).stream()
                .map(ProvinceServiceImpl::toDto)
                .toList();
    }

    @Override
    public Optional<ProvinceDto> findBySlug(String slug) {
        return provinceRepository.findBySlug(slug).map(ProvinceServiceImpl::toDto);
    }

    private static ProvinceDto toDto(Province p) {
        return new ProvinceDto(p.getId(), p.getSlug(), p.getName(), p.getSortOrder());
    }
}
