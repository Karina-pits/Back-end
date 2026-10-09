package com.clinic.web;

import com.clinic.dto.AppointmentDto;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ui/appointments")
@RequiredArgsConstructor
public class AppointmentWebController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("appointments", appointmentService.findAllVisibleToCurrentUser());
        return "appointments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        AppointmentDto dto = new AppointmentDto();
        var myProfile = patientService.findCurrentUserProfile();
        if (myProfile != null) {
             dto.setPatientId(myProfile.getId());
            model.addAttribute("myPatientName", myProfile.getFirstName() + " " + myProfile.getLastName());
        }
        model.addAttribute("appointment", dto);
        model.addAttribute("doctors", doctorService.findAll());
        return "appointments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("appointment") AppointmentDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("doctors", doctorService.findAll());
            return "appointments/form";
        }
        appointmentService.create(dto);
        return "redirect:/ui/appointments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return "redirect:/ui/appointments";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        appointmentService.updateStatus(id, com.clinic.entity.AppointmentStatus.COMPLETED);
        return "redirect:/ui/appointments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return "redirect:/ui/appointments";
    }
}
