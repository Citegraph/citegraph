package io.citegraph.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graph")
public class GraphController {
    @GetMapping("/{id}")
    public String getClient(@PathVariable Long id) {
        return "Placeholder";
    }
}
