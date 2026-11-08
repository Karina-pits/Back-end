package com.clinic.web;

import com.clinic.dto.DoctorDto;
import com.clinic.service.DepartmentService;
import com.clinic.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ui/doctors")
@RequiredArgsConstructor
public class DoctorWebController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        return "doctors/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("doctor", new DoctorDto());
        model.addAttribute("departments", departmentService.findAll());
        return "doctors/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("doctor", doctorService.findById(id));
        model.addAttribute("departments", departmentService.findAll());
        return "doctors/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("doctor") DoctorDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "doctors/form";
        }
        doctorService.create(dto);
        return "redirect:/ui/doctors";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("doctor") DoctorDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "doctors/form";
        }
        doctorService.update(id, dto);
        return "redirect:/ui/doctors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        doctorService.delete(id);
        return "redirect:/ui/doctors";
    }
}
