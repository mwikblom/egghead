package egghead.eggheadkafka;

import egghead.eggheadkafka.kafka.spring.KafkaSpringConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author mikael
 */
@SpringBootApplication

// enable any implementation
//@ComponentScan(basePackageClasses = SpringKafkaConfiguration.class)
//@ComponentScan(basePackageClasses = KafkaStreamsConfiguration.class)
@ComponentScan(basePackageClasses = KafkaSpringConfiguration.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}