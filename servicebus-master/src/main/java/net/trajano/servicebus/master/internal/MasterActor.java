package net.trajano.servicebus.master.internal;

import java.util.HashMap;
import java.util.Map;

import net.trajano.servicebus.master.ActorProvider;
import akka.actor.UntypedActor;

public class MasterActor extends UntypedActor {

	/**
	 * Only look up by exact class, polymorphic messages are not supported (and
	 * would significantly slow down the processing).
	 */
	private final Map<Class<?>, ActorProvider> providers = new HashMap<>();

	@Override
	public void onReceive(final Object message) throws Exception {
		if (message instanceof ActorRegistration) {
			final ActorProvider provider = ((ActorRegistration) message)
					.getProvider();
			for (final Class<?> messageClass : provider.messageClassesHandled()) {
				if (providers.containsKey(messageClass)) {
					throw new IllegalArgumentException("handler for  "
							+ messageClass + " already exists.");
				}
				providers.put(messageClass, provider);
			}
			return;
		} else if (message instanceof ActorDeregistration) {
			final ActorProvider provider = ((ActorDeregistration) message)
					.getProvider();
			for (final Class<?> messageClass : provider.messageClassesHandled()) {
				providers.remove(messageClass);
			}
			return;
		}
		final ActorProvider provider = providers.get(message.getClass());
		if (provider == null) {
			unhandled(message);
			return;
		}
		provider.newActor(getContext()).tell(message);
	}
}
