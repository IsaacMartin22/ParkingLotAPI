package apiservice.config;

import apiservice.model.PortfolioDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class PortfolioDataSeeder {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.seed-data:false}")
    private boolean seedData;

    @Value("${app.chat.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.chat.openai-embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    public PortfolioDataSeeder(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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
                document.setEmbedding(embedText(document.getText()));
                mongoTemplate.insert(document, collectionName);
            }
        };
    }

    private List<Double> embedText(String text) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
                "input", text,
                "model", embeddingModel
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI embeddings request failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode embeddingNode = json.path("data").get(0).path("embedding");
        List<Double> embedding = new ArrayList<>(embeddingNode.size());
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding.add(embeddingNode.get(i).asDouble());
        }
        return embedding;
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

        // Tell us about a project you owned end-to-end — from design through deployment and ongoing production support.
        // What was it, and how did you handle it when something didn't go as planned? *
        docs.add(buildDocument(
                """
                        My portfolio project is something I own end to end. It features server side events, user collected analytics that 
                        are persisted, RAG (retrieval augmented generation) chatbot to answer questions, and other things. Originally I 
                        designed it just to be a website app using server side events, it would be a proof of concept for a specific solution 
                        I had in mind, but eventually I decided for it to be my portfolio website containing said feature and contributed other 
                        features as well. So I had to walk back a bit on my original design and refactor things when I made that call. Its 
                        scope is still always growing, I'm currently unemployed so I'm spending every day learning and practicing new things 
                        and then I try to showcase my new skills in my portfolio site somehow, but now that I've accepted that the scope will 
                        always be growing it's easier to plan how I want my site to change when adding a specific section or skill. There aren't 
                        really any consequences for things not going as planned because hardly anyone visits my site, but one time my buildkite 
                        free trial ended so my integration showing my build pipelines broke. I updated the token I was using from my free trial 
                        to an active token and it resumed working again.
                """,
                "project_ownership",
                "technical_summary"
        ));

        // Do you have experience reviewing, modernizing, or replacing a legacy system? If so, briefly describe the system and what you changed. *
        docs.add(buildDocument(
                """
                Yes. My previous job had a legacy monolith structure that developers had been picking apart over the course of 15 years or so. 
                It started as a monolith and at some point the company made the architectural decision to switch to a microservices architecture. 
                So new functionality from that point on was built in microservices, and additionally when we were able to prioritize the work we 
                would duplicate chunks of functionality from the monolith in a new microservice and then direct traffic to the new microservice 
                from the monolith when we would hit that duplicate functionality. We would later go back and clean up the no longer needed monolith 
                code when it was determined the new microservice was stable. I implemented a new API endpoint in one of our new microservices 
                mimicking monolith functionality for retrieving user information. It was used for retrieving user information and then the old 
                monolith code was marked as ready for removal.
                """,
                "modernization",
                "technical_summary"
        ));

        // Tell us about a time you worked closely with a non-technical teammate or department to solve a real problem they were having.
        // How did you build that relationship, and how did you make sure you understood what they actually needed (not just what they asked for)?
        docs.add(buildDocument(
                """
                Product was the most regular non-technical team/department I worked with to solve problems. There was one time a specific customer 
                of ours wanted all their user's emails to be enabled for a specific notification and they wanted the notification preference panel 
                to be "Locked" for their users so they wouldn't be able to change their preferences to not receive the email. Product told me that 
                was what the customer wanted, in this instance I followed up with clarifying questions because "All users" probably didn't mean "All 
                users" in this case. The preference was only viewable by admin users so basic users shouldn't even be able to see or toggle their 
                preference there, and they definitely don't receive those emails. Just having good open dialogue with product where we're both 
                sharing what we're thinking and our interpretations of what the customer wants was a good way of hashing out the specific requirements 
                and what the customer actually needed.
                """,
                "cross_team_collaboration",
                "technical_summary"
        ));

        // Describe a manual or messy real-life process — tracking job tasks, orders, inventory, whatever — that you turned into a
        // working piece of software. What did the "before" look like, what did you build, and how did people's day-to-day actually change?
        docs.add(buildDocument(
                """
                        At my previous job we had a manual support workflow customers had to use to do something called "Transferring a portal". 
                        At the end of the day all it ended up being was a database update on our end after it went through our support team. 
                        One day the guy who usually handled those requests was out so the task fell to me. It was a pain for me and I saw that 
                        it could easily be automated so I talked to my manager and got approval for implementing self service functionality for 
                        the customer so that it wouldn't have to go through my team or our support team, customers could just do it themselves. 
                        I added a frontend, backend, tests, and then it just worked and neither us nor support ever had to do that manual workflow 
                        again. It was also a hit with customers as well because they didn't have to wait for us to get back to them saying the work 
                        was done, they could self service the functionality.\s
                """,
                "automation",
                "technical_summary"
        ));

        // Outside of work, what kinds of projects, tools, or problems do you like to tinker with?
        // (Side projects, open source, hobby builds, anything you geek out on.)
        docs.add(buildDocument(
                """
                I've made a couple contributions to open source - lichess, which is an open source community driven chess website. Additionally 
                I've contributed to a repository called hiring-agent - I noticed that sometimes when I applied to companies I was being automatically 
                rejected without a human ever looking at my resume, so I was looking into open source AI resume evaluators and that was one of them. 
                I made a quality of life contribution there. I've also worked on a mod for the game Minecraft in the past, I never published it or 
                finished it but it is out there, I've made a libGDX Java card game inspired by Slay the Spire, and right now I'm still actively 
                contributing to my portfolio website, I'm polishing a RAG chatbot for answering recruiter questions such as these by using vector 
                search and manually seeding responses to these questions. I'm actually going to seed these questions and my answers to these 
                questions into my Vector database as part of the RAG pipeline.
                """,
                "hobby_projects",
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
