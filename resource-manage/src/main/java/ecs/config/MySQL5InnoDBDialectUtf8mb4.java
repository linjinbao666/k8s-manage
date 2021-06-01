package ecs.config;

import org.hibernate.dialect.MySQL57Dialect;
import org.springframework.stereotype.Component;

/**
 * jpa建表时候的编码字符集问题
 */

@Component
public class MySQL5InnoDBDialectUtf8mb4 extends MySQL57Dialect {
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci";
    }
}
