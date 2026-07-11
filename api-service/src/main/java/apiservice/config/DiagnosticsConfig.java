package apiservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DiagnosticsConfig implements WebMvcConfigurer {

    private final DiagnosticsInterceptor diagnosticsInterceptor;

    public DiagnosticsConfig(DiagnosticsInterceptor diagnosticsInterceptor) {
        this.diagnosticsInterceptor = diagnosticsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(diagnosticsInterceptor);
    }
}
