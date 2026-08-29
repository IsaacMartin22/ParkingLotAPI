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

        // Do you have a Bachelor's degree or higher in Computer Science or a related field?
        docs.add(buildDocument(
                """
                I am Isaac Martin, a software engineer based in Las Vegas, NV, and I am open to relocation.
                I hold a Bachelor of Science in Computer Science from Oregon State University (2021) with a 3.68 GPA.
                """,
                "summary",
                "personal_profile"
        ));

        // What is your work history?
        docs.add(buildDocument(
                """
                I worked as an Associate Software Engineer at Widen and later as a Software Engineer at Widen from 2021 to 2026.
                Widen is a Digital Asset Management (DAM) SaaS company. My work focused on a distributed Java + TypeScript + React microservices system with
                30+ apps and services.
                """,
                "experience",
                "widen_work_history"
        ));

        // What are some significant accomplishments or contributions you have made in your previous roles?
        docs.add(buildDocument(
                """
                At Widen, I eliminated a manual support workflow by adding fullstack self-service functionality with safeguards and automated tests.
                I used Java, TypeScript, and React to improve customer experience and reduce support overhead.
                """,
                "accomplishment",
                "self_service_support_flow"
        ));

        docs.add(buildDocument(
                """
                I reduced database load and increased user clarity by adding validation and snackbar messaging to a frontend application,
                reducing total frontend requests sent. This work covered TypeScript and React.
                """,
                "accomplishment",
                "frontend_validation_and_request_reduction"
        ));

        docs.add(buildDocument(
                """
                I expanded coverage for public APIs and internal APIs/SDKs by adding Java Spring Boot endpoints and publishing the new endpoints in the app's SDK.
                This included API design, backend implementation, and SDK integration work.
                """,
                "accomplishment",
                "api_and_sdk_expansion"
        ));

        docs.add(buildDocument(
                """
                I eliminated unnecessary tripling of specific network requests by leveraging knowledge of frontend render cycles in React,
                improving performance and application responsiveness for users.
                """,
                "accomplishment",
                "frontend_performance_optimization"
        ));

        docs.add(buildDocument(
                """
                I extracted functionality from a 25-year-old monolithic application into new services and refactored functionality to be easier to extract.
                I worked with Java, Spring, and Apache Tapestry in legacy modernization efforts.
                """,
                "accomplishment",
                "monolith_refactor_and_service_extraction"
        ));

        docs.add(buildDocument(
                """
                I used SQL knowledge to improve performance for several database queries, working with Hibernate and relational database optimization.
                This included understanding query behavior, indexing, and application-level tuning.
                """,
                "accomplishment",
                "database_performance_and_sql"
        ));

        docs.add(buildDocument(
                """
                Across the collection of applications and services, I resolved thousands of defects,
                supporting a large distributed platform and helping maintain overall product quality and reliability.
                """,
                "accomplishment",
                "defect_resolution_and_quality"
        ));

        docs.add(buildDocument(
                """
                In 2026, I contributed to open source by creating Hiring-agent, a Python pipeline for AI evaluation of resumes,
                and by fixing a frontend bug in Lichess, a TypeScript and Scala community-driven chess website.
                """,
                "contribution",
                "open_source_contributions"
        ));

        docs.add(buildDocument(
                """
                I built a portfolio website containing my open source applications and services,
                including a parking lot availability application using server-sent events,
                a backend Java REST API and SDK with PostgreSQL CRUD SQL operations,
                and integrations with project CI/CD and deployment pipelines for visibility.
                It also contains a RAG (retrieval-augmented generation) chatbot trained on my resume and
                manually entered information for answering typical recruiter/hiring manager questions
                about my work experience and technical skills.
                """,
                "project",
                "portfolio_website_and_projects"
        ));

        docs.add(buildDocument(
                """
                My technical skills include Java, TypeScript, JavaScript, C++, Python, Rust, HTML, CSS, and SQL.
                Frameworks include Spring, Spring Boot, React, Hibernate, and Apache Tapestry.
                Testing includes Playwright, Spock, Groovy, and JUnit.
                """,
                "skills",
                "languages_frameworks_and_testing"
        ));

        docs.add(buildDocument(
                """
                I also work with Buildkite, Docker, Kubernetes, Maven, JFrog, and Stormforge for CI and deployment.
                My cloud experience includes AWS SQS, AWS S3, and AWS Kinesis.
                I have worked with Sumologic, Datadog, Grafana, Snowflake, and integrations tooling.
                """,
                "skills",
                "ci_deployment_cloud_and_monitoring"
        ));

        //
        docs.add(buildDocument(
                """
                I am a full stack software engineer with experience in Java, Spring Boot, Typescript, React,
                distributed systems, API design, frontend optimization, SQL, and production observability.
                I am comfortable across full-stack work, service extraction, and product reliability.
                """,
                "summary",
                "technical_summary"
        ));

        // Name a project you are most proud of and explain why. What was your role in the project, and what were the outcomes or results?
        docs.add(buildDocument(
                """
                The project I am most proud of is my portfolio website because I own it from the ground up -
                backend, frontend, databases, CI/CD, and deployment are all my responsibility. I have implemented
                several interesting features for my site including collecting user analytics, server sent events,
                an RAG chatbot for answering questions about my work experience and technical skills, and integrations
                with internal technologies to provide infrastructure visibility from within the site itself.
                """,
                "project",
                "technical_summary"
        ));

        // Name one instance in which you have had a conflict or disagreement with a coworker. How did you resolve it?
        docs.add(buildDocument(
                """
                I have had conflicts/disagreements with coworkers. One specific example is when another developer
                thought database strain was what was causing a performance issue when in reality it was something else.
                There was an outage with a specific service, it was a big deal, so we were both trying to quickly figure
                out what the root cause was. I had already looked at the service's database performance and the
                metrics we had available for it and had come to the conclusion that the database was not the root cause of the issue,
                the other developer thought it was the root cause.

                I told him I had already seen this specific pattern of metrics before and customers had not reported the issue,
                I also explained my theory behind why it wouldn't be the cause anyways. He ended up agreeing with me and I
                found the actual issue shortly after and resolved it.
                """,
                "conflict",
                "technical_summary"
        ));

        // How do you go about resolving conflicts or disagreements with coworkers? How do you find a way forward while still treating
        // your coworkers with respect and maintaining a good working relationship?
        docs.add(buildDocument(
                """
                My priority when resolving conflicts is first and foremost to treat my coworkers with respect - odds are we are
                going to continuously be working together, maybe for decades, and we're both experienced professionals. Even if
                my coworker wasn't I would still treat them with respect. So I just try to keep in mind that we're on the same
                team, we're both trying to fix the problem, that my coworker is competent, and that very likely one of us is just
                misunderstanding something.

                After I ensure I'm treating them with respect, I try to figure out where we disagree. I explain my beliefs and assumptions
                in an unbiased way. What I'm trying to do, why I'm trying to do it, why this way, what I expect will happen, etc. I also
                try to figure out those kinds of questions for my coworker - I try to question them as little as possible, preferring to
                explain myself so that they can see what I'm trying to do and that I have good intentions. I do not insist on being right,
                I don't try to make myself look smarter than them, I also don't want to shut down or disparage my coworker's ideas. There's more than
                one way to cook an egg, their solution could be just as good as mine. We are both trying to resolve the same problem.
                
                If I hold that my idea is clearly better, and my coworker does not acquiesce to my implementation, I will bring it up to my manager.
                If it is unclear I usually try to go with which dev is most senior, or back down. It's not a good use of my time to fight
                with coworkers and also hurts the relationship. Many times only time will tell which solution was better so the important
                thing is to pick one, move forward, and don't hold on to resentment because we're both trying to fix it together.
                """,
                "conflict_resolution",
                "technical_summary"
        ));

        return docs;
    }

    private PortfolioDocument buildDocument(String text, String category, String source) {
        PortfolioDocument document = new PortfolioDocument();
        document.setText(text);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", category);
        metadata.put("source", source);
        metadata.put("type", "work_experience");
        document.setMetadata(metadata);

        return document;
    }
}
