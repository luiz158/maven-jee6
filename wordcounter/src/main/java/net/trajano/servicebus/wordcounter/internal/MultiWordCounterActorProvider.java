package net.trajano.servicebus.wordcounter.internal;

import java.util.Collections;
import java.util.Map;

import net.trajano.servicebus.master.AkkaActorServiceBus;
import net.trajano.servicebus.master.MapReduceActorProvider;
import net.trajano.servicebus.wordcounter.Accumulator;
import net.trajano.servicebus.wordcounter.MultiAccumulator;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.util.Duration;
import akka.actor.ActorRef;
import akka.util.Timeout;

public class MultiWordCounterActorProvider extends
		MapReduceActorProvider<MultiAccumulator, String, Map<String, Integer>> {

	/**
	 * Service bus, used for chaining.
	 */
	private final AkkaActorServiceBus serviceBus;

	public MultiWordCounterActorProvider(final AkkaActorServiceBus serviceBus) {
		this.serviceBus = serviceBus;
	}

	@Override
	public MultiAccumulator initializeAccumulator(final Object message) {
		return new MultiAccumulator();
	}

	@Override
	public int map(final Object message, final ActorRef actor) throws Exception {
		final String[] resources = (String[]) message;
		for (final String resource : resources) {
			actor.tell(resource);
		}
		return resources.length;
	}

	@Override
	public Class<?>[] messageClassesHandled() {
		return new Class<?>[] { String[].class };
	}

	@Override
	public Map<String, Integer> process(final String derivedMessage)
			throws Exception {
		final Future<Accumulator> ask = serviceBus.ask(Accumulator.class,
				Timeout.intToTimeout(2000));
		serviceBus.tell(derivedMessage);
		final Accumulator result = Await.result(ask, Duration.Inf());
		return Collections.singletonMap(derivedMessage, result.getCount());
	}

	@Override
	public void reduce(final MultiAccumulator accumulator,
			final Map<String, Integer> result) {
		for (final String key : result.keySet()) {
			accumulator.add(key, result.get(key));
		}
	}

}