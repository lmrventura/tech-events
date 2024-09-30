package com.eventostec.api.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSConfig { //classe de anotação, cria a instância do objeto do amazon s3 com as nossas credenciais necessárias para usar o objeto para fazer o upload no s3

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public AmazonS3 createS3Instance() {
        return AmazonS3ClientBuilder.standard().
                withCredentials(new DefaultAWSCredentialsProviderChain()).
                withRegion(awsRegion).
                build(); //47, 1:04
    }
}
