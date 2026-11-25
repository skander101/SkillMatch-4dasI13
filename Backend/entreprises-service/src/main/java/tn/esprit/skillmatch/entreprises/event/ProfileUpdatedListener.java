package tn.esprit.skillmatch.entreprises.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "skillmatch.events.listener-enabled", havingValue = "true", matchIfMissing = false)
public class ProfileUpdatedListener {

    private final ProfileUpdatedHandler profileUpdatedHandler;

    public ProfileUpdatedListener(ProfileUpdatedHandler profileUpdatedHandler) {
        this.profileUpdatedHandler = profileUpdatedHandler;
    }

    @RabbitListener(queues = "${skillmatch.events.queue-prefix:skillmatch.entreprises}.ProfileUpdated")
    public void onMessage(EvenementEntreprise event) {
        profileUpdatedHandler.handle(event);
    }
}
