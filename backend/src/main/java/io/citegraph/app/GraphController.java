package io.citegraph.app;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class GraphController {
    @CrossOrigin
    @GetMapping("/author/{id}")
    public String getClient(@PathVariable Long id) {
        String json = String.format("{\"id\": %d, \"name\": \"placeholder\"}", id);
        System.out.println(json);
        return json;
    }
}
