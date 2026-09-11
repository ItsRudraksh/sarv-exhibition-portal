package com.sarv.exhibitionportal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Forwards packaged SPA routes to {@code index.html} so deep links work without Vite. */
@Controller
public class SpaForwardController {

    @GetMapping({"/staff", "/staff/"})
    public String staffRoot() {
        return "forward:/index.html";
    }

    @GetMapping("/staff/{*path}")
    public String staffPath() {
        return "forward:/index.html";
    }

    @GetMapping({"/admin", "/admin/"})
    public String adminRoot() {
        return "forward:/index.html";
    }

    @GetMapping("/admin/{*path}")
    public String adminPath() {
        return "forward:/index.html";
    }

    @GetMapping({"/web", "/web/"})
    public String websiteEntry() {
        return "forward:/index.html";
    }
}
