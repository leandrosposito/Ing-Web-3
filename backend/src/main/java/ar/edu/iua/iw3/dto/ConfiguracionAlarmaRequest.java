package ar.edu.iua.iw3.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConfiguracionAlarmaRequest(
        @NotNull @DecimalMin("-50") @DecimalMax("200") Double temperaturaUmbral,
        @NotEmpty @Size(max = 10) List<@NotBlank @Email String> destinatarios) {
}
