package com.clinic.web;

import com.clinic.dto.DepartmentDto;
import com.clinic.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ui/departments")
@RequiredArgsConstructor
public class DepartmentWebController {

    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "departments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("department", new DepartmentDto());
        return "departments/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("department", departmentService.findById(id));
        return "departments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("department") DepartmentDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.create(dto);
        return "redirect:/ui/departments";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("department") DepartmentDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.update(id, dto);
        return "redirect:/ui/departments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        departmentService.delete(id);
        return "redirect:/ui/departments";
    }
}
