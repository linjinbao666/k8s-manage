package monitor.dao;

import monitor.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AlertRuleDao extends JpaRepository<AlertRule, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from alert_rule where alert_name = ?1", nativeQuery = true)
    AlertRule findByName(String alertName);

    @Query(value = "select * from alert_rule where en_name = ?1", nativeQuery = true)
    AlertRule findByEnName(String en_name);
}
