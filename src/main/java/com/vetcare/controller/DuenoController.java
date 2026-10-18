package com.vetcare.controller;

import com.vetcare.dto.DuenoCreadoResponse;
import com.vetcare.dto.DuenoRequest;
import com.vetcare.dto.DuenoResponse;
import com.vetcare.service.DuenoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-01: dueños. */
@RestController
@RequestMapping("/api/duenos")
@RequiredArgsConstructor
public class DuenoController {

    private final DuenoService duenoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DuenoCreadoResponse crear(@Valid @RequestBody DuenoRequest req) {
        return duenoService.crear(req);
    }

    @GetMapping
    public List<DuenoResponse> listar(@RequestParam(required = false) String documento,
                                      @RequestParam(required = false) String nombre) {
        return duenoService.listar(documento, nombre);
    }

    @GetMapping("/{id}")
    public DuenoResponse obtener(@PathVariable Long id) {
        return duenoService.obtener(id);
    }

    @PutMapping("/{id}")
    public DuenoResponse actualizar(@PathVariable Long id, @Valid @RequestBody DuenoRequest req) {
        return duenoService.actualizar(id, req);
    }
}
