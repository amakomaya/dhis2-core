package org.hisp.dhis;

import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/extend")
public class ExtendPlatformController {

  CategoryService catService;

  public ExtendPlatformController(CategoryService catService) {
    this.catService = catService;
  }

  @GetMapping("/hello")
  public ResponseEntity<String> hello() {
    return ResponseEntity.ok("hello from extend platform");
  }

  @GetMapping("/categoryCombos")
  public ResponseEntity<List<CategoryCombo>> getCatCombos() {
    return ResponseEntity.ok(catService.getAllCategoryCombos());
  }
}
