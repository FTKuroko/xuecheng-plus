package com.xuecheng.checkcode.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Kuroko
 * @description 邮件工具类
 * @date 2023/5/29 20:29
 */
public class MailUtil {

    /**
     * 发送邮件
     * @param email 收件邮箱
     * @param code  验证码
     * @throws MessagingException
     */
    public static void sendTestMail(String email, String code) throws MessagingException {
        // 创建 Properties 类用于记录邮箱的一些属性
        Properties properties = new Properties();
        // 表示 SMTP 发送邮件，必须进行身份验证
        properties.put("mail.smtp.auth", "true");
        // SMTP 服务器
        properties.put("mail.smtp.host", "smtp.qq.com");
        // 端口号, QQ 邮箱端口号是 587
        properties.put("mail.smtp.port", "587");
        // 写信人的账号
        properties.put("mail.user", "982239116@qq.com");
        // 16 位 SMTP 口令
        properties.put("mail.password", "ylmcehseptjhbecg");
        // 构建授权信息，用于进行 SMTP 身份验证
        Authenticator authenticator = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = properties.getProperty("mail.user");
                String password = properties.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 创建邮件会话
        Session mailSession = Session.getInstance(properties, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress from = new InternetAddress(properties.getProperty("mail.user"));
        message.setFrom(from);
        // 设置收件人
        InternetAddress to = new InternetAddress(email);
        message.setRecipient(Message.RecipientType.TO, to);
        // 设置邮件标题
        message.setSubject("Kuroko 邮件测试");
        // 设置邮件的内容体
        message.setContent("尊敬的用户:你好!\n注册验证码为:" + code + "(有效期为一分钟,请勿告知他人)", "text/html;charset=UTF-8");
        // 发送邮件
        Transport.send(message);
    }

    /**
     * 生成验证码
     * @return
     */
    public static String achieveCode() {  //由于数字 1 、 0 和字母 O 、l 有时分不清楚，所以，没有数字 1 、 0
        String[] beforeShuffle = new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a",
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z"};
        List<String> list = Arrays.asList(beforeShuffle);//将数组转换为集合
        Collections.shuffle(list);  //打乱集合顺序
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s); //将集合转化为字符串
        }
        return sb.substring(3, 8);
    }

    public static void main(String[] args) throws MessagingException {
        sendTestMail("982239116@qq.com", new MailUtil().achieveCode());
    }
}
