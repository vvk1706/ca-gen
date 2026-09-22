package com.dlis.core.dao;

import com.dlis.core.domain.DrivingHistory;
import java.util.List;

/**
 * DAO interface for DLIS.DRIVING_HISTORY table.
 */
public interface DrivingHistoryDao {

    /** Returns all active records for a candidate. */
    List<DrivingHistory> findActiveByCandidateId(Long candidateId);
}
