package ecs;

import cn.hutool.Hutool;
import cn.hutool.extra.mail.Mail;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;

public class MailTest {
    public static void main(String[] args) {
        MailAccount account = new MailAccount();
        account.setHost("mail.fline88.com");
        account.setPort(587);
        account.setAuth(true);
        account.setFrom("linjb@fline88.com");
        account.setUser("linjb@fline88.com");
        account.setPass("66");

        MailUtil.send(account, "linjb@fline88.com", "测试", "邮件来自Hutool测试", false);

    }
}

