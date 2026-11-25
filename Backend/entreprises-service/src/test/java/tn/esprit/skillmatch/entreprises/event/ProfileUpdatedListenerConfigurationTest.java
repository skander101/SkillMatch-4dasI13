package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tn.esprit.skillmatch.entreprises.repository.AvisEntrepriseRepository;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUpdatedListenerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(AvisEntrepriseRepository.class, () -> Mockito.mock(AvisEntrepriseRepository.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(ProfileUpdatedHandler.class,
                    () -> new ProfileUpdatedHandler(
                            Mockito.mock(AvisEntrepriseRepository.class),
                            new ObjectMapper()
                    ))
            .withUserConfiguration(ListenerConfiguration.class);

    @Test
    void listenerIsNotCreatedByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ProfileUpdatedListener.class));
    }

    @Test
    void listenerIsCreatedWhenEnabled() {
        contextRunner.withPropertyValues("skillmatch.events.listener-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ProfileUpdatedListener.class));
    }

    @Configuration
    @Import(ProfileUpdatedListener.class)
    static class ListenerConfiguration {
    }
}
