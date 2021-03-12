package com.snaplogic.slackrtm;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.GoodbyeEvent;
import com.slack.api.model.event.HelloEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.UserTypingEvent;
import com.slack.api.rtm.RTMClient;
import com.slack.api.rtm.RTMEventHandler;
import com.slack.api.rtm.RTMEventsDispatcher;
import com.slack.api.rtm.RTMEventsDispatcherFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

public class SlackRTM {
    static final String botToken = "dummyBotToken"; // Should provide slack Bot token;
    static RTMClient rtm = null;

    public static void main(String[] args) throws IOException, DeploymentException, SlackApiException, URISyntaxException, InterruptedException {
        RTMEventsDispatcher dispatcher = RTMEventsDispatcherFactory.getInstance();


// Register a event handler runtime
        RTMEventHandler<UserTypingEvent> userTyping = new RTMEventHandler<UserTypingEvent>() {
            @Override
            public void handle(UserTypingEvent event) {
               System.out.println(event.getChannel());
               System.out.println(event);
            }
        };


        RTMEventHandler<HelloEvent> helloEvent = new RTMEventHandler<HelloEvent>() {
            @Override
            public void handle(HelloEvent event) {
                System.out.println(event);
                System.out.println(event.getType());
            }
        };

        RTMEventHandler<MessageEvent> messageEvent = new RTMEventHandler<MessageEvent>() {
            @Override
            public void handle(MessageEvent event) {
                System.out.println(event);
                if (event != null && event.getBotId() == null) {
                    Slack slack = Slack.getInstance();
                    MethodsClient methods = slack.methods(
                           botToken);
                    ChatPostMessageResponse response11 = null;
                    try {
                        ChatPostMessageRequest request = ChatPostMessageRequest.builder().channel("#team")
                                .text(":wave: Hi" + event.getText()).build();
                        response11 = methods.chatPostMessage(request);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SlackApiException e) {
                        e.printStackTrace();
                    }
                    System.out.println(response11);
                }
            }
        };

        RTMEventHandler<GoodbyeEvent> goodbyeEvent = new RTMEventHandler<GoodbyeEvent>() {
            @Override
            public void handle(GoodbyeEvent event) {
                System.out.println(event);
                close();
            }
        };
        dispatcher.register(helloEvent);
        dispatcher.register(userTyping);
        dispatcher.register(messageEvent);
        dispatcher.register(goodbyeEvent);


        Slack slack = Slack.getInstance();

        System.out.println("Initialize the client with a valid WSS URL");
        rtm = slack.rtmConnect(botToken);


        System.out.println("Enable an event dispatcher");
        rtm.addMessageHandler(dispatcher.toMessageHandler());

        System.out.println("Establish a WebSocket connection and start subscribing Slack events");
        rtm.connect();

        System.out.println("RTM client successfully started!");
        Thread.sleep(Long.MAX_VALUE);

/*        System.out.println("Send messages over a WebSocket connection");
        String channelId = "C0159DQ7ABY";
        String message = Message.builder().id(1234567L).channel(channelId).text(":wave: Hi " +
                "there!").build().toJSONString();
        rtm.sendMessage(message);

        System.out.println("To subscribe \"presence_change\" events");

        // Bot : U01P9MR3PML
        //.. = U01PCTEKZAL
        String userId = "U01PCTEKZAL";
        String presenceQuery =
                PresenceQuery.builder().ids(Arrays.asList(userId)).build().toJSONString();
        rtm.sendMessage(presenceQuery);
        String presenceSub =
                PresenceSub.builder().ids(Arrays.asList(userId)).build().toJSONString();
        rtm.sendMessage(presenceSub);

// A bit heavy-weight operation to re-establish a WS connection for sure
// Don't call this method frequently - it will result in a rate-limited error
        rtm.reconnect();

        System.out.println("Deregister a event handler runtime");
        dispatcher.deregister(userTyping);

        System.out.println(" Disconnect from Slack - #close() method does the same");
        rtm.disconnect();*/
    }

    static final void close() {
        if (rtm != null) {
            try {
                rtm.disconnect();
                rtm.close();
            } catch (IOException e) {
                System.out.println("Error while disconnecting the RTM");
            }
        }
    }
}
