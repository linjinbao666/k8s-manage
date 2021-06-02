package monitor.dao;

import monitor.entity.IPRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface IPRuleDao extends JpaRepository<IPRule, Long>, JpaSpecificationExecutor {

    @Query(value = "select * from iprule where name =?1 ", nativeQuery = true)
    IPRule findByName(String name);
}
