package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.services.HelloWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/helloworld")
@RequiredArgsConstructor
@Tag(name = "Hello World Controller", description = "Display Hello World")
public class HelloWorldController {
    private final HelloWorldService helloWorldService;

    @Operation(summary = "Display Hello World", description = "Display Hello World.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return Hello World", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<String> getHelloWorld() {
        String result = helloWorldService.getHelloWorld();
        return ResponseEntity.ok(result);
    }

}
