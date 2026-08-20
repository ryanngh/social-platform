package com.ryan.socialplatform;

import org.springframework.boot.SpringApplication;

public class TestSocialplatformApplication {

    public static void main(String[] args) {
        SpringApplication.from(SocialplatformApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
