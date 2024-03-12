package com.baeldung.jnats;

import io.nats.client.Message;
import io.nats.client.Subscription;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class NatsClientLiveTest {

    private NatsClient connectClient() {
        return new NatsClient();
    }

    @Test
    public void messageExchangeViaPublishSubscribe_MessagesReceived() throws Exception {

        NatsClient client = connectClient();

        Subscription replySideSubscription = client.subscribeSync("requestSubject");
        Subscription publishSideSubscription = client.subscribeSync("replyToSubject");
        client.publishMessageWithReply("requestSubject", "replyToSubject", "hello there");

        Message message = replySideSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello there", new String(message.getData()));
        client.publishMessage(message.getReplyTo(), "hello back");

        message = publishSideSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello back", new String(message.getData()));
    }

    @Test
    public void messageExchangeViaRequestReply_MessagesReceived() throws Exception {

        NatsClient client = connectClient();

        Subscription replySideSubscription = client.subscribeSync("requestSubject");

        CompletableFuture<Message> future = client.makeRequest("requestSubject", "hello there");

        Message message = replySideSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello there", new String(message.getData()));
        client.publishMessage(message.getReplyTo(), "hello back");

        message = future.get(200, TimeUnit.MILLISECONDS);
        assertNotNull("No message!", message);
        assertEquals("hello back", new String(message.getData()));
    }

    @Test
    public void whenWildCardSubscription_andMatchTopic_MessageReceived() throws Exception {

        NatsClient client = connectClient();

        Subscription fooStarSubscription = client.subscribeSync("foo.*");

        client.publishMessage("foo.star", "hello foo star");

        Message message = fooStarSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello foo star", new String(message.getData()));

        Subscription fooGreaterSubscription = client.subscribeSync("foo.>");
        client.publishMessage("foo.greater.than", "hello foo greater");

        message = fooGreaterSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello foo greater", new String(message.getData()));
    }

    @Test
    public void whenWildCardSubscription_andNotMatchTopic_NoMessageReceived() throws Exception {

        NatsClient client = connectClient();

        Subscription starSubscription = client.subscribeSync("foo.*");

        client.publishMessage("foo.bar.plop", "hello there");

        Message message = starSubscription.nextMessage(200);
        assertNull("Got message!", message);

        Subscription greaterSubscription = client.subscribeSync("foo.>");
        client.publishMessage("foo.bar.plop", "hello there");

        message = greaterSubscription.nextMessage(200);
        assertNotNull("No message!", message);
        assertEquals("hello there", new String(message.getData()));
    }

    @Test
    public void givenQueueMessage_OnlyOneReceived() throws Exception {

        NatsClient client = connectClient();

        Subscription qSub1 = client.subscribeSyncInQueueGroup("foo.bar.requests", "myQueue");
        Subscription qSub2 = client.subscribeSyncInQueueGroup("foo.bar.requests", "myQueue");

        client.publishMessage("foo.bar.requests", "foobar");

        List<Message> messages = new ArrayList<>();

        Message message = qSub1.nextMessage(200);
        if (message != null) {
            messages.add(message);
        }

        message = qSub2.nextMessage(200);
        if (message != null) {
            messages.add(message);
        }

        assertEquals(1, messages.size());
    }
}
