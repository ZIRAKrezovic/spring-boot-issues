package com.example.demo;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("download")
public class DemoController {
  @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Resource get() {
    return new ClassPathResource("test.txt");
  }
}
