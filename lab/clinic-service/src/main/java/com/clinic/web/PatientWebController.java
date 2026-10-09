package com.clinic.web;

import com.clinic.dto.PatientDto;
import com.clinic.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ui/patients")
@RequiredArgsConstructor
public class PatientWebController {

    private final PatientService patientService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("patients", patientService.findAll());
        return "patients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("patient", new PatientDto());
        return "patients/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("patient", patientService.findByIdForCurrentUser(id));
        return "patients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("patient") PatientDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "patients/form";
        }
        patientService.create(dto);
        return "redirect:/ui/patients";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("patient") PatientDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "patients/form";
        }
        patientService.update(id, dto);
        return "redirect:/ui/patients";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        patientService.delete(id);
        return "redirect:/ui/patients";
    }
}
