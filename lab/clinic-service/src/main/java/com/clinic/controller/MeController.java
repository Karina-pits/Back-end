package com.clinic.controller;

import com.clinic.dto.PatientDto;
import com.clinic.service.PatientService;
import com.clinic.web.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC8: значення, резолвлене через @CurrentUserId, реально ВИКОРИСТОВУЄТЬСЯ
 * всередині методу - передається у findCurrentUserProfileByUserId(userId),
 * а не просто резолвиться і відкидається.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PatientService patientService;

    @GetMapping
    public PatientDto whoAmI(@CurrentUserId String userId) {
        return patientService.findCurrentUserProfileByUserId(userId);
    }
}
