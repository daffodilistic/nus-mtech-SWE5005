package com.nus.team3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Controller
@SpringBootApplication
@EnableSwagger2
public class OrderMatchingServceApplication {

  @RequestMapping("/")
  @ResponseBody
  String home() {
    return "Hello World Order!";
  }

  public static void main(String[] args) {
    SpringApplication.run(OrderMatchingServceApplication.class, args);
  }
}