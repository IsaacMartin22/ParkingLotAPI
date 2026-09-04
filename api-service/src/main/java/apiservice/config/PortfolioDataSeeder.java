package apiservice.config;

import apiservice.model.PortfolioDocument;
import apiservice.service.PortfolioDocumentSeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class PortfolioDataSeeder {

    private final PortfolioDocumentSeedService portfolioDocumentSeedService;

    @Value("${app.chat.seed-data:false}")
    private boolean seedData;

    public PortfolioDataSeeder(PortfolioDocumentSeedService portfolioDocumentSeedService) {
        this.portfolioDocumentSeedService = portfolioDocumentSeedService;
    }

    @Bean
    ApplicationRunner seedPortfolioData() {
        return args -> {
            if (!seedData) {
                return;
            }

            portfolioDocumentSeedService.seedMissingDocuments(buildSeedDocuments());
        };
    }

    private List<PortfolioDocument> buildSeedDocuments() {
        List<PortfolioDocument> docs = new ArrayList<>();

        // What are you looking for in your next role? What are your career goals?
        docs.add(buildDocument(
                """
                I want to become a better engineer. I want to be able to improve things to be newer and better than they were before.
                I want to work with other developers in a team environment. I believe working with other people is the best path for
                career longevity, growth, staying motivated, and overall just a more enjoyable life. People lead very different lives
                and have different experiences and perspectives. So much can be learned from people, so that's the environment I would
                like to be in.
                
                So in short, being a software engineer on a team and contributing towards a project with other developers is what I want 
                to be doing.
                """,
                "career_goals",
                "personal_profile"
        ));

        // Why are you leaving your current position or seeking a new role?
        docs.add(buildDocument(
                """
                I'm currently unemployed, I had worked at my previous job Widen for almost 5 years and my position was eliminated during company downsizing.
                In addition to wanting to actually contribute to larger projects in meaningful ways alongside other developers, I have bills
                to pay and have decided that I want to stick with software engineering as a career, so I'm seeking employment in a software
                engineering role. I like working with other people in any shape - on a team, at a company, as friends - it keeps me motivated
                and reminds me that the work I do is important - so although I might be able to strengthen my skills or learn new technologies
                on my own, I would rather work alongside other developers so that we can grow and learn from each other together.
                """,
                "seeking_employment",
                "personal_profile"
        ));

        // Do you have a Bachelor's degree or higher in Computer Science or a related field?
        docs.add(buildDocument(
                """
                I hold a Bachelor of Science in Computer Science from Oregon State University (2021) with a 3.68 GPA.
                """,
                "degree",
                "personal_profile"
        ));

        // List all the technologies you are proficient in or have experience with, including programming languages,
        // frameworks, databases, and any other relevant tools or platforms.
        docs.add(buildDocument(
                """
                For languages I'm familiar with Java, Typescript, Javascript, C++, Python, Rust, HTML, CSS, SQL, NoSQL, Bash, Groovy, and Ruby. 
                The frameworks I'm familiar with are Spring, Spring Boot, React, Hibernate, Playwright, Apache Tapestry, and JUnit. 
                For databases I'm most familiar with MySQL and PostgreSQL, however I've also worked with DynamoDB and MongoDB and MongoDB's vector database.
                For tools and platforms I'm most familiar with Github Copilot, Docker, Kubernetes, Postman, Maven, JFrog, Stormforge, Sumologic, Buildkite, Datadog, 
                Grafana, and Snowflake. My preferred cloud provider is AWS. I've used AWS SQS, AWS S3, AWS Kinesis, DynamoDB, Aurora DB, RDS, Lambda, and IAM. 
                """,
                "technologies",
                "personal_profile"
        ));

        // What is this project? What is this chatbot?
        docs.add(buildDocument(
                """
                This project is Isaac's portfolio website. It contains a chatbot trained on Isaac's work experience and technical skills as well as
                answers to recruiter questions he's received or found. It also contains a parking lot availability application using server-sent events, 
                user analytics collection, and dashboards for visibility into project infrastructure. The 2 main repositories powering this site are 
                entirely open source and visible on Isaac's Github profile IsaacMartin22. All chatbot seed data can be found in the backend repository 
                ParkingLotAPI in PortfolioDataSeeder.java. The chatbot is a RAG (retrieval-augmented generation) chatbot that uses vector search to find 
                relevant seed data and then uses an OpenAI model to generate answers to questions based on the seed data.
                """,
                "this_project_chatbot",
                "personal_profile"
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
                At Widen I reduced database load and increased user clarity by adding validation and snackbar messaging to a frontend application,
                reducing total frontend requests sent. This work covered TypeScript and React.
                """,
                "accomplishment",
                "frontend_validation_and_request_reduction"
        ));

        docs.add(buildDocument(
                """
                At Widen I expanded coverage for public APIs and internal APIs/SDKs by adding Java Spring Boot endpoints and publishing the new endpoints in the app's SDK.
                This included API design, backend implementation, and SDK integration work.
                """,
                "accomplishment",
                "api_and_sdk_expansion"
        ));

        docs.add(buildDocument(
                """
                At Widen I eliminated unnecessary tripling of specific network requests by leveraging knowledge of frontend render cycles in React,
                improving performance and application responsiveness for users.
                """,
                "accomplishment",
                "frontend_performance_optimization"
        ));

        docs.add(buildDocument(
                """
                At Widen I extracted functionality from a 25-year-old monolithic application into new services and refactored functionality to be easier to extract.
                I worked with Java, Spring, and Apache Tapestry in legacy modernization efforts.
                """,
                "accomplishment",
                "monolith_refactor_and_service_extraction"
        ));

        docs.add(buildDocument(
                """
                At Widen I used SQL knowledge to improve performance for several database queries, working with Hibernate and relational database optimization.
                This included understanding query behavior, indexing, and application-level tuning.
                """,
                "accomplishment",
                "database_performance_and_sql"
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


        addResumeDocuments(docs);
        addAccomplishmentsDocuments(docs);
        addRecruiterQuestionDocuments(docs);
        addDashboardOverviewDocuments(docs);
        addNonProfessionalDocuments(docs);

        return docs;
    }

    private void addNonProfessionalDocuments(List<PortfolioDocument> docs) {
        docs.add(buildDocument("""
                My ipV4 address is 67.676.76.767
                """,
                "hobbies",
                "personal"
        ));

        docs.add(buildDocument("""
                In my free time I play tennis in a 3.5 USTA League, I play chess (Rated ~1500, my favorite game mode is rapid), 
                and I play video games (One of my hobby projects is a card game inspired by Slay the Spire, built with the same underlying libGDX 
                Java framework as Slay the Spire). I also run half marathons - my PR is 1:27:17 in Eugene in 2026. I play flute in the UNLV community
                concert band and I have an orange cat named Cheddar. I read occasionally, mainly Sci-fi and Fantasy.
                """,
                "hobbies",
                "personal"
        ));
    }

    private void addDashboardOverviewDocuments(List<PortfolioDocument> docs) {
        // Add more dashboard overview documents here if needed

        docs.add(buildDocument("""
            I made the frontend and needed a way to store data for the parking app and analytics. Frontends shouldn't directly query databases.
            I'm most familiar with Java for the backend so I created a
            basic CRUD API for the parking database entities. I removed many update/delete endpoints because I just didn't see any
            use case for end users updating or deleting floors or lots, those changes are best done via migrations. So I removed
            those, and when I did want to remove some floors at a later point I did write a migration using flyway, which worked
            well.

            I later expanded the API past just a CRUD wrapper around parking lot entities. It's now my core backend application,
            any functionality I don't want to route from the frontend goes through this service. So it now makes requests with
            API tokens for the build and deploy dashboards and persists analytics data emitted from the frontend for the analytics
            dashboard. It also interacts with my MongoDB vector database for the RAG chatbot, and queries OpenAI for embeddings and 
            completions at the end of the RAG pipeline.
        """,
                "dashboard_overview",
                "backend_api"
        ));

        docs.add(buildDocument("""
            I built an SDK for my backend API. My initial reason for doing so was to have a separate "Event Generator" service that automatically 
            made a constant stream of API calls to update the parking spaces, which would make my server sent events feature very visible. My goal 
            was to showcase the server sent events by having the generator constantly be updating the frontend so whenever anyone viewed floor data 
            they'd see it updating live. I also wanted to create an SDK, knowledge of how to create and publish an SDK could be something companies 
            are looking for. Those were my reasons, functionally though in terms of the app the impetus for the SDK was to use it in the Event Generator service.

            At some point I changed my mind, I scrapped the Event Generator service idea. In my mind it would be overkill.
            It would just be some background worker artificially creating random traffic. It would cost me money to host, it would
            slow down my site by using up the API's bandwidth and memory, and it could also just be confusing for site viewers.
            They wouldn't know where the events were coming from until they read about the generator service. So I decided to instead
            allow the user to create a random car in a parking space or remove a car via the frontend, they could just
            open another tab to see the SSE effect. So I scrapped the generator service idea and left the project as just an SDK.
        """,
                "dashboard_overview",
                "sdk"
        ));

        docs.add(buildDocument("""
            The parking explorer was the first thing I decided to build for my site. I wanted to solve a real world problem. I had seen a hand
            placed sign at the LAS airport where I live saying a lot was full. It was not full. I figured a website would provide more
            up-to-date information and be more reliable than a sign, so I started building the Parking Lot app.

            It's a simple application, not very impressive. But it is a real world problem and I had to start somewhere. I figured I
            could add features and improve it over time if it proved too simple a task, which I did. The server sent events are the
            best aspect. My idea is that something would trigger API requests, which would update server state. The trigger could be a
            pressure plate under the parking space, a drone taking a photo and having it be processed, or a human on a computer.
            Something makes an API request and then after the API updates the database it sends a server sent event to update all
            subscribed clients. A pretty good use case for SSE. I could make more improvements but I wanted to spend time elsewhere.

            I used React and Typescript because I'm most familiar with them for frontend work. Reusable component libraries are good
            practice so I have resuable components, and tanstack query is a great library for making network calls so I used tanstack.
        """,
                "dashboard_overview",
                "parking_app"
        ));

        docs.add(buildDocument("""
            Analytics was an "I need to make my site better somehow" feature. Analytics is very useful, figuring out how
            users are using the product is helpful to know. It lets product make data driven decisions. What users like,
            what they don't like, what they're spending time on and clicking and viewing, these kinds of things are really
            important to product. Being able to build that kind of thing out on my site seems incredibly useful.

            As mentioned in the database segment if I expected my site to get a ton of traffic I would have
            a separate service and database just for analytics because it scales much differently and isn't
            a truly core customer experience.

            I wanted all my code to be publicly available so I didn't want any PII. The closest the site does to identify
            a user is generate a random id and persist it to session storage. The logic is not secure. Someone could easily
            copy the value and give it to someone else to spoof data, but it should be safe to assume that most users
            aren't doing that and therefore that most events sharing the same session id were done by the same end user.
        """,
                "dashboard_overview",
                "analytics"
        ));

        docs.add(buildDocument("""
            I found myself trying to come up with ways to demonstrate that I'm following good development practice. At
            this point I had implemented pipelines and deployed my services publicly, so there were more things involved
            than was immediately visible on the site. Infrastructure is incredibly relevant for developers and my site
            tries to showcase my development skills so I wanted to provide insight into my CI pipelines somehow. I had
            automated pipelines at this point, I just needed to find a way to make the pipelines publicly available without
            letting someone modify or break anything.
            
            I originally just had buildkite listed as one of the technologies on the home page. I didn't like that, to me
            it wasn't enough visual insight. I had written tests and created multiple pipelines, it seemed like too much
            effort I had gone through to just leave as a dumb text oval in a list. I then remembered I was a developer, I
            could just create a buildkite API token and use it. I assumed correctly that buildkite would have an API, I
            looked at the documentation and found I could run a simple GET builds request to get all the information I wanted.
            It showed my pipelines, build status, build duration, dates, it was perfect. So that was the builds dashboard.
            
            I only have very minimal unit tests, ideally I'd add integration tests, end to end tests, and smoke tests.
            I'd use playwright for the frontend, I've used it before, and the backend I'd want to set up
            some hard data that doesn't change in a staging environment to test. It's just me though, tests seem much
            less important with less developers because there aren't other people who aren't as familiar with my code
            trying to change it. Usually those situations are when bugs happen so the automated tests are to prevent it,
            but being a solo dev lowers that priority for me.
        """,
                "dashboard_overview",
                "buildkite"
        ));

        docs.add(buildDocument("""
            The deployments dashboard was pretty much the same as the build dashboard but instead of Buildkite it was Render. I wanted
            visibility into deployment activity so I did the same thing - checked if Render had an API, generated
            a key, used the key in the one request I wanted, and now it displays Deployment information.

            Render actually had an outage when I was testing which made me think I had wired it up wrong,
            eventually I verified with Postman that it just wasn't up and checked the status page and it
            was having issues. So out of that now both the Deployments and Builds pages link to their
            respective status pages while loading in case they're down. Not much I can do in that case.
        """,
                "dashboard_overview",
                "render"
        ));

        docs.add(buildDocument("""
            I was originally just keeping API logs in memory and exposing them via a diagnostic endpoint. After I
            started using API keys for buildkite and render and using environment variables I realized there was
            a good chance one of those might accidentally get exposed via that method, so I probably shouldn't make
            all service logs publicly available. It also wasn't scalable or standard of me to do that.
            
            So I decided to start using Sumologic for logs. I can access all logs for the API service in Sumo.
            The API is really the only service that has logs. The frontend will emit analytics events for errors and
            post those errors to the API for me to check out if desired so I don't feel the need to have separate
            logging for the frontend. I use a Trello board for Project Management. I thought about exposing
            some sort of feature request functionality on my site but I think I'm just going to work on what I
            work on. Trello is already good enough standalone, I don't feel the need to add an API wrapper and
            have a separate dashboard for it. It should already be clear I know how to integrate with APIs so
            another API integration seems redundant in terms of expressing knowledge of development concepts.
            I also have my portfolio linked to friends, I wouldn't put it past them to put something undesired
            on a feature request board if I built that feature.
        """,
                "dashboard_overview",
                "logging_and_project_management"
        ));


    }

    private void addResumeDocuments(List<PortfolioDocument> docs) {
        // Add more resume documents here if needed

        docs.add(buildDocument("""
            My Github profile is IsaacMartin22 - https://github.com/IsaacMartin22. The frontend for my portfolio site can be found at https://github.com/IsaacMartin22/ParkingLotFrontend,
            the backend for my portfolio site can be found at https://github.com/IsaacMartin22/ParkingLotAPI. My site also has an SDK for interacting with the backend API at
            https://github.com/IsaacMartin22/ParkingLotEventGenerator. A libGDX Java card game inspired by Slay the Spire can be found at https://github.com/IsaacMartin22/CardGame.
            Various open source contributions can be found on the forked repositories in my Github profile, including lichess and hiring-agent.
        """,
                "github",
                "personal_profile"
        ));

        docs.add(buildDocument("""
            I worked as an Associate Software Engineer at Widen and later as a Software Engineer at Widen from 2021 to 2026.
            Widen is a Digital Asset Management (DAM) SaaS company. My work focused on a distributed Java + TypeScript + React microservices system with
            30+ apps and services.
        """,
                "experience",
                "widen_work_history"
        ));

        docs.add(buildDocument("""
            I have resolved thousands of defects over the course of my career, some of which include critical production issues taking down entire
            services. I have experience with defect triage, root cause analysis, and implementing fixes in a timely manner to restore service availability.
            I have also made countless improvements to performance by optimizing queries, refactoring code logic, and improving application responsiveness. 
            I have experience with production observability tools such as Datadog, Sumologic, and Grafana.
        """,
                "accomplishment",
                "defect_resolution_and_improvements"
        ));

        docs.add(buildDocument("""
            I have contributed to open source. One open source project I have contributed to is Hiring-agent, a Python pipeline for AI evaluation of resumes.
            I added a simple quality of life feature to speed up the setup process of using the tool. Another open source contribution I have made is
            to Lichess, a TypeScript and Scala community-driven chess website. I fixed a simple css transparency bug on the homepage of the site.
        """,
                "contribution",
                "open_source_contributions"
        ));

        docs.add(buildDocument("""
            My technical skills include Java, TypeScript, JavaScript, C++, Python, Rust, HTML, CSS, SQL, and NoSQL.
            Frameworks include Spring, Spring Boot, React, Hibernate, and Apache Tapestry.
            Testing includes Playwright, Spock, Groovy, and JUnit.
        """,
                "skills",
                "languages_frameworks_and_testing"
        ));

        docs.add(buildDocument("""
            I have also worked with Buildkite, Docker, Kubernetes, Maven, JFrog, and Stormforge for CI and deployment.
            My cloud experience includes AWS SQS, AWS S3, and AWS Kinesis.
            I have worked with Postman, Sumologic, Datadog, Grafana, Snowflake, and integrations tooling.
        """,
                "skills",
                "ci_deployment_cloud_and_monitoring"
        ));

        //docs.add(buildDocument())

    }

    private void addAccomplishmentsDocuments(List<PortfolioDocument> docs) {
        // Add more accomplishments documents here if needed

        // Server sent events
        docs.add(buildDocument("""
            My portfolio site contains a parking lot availability application that uses server-sent events to provide real-time updates to users.
            Clients visiting my site are subscribed to server sent events when they view a specific floor and my API backend handles all updates
            to parking spaces on a floor. So when a parking space is updated, the backend sends an event to all subscribed clients, including
            the one that made the request, which updates the floor view for all clients in real time without the user having to refresh the page.
        """,
                "feature",
                "accomplishments"
        ));

        // RAG chatbot explanation
        docs.add(buildDocument("""
            The chatbot works by taking a user question, embedding it into a vector representation, and then performing a vector search
            against other pre-embedded documents Isaac typed up about himself. Anything it finds it supplies it as context to the chat model. 
            Picture ChatGPT with one extra step - the vector search step. If you ask ChatGPT right now "What degree does Isaac have?" it won't know.
            However, if you do a database lookup for things that might be related first and find something that exists already (The answers I pre-typed) 
            and then ask ChatGPT the same question with that context, it's like asking "What degree does Isaac have? By the way, Isaac has a 
            Bachelor of Science in Computer Science from Oregon State University". It will know the answer because it has additionaly context.
            The embedding and vector search parts are the magical steps that make the chatbot look complex and smart.
        """,
                "chatbot",
                "explanation"
        ));

        // RAG chatbot models, specs, and training data
        docs.add(buildDocument("""
            The model currently used for the chatbot's text embeddings is text-embedding-3-small by OpenAI, the model used for chat
            is also OpenAI, gpt-4o-mini, and the maximum number of context chunks is 5. There are around ~50 seeded documents,
            all of which are publicly available on Isaac's Github
            https://github.com/IsaacMartin22/ParkingLotAPI/blob/main/api-service/src/main/java/apiservice/config/PortfolioDataSeeder.java.
            Any of those things are subject to change at any time though
        """,
                "chatbot",
                "specs"
        ));

        // RAG chatbot
        docs.add(buildDocument("""
            My portfolio site contains a RAG chatbot intended to speed up the process of figuring out if I'm a good fit for a role. I have
            seeded a ton of hand-typed data, I add to it whenever I encounter new recruiter or hiring manager questions I haven't already answered. 
            The chatbot uses vector search to find relevant chunks of seed data and then uses an OpenAI model to generate answers to questions based 
            on the relevant seed data. It speeds up the recruiter process, RAG/AI/LLM knowledge is in demand for software engineering at the moment
            so I figured it would be useful to learn how it works so I just built something. It happens to be good enough production wise to 
            be shipped without too much chunk configuration or optimization from me.
        """,
                "feature",
                "accomplishments"
        ));

        // User analytic collection
        docs.add(buildDocument("""
            My portfolio site collects user analytics for certain actions. I was just trying to build out myself some of the services/features
            my previous job had used in production and analytics was one. As a side bonus for implementing the feature I could now see how people
            were interacting with my site (if at all), which application tools would scrape my site if it was linked in my resume, and other
            interesting things. It's pretty important with software to understand how users are interacting with the product, like what they're
            using it for and which components are used the most, so it seemed like analytics collection would be a good feature for any software
            product to have so I should be familiar with how to implement it and figure out other related concerns.
        """,
                "feature",
                "accomplishments"
        ));

        // Portfolio Integrations
        docs.add(buildDocument("""
            My portfolio site contains integrations with several internal infrastructure technologies to provide visibility into the other 
            tools involved in the development and deployment of my site. Automated testing, CI/CD, version control, logging, observability, 
            and deployment are all important parts of the software development lifecycle so I wanted to show I am familiar with those tools.
            So I created dashboards for the CI/CD pipeline I use, which is buildkite, and for the deployment service I use, which is Render.
        """,
                "feature",
                "accomplishments"
        ));

    }

    private void addRecruiterQuestionDocuments(List<PortfolioDocument> docs) {
        // Add more recruiter question documents here if needed

        // Provide an example of a goal you reached and tell me how you achieved it.
        docs.add(buildDocument("""
            I would see opportunities for improvement across our tech stack.
            When I saw opportunities for improvement I would create tickets in our backlog. My Agile Team Lead (ATL) would pull 
            tickets in according to priority and I would work on the tickets in the sprint. 
            
            To pick one such ticket, I mention it in my resume but at one point I noticed that one of our services was unnecessarily tripling 
            a specific network request on page load. It had very high traffic, high costs, and had been experiencing 
            load issues, so I had a goal to improve the service's efficiency. It wasn't until a few months later that I got the opportunity
            when my ATL pulled my ticket in to one of our sprints.
            
            So in terms of how I achieved my goal of improving performance for this service, I identified the problem, documented that it was a 
            problem, initiated the process of resolving the problem by creating a ticket, and then when it came my turn to implement a fix I 
            analyzed the problem using a debugger and used my knowledge of React lifecycles to fix a prop/state recursive iteration so we only
            sent the one request we needed. 
        """,
                "goal",
                "personal_profile"
        ));

        // Describe a time you faced a significant challenge at work.
        docs.add(buildDocument("""
            I encountered a regularly occurring issue that was causing a lot of related issues. We had some very big customers and some
            very small customers. Spikes of traffic from our larger customers would completely lock up certain services systems by 
            overwhelming them, which would cause traffic for all other customers to get backed up.
            
            I was one of the developers who investigated the issue and identified the root cause. Several developers along with other
            important stakeholders were pulled into a post mortem meeting where we discussed solutions. Obviously the ideal solution 
            would be to scale all bottlenecked resources up, but that would dramatically increase costs. There were other longer term
            solutions suggested, we did end up in agreement on one longer term implementation, but for a short term solution we 
            created queues targeting single customers that would store work in a holding queue to be replayed at a controlled rate 
            after our systems and services had stabilized
        """,
                "challenge",
                "personal_profile"
        ));

        // How do you handle stress and pressure?
        docs.add(buildDocument("""
            I do what I can do and let the chips fall where they may. If it's a recurring stressor I might step back and evaluate
            why I'm choosing to remain in the stressful environment and if it's worth remaining in, but if it's a one time stressor
            I do what I can. Do what I'm supposed to be doing and do it to the best of my ability. I have limits, everyone does,
            regularly finding a balance between comfortability and learning keeps me on the best path for sustainable growth.
            
            There are situations where a task is beyond me, and that's ok. There is always going to be a realistic path
            from where I'm at currently to where I need to be to complete the task, whether that path is 100 miles or 1 mile. Other
            developers and the company can and will work with me, around me, or beneath me, it really doesn't matter. The important
            thing for me is to take my next step I need to be taking on my path.
        """,
                "stress_and_pressure",
                "personal_profile"
        ));

        // What are your strengths?
        docs.add(buildDocument("""
            I'm proactive about making myself useful. It's usually very easy to find stuff that needs to be done, if it's not strictly 
            someone else's responsibility or not already being worked on I'll usually pick it up and get it done or mention it to someone who
            is in a better position to take care of it. Rather than waiting for someone to tell me what to do I do stuff until someone tells me 
            not to. I also want to be a better engineer and developer, I want to be able to come up with better solutions and make things work
            better, so I will actively seek out and take on new things to learn and tasks I haven't yet fully mastered.
        """,
                "strengths",
                "personal_profile"
        ));

        // What are your weaknesses?
        docs.add(buildDocument("""
            In relation to the strengths I described, being proactive about making myself useful can be a double edged sword in a way. Sometimes
            the things I end up doing would be better handled by someone else and I end up stepping on toes, or I end up duplicating portions of tasks
            because someone was already working on what I jumped on and hadn't mentioned it yet. I get frustrated when the same problems pop up over 
            and over, I don't really care what someone's reasons are for not taking care of it, maybe they have other priorities or they don't know how
            to fix the issue, I will step in and resolve the issue or at least start working towards resolving it.
        """,
                "weaknesses",
                "personal_profile"
        ));

        // Give me a brief overview of who you are and your software engineering experience.
        docs.add(buildDocument("""
            I am Isaac Martin, a fullstack software engineer based in Las Vegas, Nevada (Open to relocation). I have 4 years and 8 months of
            professional work experience entirely with Widen - later Widen, an Acquia company. Widen is in the Digital Asset
            Management (DAM) industry making Software as a Service (SAAS) apps and services. I'm most familiar with microservices
            architecture, Java, Spring Boot, Typescript, React, SQL, and distributed systems.
        """,
                "professional_summary",
                "personal_profile"
        ));

        // Name a project you are most proud of and explain why. What was your role in the project, and what were the outcomes or results?
        docs.add(buildDocument("""
            The project I am most proud of is my portfolio website because I built and own it from the ground up -
            backend, frontend, databases, CI/CD, and deployment are all my responsibility. I have implemented
            several interesting features for my site including collecting user analytics, server sent events,
            a RAG chatbot for answering questions about my work experience and technical skills, and integrations
            with internal technologies to provide infrastructure visibility from within the site itself.
        """,
                "project",
                "technical_summary"
        ));

        // Name one instance in which you have had a conflict or disagreement with a coworker. How did you resolve it?
        docs.add(buildDocument("""
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
        docs.add(buildDocument("""
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

        // Outside of work, what kinds of projects, tools, or problems do you like to tinker with?
        // (Side projects, open source, hobby builds, anything you geek out on.)
        docs.add(buildDocument("""
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

        // Describe a manual or messy real-life process — tracking job tasks, orders, inventory, whatever — that you turned into a
        // working piece of software. What did the "before" look like, what did you build, and how did people's day-to-day actually change?
        docs.add(buildDocument("""
            At my previous job we had a manual support workflow customers had to use to do something called "Transferring a portal". 
            At the end of the day all it ended up being was a database update on our end after it went through our support team. 
            One day the guy who usually handled those requests was out so the task fell to me. It was a pain for me and I saw that 
            it could easily be automated so I talked to my manager and got approval for implementing self service functionality for 
            the customer so that it wouldn't have to go through my team or our support team, customers could just do it themselves. 
            I added a frontend, backend, tests, and then it just worked and neither us nor support ever had to do that manual workflow 
            again. It was also a hit with customers as well because they didn't have to wait for us to get back to them saying the work 
            was done, they could self service the functionality.
        """,
                "automation",
                "technical_summary"
        ));

        // Tell us about a time you worked closely with a non-technical teammate or department to solve a real problem they were having.
        // How did you build that relationship, and how did you make sure you understood what they actually needed (not just what they asked for)?
        docs.add(buildDocument("""
            Product is the most regular non-technical team/department I worked with to solve problems. There was one time a customer
            wanted all their user's emails to be enabled for a specific notification and they wanted the notification preference 
            to be "Locked" for all users so they wouldn't be able to change their preferences to not receive the email. Product told me that 
            was what the customer wanted. I followed up by clarifying things because "All users" probably didn't mean every single 
            users in this case. The preference was only viewable by admin users so basic users shouldn't even be able to see or toggle their 
            preference there, and they definitely don't receive those emails. Just having good open dialogue with product where we're both 
            sharing what we're thinking and our interpretations of what the customer wants was a good way of hashing out the specific requirements 
            and what the customer actually needed.
        """,
                "cross_team_collaboration",
                "technical_summary"
        ));

        // Do you have experience reviewing, modernizing, or replacing a legacy system? If so, briefly describe the system and what you changed. *
        docs.add(buildDocument("""
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

        // Tell us about a project you owned end-to-end — from design through deployment and ongoing production support.
        // What was it, and how did you handle it when something didn't go as planned? *
        docs.add(buildDocument("""
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
