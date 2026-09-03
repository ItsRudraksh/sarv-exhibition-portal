package com.sarv.exhibitionportal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
}
