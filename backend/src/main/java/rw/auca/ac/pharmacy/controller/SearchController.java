package rw.auca.ac.pharmacy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import rw.auca.ac.pharmacy.dto.search.GlobalSearchResultDto;
import rw.auca.ac.pharmacy.service.SearchService;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<GlobalSearchResultDto>> search(@RequestParam("q") String query,
                                                              @RequestParam(required = false) String type,
                                                              @RequestParam(required = false, defaultValue = "false") boolean byId) {
        return ResponseEntity.ok(searchService.search(query, type, byId));
    }
}
