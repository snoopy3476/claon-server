package coLaon.ClaonBack.user.service;

import coLaon.ClaonBack.common.domain.enums.BasicLocalArea;
import coLaon.ClaonBack.common.domain.enums.MetropolitanArea;
import coLaon.ClaonBack.common.exception.BadRequestException;
import coLaon.ClaonBack.common.exception.ErrorCode;
import coLaon.ClaonBack.common.infrastructure.EmailSender;
import coLaon.ClaonBack.common.validator.PasswordFormatValidator;
import coLaon.ClaonBack.common.validator.Validator;
import coLaon.ClaonBack.user.domain.User;
import coLaon.ClaonBack.user.dto.DuplicatedCheckResponseDto;
import coLaon.ClaonBack.user.dto.ResetPasswordRequestDto;
import coLaon.ClaonBack.user.dto.SignUpRequestDto;
import coLaon.ClaonBack.user.dto.UserResponseDto;
import coLaon.ClaonBack.user.repository.UserRepository;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DuplicatedCheckResponseDto emailDuplicatedCheck(String email) {
        return DuplicatedCheckResponseDto.of(this.userRepository.findByEmail(email).isPresent());
    }

    @Transactional(readOnly = true)
    public DuplicatedCheckResponseDto nicknameDuplicatedCheck(String nickname) {
        return DuplicatedCheckResponseDto.of(this.userRepository.findByNickname(nickname).isPresent());
    }

    @Transactional
    public UserResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        this.userRepository.findByEmail(signUpRequestDto.getEmail()).ifPresent(
                email -> {
                    throw new BadRequestException(
                            ErrorCode.ROW_ALREADY_EXIST,
                            "이미 존재하는 이메일입니다."
                    );
                }
        );

        this.userRepository.findByNickname(signUpRequestDto.getNickname()).ifPresent(
                nickname -> {
                    throw new BadRequestException(
                            ErrorCode.ROW_ALREADY_EXIST,
                            "이미 존재하는 닉네임입니다."
                    );
                }
        );

        signUpRequestDto.getPassword().ifPresent(password -> {
            Validator validator = new PasswordFormatValidator(password);
            validator.validate();
        });

        return UserResponseDto.from(userRepository.save(
                User.of(
                        signUpRequestDto.getPhoneNumber(),
                        signUpRequestDto.getEmail(),
                        signUpRequestDto.getPassword().orElse(null),
                        signUpRequestDto.getNickname(),
                        MetropolitanArea.of(signUpRequestDto.getMetropolitanActiveArea()),
                        BasicLocalArea.of(
                                signUpRequestDto.getMetropolitanActiveArea(),
                                signUpRequestDto.getBasicLocalActiveArea()
                        ),
                        signUpRequestDto.getImagePath(),
                        signUpRequestDto.getInstagramId()
                ))
        );
    }

    
    @Transactional
    public void resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
        
        User user = this.userRepository
            .findByEmail(resetPasswordRequestDto.getEmail())
            .or(() -> this.userRepository
                .findByPhoneNumber(resetPasswordRequestDto.getPhoneNumber()))
            .orElseThrow(() -> {
                    throw new BadRequestException(ErrorCode.INVALID_FORMAT,
                                                  "해당하는 유저를 찾을 수 없습니다.");
                });
        

        // generate password
        final int PASSWORD_LEN = 20;
        final String ALPHABETS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String NUMBERS = "0123456789";
        final String SPECIALCHARS = "@$!%*#?&";
        final char PASSWORD_CHARS[] = (ALPHABETS + NUMBERS + SPECIALCHARS).toCharArray();
        final char PASSWORD_ALPHABETS_CHARS[] = ALPHABETS.toCharArray();
        final char PASSWORD_NUMBERS_CHARS[] = NUMBERS.toCharArray();
        final char PASSWORD_SPECIALCHARS_CHARS[] = SPECIALCHARS.toCharArray();
        
        Random rand = new Random();
        
        StringBuilder newRandomPasswordBuilder = 
            (rand.ints(0, PASSWORD_CHARS.length - 3)
             .limit(PASSWORD_LEN)
             .map(i -> PASSWORD_CHARS[i])
             .collect(StringBuilder::new,
                      StringBuilder::appendCodePoint,
                      StringBuilder::append));
        
        int insertPos[] = {
            rand.nextInt(PASSWORD_LEN - 3),
            rand.nextInt(PASSWORD_LEN - 3),
            rand.nextInt(PASSWORD_LEN - 3)
        };
        char insertChar[] = {
            PASSWORD_ALPHABETS_CHARS[rand.nextInt(PASSWORD_ALPHABETS_CHARS.length)],
            PASSWORD_NUMBERS_CHARS[rand.nextInt(PASSWORD_NUMBERS_CHARS.length)],
            PASSWORD_SPECIALCHARS_CHARS[rand.nextInt(PASSWORD_SPECIALCHARS_CHARS.length)],
        };
        for (int i = 0; i < insertPos.length; i++) {
            newRandomPasswordBuilder.insert(insertPos[i], insertChar[i]);
        }

        String newRandomPassword = newRandomPasswordBuilder.toString();

        
        // set to db
        user.setPassword(newRandomPassword);
        userRepository.save(user);
        
        
        // send email if commit success
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization(){
                    
                static final String NEW_PW_EMAIL_SUBJECT =
                    "[CLAON] 임시 비밀번호 안내";
                static final String NEW_PW_EMAIL_BODY_HEAD =
                    "<p>회원님의 임시비밀번호는 다음과 같습니다:</p>"
                    +  ("<h1 "
                        + ("style='"
                           + "background-color:rgba(127,127,127,0.2); "
                           + "text-align:center"
                           + "'>")
                        );
                static final String NEW_PW_EMAIL_BODY_TAIL =
                    "</h1>";
                                
                
                public void afterCommit(){

                    EmailSender.send(NEW_PW_EMAIL_SUBJECT,
                                     (NEW_PW_EMAIL_BODY_HEAD
                                      + HtmlUtils.htmlEscape(newRandomPassword)
                                      + NEW_PW_EMAIL_BODY_TAIL),
                                     true,
                                     user.getEmail());
                }
            });
    }
}


