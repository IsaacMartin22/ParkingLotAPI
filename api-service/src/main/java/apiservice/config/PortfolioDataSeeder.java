package apiservice.config;

import apiservice.model.PortfolioDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class PortfolioDataSeeder {

    private final MongoTemplate mongoTemplate;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.seed-data:false}")
    private boolean seedData;

    public PortfolioDataSeeder(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Bean
    ApplicationRunner seedPortfolioData() {
        return args -> {
            if (!seedData) {
                return;
            }

            if (!mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.createCollection(collectionName);
            }

            if (mongoTemplate.getCollection(collectionName).countDocuments() > 0) {
                return;
            }

            for (PortfolioDocument document : buildSeedDocuments()) {
                mongoTemplate.insert(document, collectionName);
            }
        };
    }

    private List<PortfolioDocument> buildSeedDocuments() {
        List<PortfolioDocument> docs = new ArrayList<>();

        docs.add(buildDocument(
                "I am Isaac Martin, a software engineer based in Las Vegas, NV, and I am open to relocation. I hold a Bachelor of Science in Computer Science from Oregon State University (2021) with a 3.68 GPA.",
                "summary",
                "personal_profile"
        ));

        docs.add(buildDocument(
                "I worked as an Associate Software Engineer at Widen and later as a Software Engineer at Widen from 2021 to 2026. Widen is a Digital Asset Management (DAM) SaaS company. My work focused on a distributed Java + TypeScript + React microservices system with 30+ apps and services.",
                "experience",
                "widen_work_history"
        ));

        docs.add(buildDocument(
                "At Widen, I eliminated a manual support workflow by adding fullstack self-service functionality with safeguards and automated tests. I used Java, TypeScript, and React to improve customer experience and reduce support overhead.",
                "accomplishment",
                "self_service_support_flow"
        ));

        docs.add(buildDocument(
                "I reduced database load and increased user clarity by adding validation and snackbar messaging to a frontend application, reducing total frontend requests sent. This work covered TypeScript and React.",
                "accomplishment",
                "frontend_validation_and_request_reduction"
        ));

        docs.add(buildDocument(
                "I expanded coverage for public APIs and internal APIs/SDKs by adding Java Spring Boot endpoints and publishing the new endpoints in the app's SDK. This included API design, backend implementation, and SDK integration work.",
                "accomplishment",
                "api_and_sdk_expansion"
        ));

        docs.add(buildDocument(
                "I eliminated unnecessary tripling of specific network requests by leveraging knowledge of frontend render cycles in React, improving performance and application responsiveness for users.",
                "accomplishment",
                "frontend_performance_optimization"
        ));

        docs.add(buildDocument(
                "I extracted functionality from a 25-year-old monolithic application into new services and refactored functionality to be easier to extract. I worked with Java, Spring, and Apache Tapestry in legacy modernization efforts.",
                "accomplishment",
                "monolith_refactor_and_service_extraction"
        ));

        docs.add(buildDocument(
                "I used SQL knowledge to improve performance for several database queries, working with Hibernate and relational database optimization. This included understanding query behavior, indexing, and application-level tuning.",
                "accomplishment",
                "database_performance_and_sql"
        ));

        docs.add(buildDocument(
                "Across the collection of applications and services, I resolved thousands of defects, supporting a large distributed platform and helping maintain overall product quality and reliability.",
                "accomplishment",
                "defect_resolution_and_quality"
        ));

        docs.add(buildDocument(
                "In 2026, I contributed to open source by creating Hiring-agent, a Python pipeline for AI evaluation of resumes, and by fixing a frontend bug in Lichess, a TypeScript and Scala community-driven chess website.",
                "contribution",
                "open_source_contributions"
        ));

        docs.add(buildDocument(
                "I built a portfolio website containing my open source applications and services, including a parking lot availability application using server-sent events, a backend Java REST API and SDK with PostgreSQL CRUD SQL operations, and integrations with project CI/CD and deployment pipelines for visibility.",
                "project",
                "portfolio_website_and_projects"
        ));

        docs.add(buildDocument(
                "My technical skills include Java, TypeScript, JavaScript, C++, Python, Rust, HTML, CSS, and SQL. Frameworks include Spring, Spring Boot, React, Hibernate, and Apache Tapestry. Testing includes Playwright, Spock, Groovy, and JUnit.",
                "skills",
                "languages_frameworks_and_testing"
        ));

        docs.add(buildDocument(
                "I also work with Buildkite, Docker, Kubernetes, Maven, JFrog, and Stormforge for CI and deployment. My cloud experience includes AWS SQS, AWS S3, and AWS Kinesis. I have worked with Sumologic, Datadog, Grafana, Snowflake, and integrations tooling.",
                "skills",
                "ci_deployment_cloud_and_monitoring"
        ));

        docs.add(buildDocument(
                "I am a backend-focused software engineer with experience in Java, Spring Boot, distributed systems, API design, frontend optimization, SQL, and production observability. I am comfortable across full-stack work, service extraction, and product reliability.",
                "summary",
                "technical_summary"
        ));

        return docs;
    }

    private PortfolioDocument buildDocument(String text, String category, String source) {
        PortfolioDocument document = new PortfolioDocument();
        document.setText(text);
        document.setEmbedding(buildDeterministicEmbedding(text));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", category);
        metadata.put("source", source);
        metadata.put("type", "work_experience");
        document.setMetadata(metadata);

        return document;
    }

    private List<Double> buildDeterministicEmbedding(String text) {
        List<Double> embedding = new ArrayList<>(1536);
        for (int i = 0; i < 1536; i++) {
            int charIndex = i % Math.max(text.length(), 1);
            char character = text.charAt(charIndex);
            double normalized = ((int) character % 17) / 17.0;
            double drift = ((i % 13) * 0.01);
            embedding.add(normalized + drift);
        }
        return embedding;
    }
}
