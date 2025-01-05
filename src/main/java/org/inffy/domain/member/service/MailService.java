package org.inffy.domain.member.service;

import org.inffy.domain.common.dto.ResponseDto;
import org.inffy.domain.member.dto.res.EmailResponseDto;
import org.inffy.domain.member.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Random;

@Service
public class MailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private RedisUtil redisUtil;
    private int authNumber;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void makeRandomNumber() {
        Random random = new Random();
        String randomNumber = "";
        for (int i = 0; i < 6; i++) {
            randomNumber += Integer.toString(random.nextInt(10));
        }

        authNumber = Integer.parseInt(randomNumber);
    }

    public ResponseDto<EmailResponseDto> mailAuthCheck(String schoolEmail, String authNum) {
        EmailResponseDto emailResponseDto = new EmailResponseDto();

        emailResponseDto.setAuthNumber(Integer.toString(authNumber));
        emailResponseDto.setSchoolEmail(schoolEmail);

        if (redisUtil.getData(authNum) == null) {
            emailResponseDto.setSuccess(false);
        } else if (redisUtil.getData(authNum).equals(schoolEmail)) {
            emailResponseDto.setSuccess(true);
        }
        else {
            emailResponseDto.setSuccess(false);
        }

        return ResponseDto.of(emailResponseDto, "이메일 인증");
    }

    public ResponseDto<EmailResponseDto> joinEmail(String schoolEmail){
        makeRandomNumber();
        String setFrom = fromEmail;
        String toMail = schoolEmail;
        String title = "회원 가입 인증 이메일 입니다.";
        String content =
                "[Inffy] 인천대학교 계정 인증 요청" +
                        "<br><br>" +
                        "안녕하세요, 인천대학교 회원님," +
                        "<br>" +
                        "Inffy 서비스를 이용해 주셔서 감사합니다." +
                        "<br><br>" +
                        "아래 인증 번호를 확인하여 이메일 인증을 완료해 주세요." +
                        "<br><br>" +
                        "<strong>인증 번호:</strong> <span style='font-size: 18px; font-weight: bold; color: #007BFF;'>" + authNumber + "</span>" +
                        "<br><br>" +
                        "※ 이 코드는 10분 동안 유효합니다." +
                        "<br>" +
                        "※ 코드를 정확히 입력하지 않을 경우, 인증이 완료되지 않습니다." +
                        "<br><br>" +
                        "인증 완료 후, Inffy 서비스를 정상적으로 이용하실 수 있습니다." +
                        "<br>" +
                        "문의사항이 있으시면 아래 연락처로 연락주세요." +
                        "<br><br>" +
                        "감사합니다." +
                        "<br>" +
                        "- Inffy 운영팀" +
                        "<br>" +
                        "- 문의: inffy-support@inu.ac.kr";

        EmailResponseDto emailResponseDto = new EmailResponseDto();

        try {
            mailSend(setFrom, toMail, title, content);
            emailResponseDto.setSuccess(true);
            emailResponseDto.setSchoolEmail(schoolEmail);
            emailResponseDto.setAuthNumber(Integer.toString(authNumber));
        } catch (MessagingException e) {
            emailResponseDto.setSuccess(false);
        }

        return ResponseDto.of(emailResponseDto, "인증 이메일 전송");
    }

    private void mailSend(String setFrom, String toMail, String title, String content) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try{
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            helper.setFrom(setFrom); //이메일의 발신자 주소 설정
            helper.setTo(toMail); //이메일의 수신자 주소 설정
            helper.setSubject(title); //이메일의 제목을 설정
            helper.setText(content, true);
            mailSender.send(mimeMessage);
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
        }

        // 5분 동안 인증번호가 생존
        redisUtil.setDataExpire(Integer.toString(authNumber), toMail, 60*5L);
    }
}
