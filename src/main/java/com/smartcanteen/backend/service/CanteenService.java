package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.CanteenStatus;
import com.smartcanteen.backend.repository.CanteenStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CanteenService {

    private final CanteenStatusRepository repository;

    public boolean isCanteenOpen() {
        return repository.findById(1L)
                .map(CanteenStatus::isOpen)
                .orElse(false);
    }

    public void openCanteen() {
        repository.save(new CanteenStatus(1L, true));
    }

    public void closeCanteen() {
        repository.save(new CanteenStatus(1L, false));
    }
}