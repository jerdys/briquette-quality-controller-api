package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.dto.CameraDto;
import ru.kpfu.itis.diploma.backend.api.form.EditCameraForm;
import ru.kpfu.itis.diploma.backend.service.CameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CameraController {
    private final CameraService cameraService;

    @RequestMapping(value = "/cameras", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ADMIN')")
    public CameraDto editCamera(@RequestBody EditCameraForm form) {
        return cameraService.configure(form);
    }

    @RequestMapping(value = "/cameras", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ADMIN')")
    public List<CameraDto> getAllCameras() {
        return cameraService.all();
    }
}
