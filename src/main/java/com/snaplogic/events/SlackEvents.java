package com.snaplogic.events;

import java.util.regex.Pattern;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.reactions.ReactionsAddResponse;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;

public class SlackEvents {

    public static void main(String[] args) throws Exception {
        App app = new App();

        app.command("/hello", (req, ctx) -> {
            return ctx.ack(":wave: Hello!");
        });

        app.event(ReactionAddedEvent.class, (payload, ctx) -> {
            ReactionAddedEvent event = payload.getEvent();
            if (event.getReaction().equals("white_check_mark")) {
                ChatPostMessageResponse message = ctx.client().chatPostMessage(r -> r
                        .channel(event.getItem().getChannel()).threadTs(event.getItem().getTs())
                        .text("<@" + event.getUser()
                                + "> Thank you! We greatly appreciate your efforts :two_hearts:"));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            }
            return ctx.ack();
        });

        String notificationChannelId = "D1234567";

        // check if the message contains some monitoring keywords
        Pattern sdk = Pattern.compile(".*[(Java SDK)|(Bolt)|(slack\\-java\\-sdk)].*",
                Pattern.CASE_INSENSITIVE);
        app.message(sdk, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();
            String text = event.getText();
            MethodsClient client = ctx.client();

            // Add 👀reacji to the message
            String channelId = event.getChannel();
            String ts = event.getTs();
            ReactionsAddResponse reaction = client
                    .reactionsAdd(r -> r.channel(channelId).timestamp(ts).name("eyes"));
            if (!reaction.isOk()) {
                ctx.logger.error("reactions.add failed: {}", reaction.getError());
            }

            // Send the message to the SDK author
            ChatGetPermalinkResponse permalink = client
                    .chatGetPermalink(r -> r.channel(channelId).messageTs(ts));
            if (permalink.isOk()) {
                ChatPostMessageResponse message = client.chatPostMessage(r -> r
                        .channel(notificationChannelId)
                        .text("The Java SDK might be mentioned:\n" + permalink.getPermalink())
                        .unfurlLinks(true));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            } else {
                ctx.logger.error("chat.getPermalink failed: {}", permalink.getError());
            }
            return ctx.ack();
        });

        SlackAppServer server = new SlackAppServer(app);
        server.start(); // http://localhost:3000/slack/events
    }
}
