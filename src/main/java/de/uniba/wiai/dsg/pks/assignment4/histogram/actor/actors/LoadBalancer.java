package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors;

import akka.actor.AbstractActor;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FileRequestMessage;
import net.jcip.annotations.ThreadSafe;

import java.util.List;

@ThreadSafe
public class LoadBalancer extends AbstractActor {

    private final Router router;

    public LoadBalancer(List<Routee> routees){
        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FileRequestMessage.class, message -> router.route(message,getSender()))
                .build();
    }

}
