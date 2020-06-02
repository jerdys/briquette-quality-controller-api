package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.model.analyze.AnalyzeProfile;
import ru.kpfu.itis.diploma.backend.service.analyze.AnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalyzeSettingsController {
    private final AnalyzeService analyzeService;

    @RequestMapping(value = "/analyze/profiles/change", method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public AnalyzeProfile changeActiveProfile(@RequestParam Long active) {
        return analyzeService.changeActiveProfile(active);
    }


    @RequestMapping(value = "/analyze/profiles", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<AnalyzeProfile> getProfiles() {
        return analyzeService.getProfiles();
    }

    @RequestMapping(value = "/analyze/profiles/{profileId}", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ADMIN')")
    public AnalyzeProfile getProfileById(@PathVariable Long profileId) {
        return analyzeService.getProfileById(profileId);
    }

    @RequestMapping(value = "/analyze/profiles", method = RequestMethod.POST)
    @PreAuthorize("hasRole('ADMIN')")
    public AnalyzeProfile createProfile(@RequestBody AnalyzeProfile settings) {
        return analyzeService.createProfile(settings);
    }

    @RequestMapping(value = "/analyze/profiles", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ADMIN')")
    public AnalyzeProfile updateProfile(@RequestBody AnalyzeProfile settings) {
        return analyzeService.updateProfile(settings);
    }
}
