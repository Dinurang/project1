package com.suntravels.controller;

import com.suntravels.model.SearchRequest;
import com.suntravels.model.SearchResult;
import com.suntravels.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
// Call-center search API that delegates all pricing and availability rules to SearchService.
public class AgentSearchController {

    private final SearchService searchService;

    public AgentSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest req) {
        return searchService.search(req);
    }
}
