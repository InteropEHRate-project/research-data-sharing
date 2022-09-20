
package unitn.iehr.researchCentre;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class ResearchCentreApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ResearchCentreApplication.class, args);

        //SpringApplication app = new SpringApplication(ResearchCentreApplication.class);
        //app.setDefaultProperties(Collections.singletonMap("server.port", "8083"));
        // USE port 80 when the jar has to be deployed on 213.249.46.208 IEHR host
        //app.run(args);
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
        return route(GET("/research-centre"), req ->
                ServerResponse.temporaryRedirect(URI.create("swagger-ui.html")).build());
    }

    @Override
    public void run(String... args) throws Exception {
        //app.run(args);
    }
}
