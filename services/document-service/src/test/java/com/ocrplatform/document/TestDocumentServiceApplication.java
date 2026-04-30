package com.ocrplatform.document;

import org.springframework.boot.SpringApplication;

public class TestDocumentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocumentServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
