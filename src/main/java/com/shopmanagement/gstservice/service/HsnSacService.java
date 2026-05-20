package com.shopmanagement.gstservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.gstservice.api.GstApi.HsnSacResponse;
import com.shopmanagement.gstservice.repository.HsnSacMasterRepository;

@Service
public class HsnSacService {

    private final HsnSacMasterRepository repository;

    public HsnSacService(HsnSacMasterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HsnSacResponse> search(String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) {
            return List.of();
        }
        return repository.search(query).stream()
                .map(h -> new HsnSacResponse(h.getId(), h.getCode(), h.getDescription(), h.getType(), h.getChapter()))
                .limit(50)
                .toList();
    }
}
