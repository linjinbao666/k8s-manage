package ama.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.springframework.stereotype.Component;

/**
 * jpa建表时候的编码字符集问题
 */

@Slf4j
public class MySQL5InnoDBDialectUtf8mb4 extends MySQL57Dialect {
    @Override
    public String getTableTypeString() {
        log.info("使用utf8mb4");
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci";
    }
}
