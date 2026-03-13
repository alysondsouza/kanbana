package com.kanbana;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AppController — handles all simple app endpoints
 *
 * @RestController combines two annotations:
 *   @Controller    — marks this class as a Spring MVC controller
 *   @ResponseBody  — writes return value directly to HTTP response body
 */
@RestController
public class AppController {

    /**
     * @GetMapping maps HTTP GET /hello to this method.
     * The return value is written directly to the response body.
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello World from embedded Tomcat!";
    }

    @GetMapping("/bye")
    public String bye() {
        return "Ciao!";
    }
}
