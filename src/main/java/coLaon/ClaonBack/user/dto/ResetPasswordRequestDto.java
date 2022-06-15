package coLaon.ClaonBack.user.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordRequestDto {
    private String phoneNumber;
    private String email;
}
