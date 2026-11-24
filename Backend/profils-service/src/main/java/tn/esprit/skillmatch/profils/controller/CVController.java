package tn.esprit.skillmatch.profils.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.skillmatch.profils.dto.CVResponse;
import tn.esprit.skillmatch.profils.service.ProfilService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch")
public class CVController {

    private final ProfilService profilService;

    public CVController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @PostMapping(value = "/profiles/me/cv", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public CVResponse upload(@RequestAttribute("userId") Long userId,
                             @RequestParam("file") MultipartFile file) {
        return profilService.uploadCV(userId, file);
    }

    @GetMapping("/profiles/me/cv")
    public List<CVResponse> mesCVs(@RequestAttribute("userId") Long userId) {
        return profilService.findCVs(userId);
    }

    @DeleteMapping("/profiles/me/cv/{cvId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@RequestAttribute("userId") Long userId, @PathVariable Long cvId) {
        profilService.supprimerCV(userId, cvId);
    }

    @GetMapping("/profiles/{userId}/cv/{cvId}")
    public CVResponse cvParUtilisateur(@PathVariable Long userId, @PathVariable Long cvId) {
        return profilService.findCV(userId, cvId);
    }
}
