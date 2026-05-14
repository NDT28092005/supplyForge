package com.supplyforge.ai.application;

import com.supplyforge.ai.domain.model.DataSourceStatus;
import com.supplyforge.ai.domain.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataSourceStatusService {

    private final DataSourceRepository dataSourceRepository;

    public DataSourceStatusService(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long dataSourceId, String message) {
        if (dataSourceId == null) {
            return;
        }
        dataSourceRepository.findById(dataSourceId).ifPresent(ds -> {
            ds.setStatus(DataSourceStatus.FAILED);
            String msg = message == null ? "error" : message;
            ds.setErrorMessage(msg.substring(0, Math.min(2000, msg.length())));
            dataSourceRepository.save(ds);
        });
    }
}
