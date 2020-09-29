package no.fint.personvern;

import no.fint.adapter.AbstractSupportedActions;
import no.fint.model.personvern.kodeverk.KodeverkActions;
import no.fint.model.personvern.samtykke.SamtykkeActions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class    SupportedActions extends AbstractSupportedActions {

    /**
     * TODO
     * <p>
     * This is where you add the actions that are supported for your adapter.
     * Use the add() for single action and addAll() for all actions in the enum.
     * </p>
     * <pre>
     *  add(PwfaActions.GET_ALL_DOGS);
     *  add(PwfaActions.GET_DOG);
     * </pre>
     */
    @PostConstruct
    public void addSupportedActions() {
        addAll(SamtykkeActions.class);
        addAll(KodeverkActions.class);
    }
}
