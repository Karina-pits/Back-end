package com.clinic.controller;

import com.clinic.dto.DepartmentDto;
import com.clinic.security.SecurityUtils;
import com.clinic.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DepartmentController.class)
class CreateMappingWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDepartment_withCreateMapping_returns201AndCorrectRoute() throws Exception {
        DepartmentDto requestDto = new DepartmentDto();
        requestDto.setName("Неврологія");
        requestDto.setDescription("Тестовий опис");

        when(departmentService.create(any(DepartmentDto.class))).thenReturn(requestDto);

        mockMvc.perform(post("/api/departments")
                        .with(csrf())                 // ← новий рядок
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }
}