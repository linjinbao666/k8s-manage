package monitor.dao;

import monitor.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertHistoryDao extends JpaRepository<AlertHistory, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from alert_history where rule_id = ?1", nativeQuery = true)
    List<AlertHistory> findByAlertRuleId(long ruleId);
}
